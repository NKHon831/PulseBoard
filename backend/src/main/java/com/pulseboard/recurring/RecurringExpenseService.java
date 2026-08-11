package com.pulseboard.recurring;

import com.pulseboard.common.currency.CurrencyCode;
import com.pulseboard.common.currency.ExchangeRateService;
import com.pulseboard.common.exception.BadRequestException;
import com.pulseboard.common.exception.NotFoundException;
import com.pulseboard.expense.Expense;
import com.pulseboard.expense.ExpenseRepository;
import com.pulseboard.recurring.dto.RecurringExpenseRequest;
import com.pulseboard.recurring.dto.RecurringExpenseResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fixed expenses: entered once, then written into the user's expenses on every
 * day their schedule matches.
 *
 * <p>Generation is a lazy catch-up rather than a nightly job. A personal app is
 * not guaranteed to be running at midnight, so the days that are owed are worked
 * out whenever the user looks at their expenses. That survives downtime, and
 * cannot skip a day just because nothing was running when it turned over.
 */
@Service
@RequiredArgsConstructor
public class RecurringExpenseService {

    private static final Logger log = LoggerFactory.getLogger(RecurringExpenseService.class);

    /**
     * Ceiling on how far back a single catch-up will reach. Only bites when an
     * account has gone unused for a year; it stops one request from having to
     * insert an unbounded number of rows.
     */
    private static final int MAX_CATCH_UP_DAYS = 366;

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseRepository expenseRepository;
    private final ExchangeRateService exchangeRateService;

    public List<RecurringExpenseResponse> list(UUID userId, CurrencyCode currency) {
        return recurringExpenseRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(recurring -> toResponse(recurring, currency))
                .toList();
    }

    /**
     * Creates a template and immediately generates anything it already owes, so
     * a fixed expense starting today shows up without waiting for a reload.
     */
    public RecurringExpenseResponse create(UUID userId, RecurringExpenseRequest request, LocalDate clientToday) {
        LocalDate today = resolveToday(clientToday);
        LocalDate startDate = request.startDate() == null ? today : request.startDate();

        RecurringExpense recurring = RecurringExpense.builder()
                .userId(userId)
                .category(request.category().trim())
                .description(blankToNull(request.description()))
                .frequency(request.frequency())
                .active(request.active() == null || request.active())
                .startDate(startDate)
                .build();
        applyAmount(recurring, request);

        recurringExpenseRepository.save(recurring);
        materialize(recurring, today);

        return toResponse(recurring, currencyOf(request));
    }

    /**
     * Edits a template. Expenses it has already generated are left alone: they
     * are a record of days that have passed, not a projection to be rewritten.
     */
    public RecurringExpenseResponse update(
            UUID userId, UUID id, RecurringExpenseRequest request, LocalDate clientToday) {
        RecurringExpense recurring = find(userId, id);

        recurring.setCategory(request.category().trim());
        recurring.setDescription(blankToNull(request.description()));
        recurring.setFrequency(request.frequency());
        if (request.active() != null) recurring.setActive(request.active());
        if (request.startDate() != null) recurring.setStartDate(request.startDate());
        applyAmount(recurring, request);

        recurringExpenseRepository.save(recurring);
        materialize(recurring, resolveToday(clientToday));

        return toResponse(recurring, currencyOf(request));
    }

    /**
     * Pauses or resumes a template without touching its money. Kept apart from
     * {@link #update} on purpose: that re-freezes the amount at today's rates,
     * which would quietly re-price a fixed expense just for being paused.
     */
    public RecurringExpenseResponse setActive(
            UUID userId, UUID id, boolean active, CurrencyCode currency, LocalDate clientToday) {
        RecurringExpense recurring = find(userId, id);
        recurring.setActive(active);

        // Resuming picks up from today rather than backfilling the days it spent
        // paused: they were paused precisely so nothing would be charged for them.
        // Today itself still counts, and is not duplicated if it already exists.
        if (active) recurring.setLastGeneratedDate(resolveToday(clientToday).minusDays(1));

        recurringExpenseRepository.save(recurring);
        return toResponse(recurring, currency);
    }

    /**
     * Deletes the template only. The expenses it generated are kept — that money
     * was spent — and the database clears their link to it.
     */
    public void delete(UUID userId, UUID id) {
        recurringExpenseRepository.delete(find(userId, id));
    }

    /**
     * Writes in every fixed expense the user is owed up to {@code clientToday}.
     *
     * <p>Called before listing expenses, so the day's entries are already there
     * by the time they are read. Never lets a failure here break that listing:
     * seeing your expenses matters more than the fixed ones being current.
     */
    public void materializeDueFor(UUID userId, LocalDate clientToday) {
        LocalDate today = resolveToday(clientToday);

        for (RecurringExpense recurring : recurringExpenseRepository.findByUserIdAndActiveTrue(userId)) {
            try {
                materialize(recurring, today);
            } catch (Exception ex) {
                // Typically two requests racing to generate the same day, which the
                // unique index rejects. The loser has nothing left to do.
                log.warn("Could not generate fixed expense {} ({})", recurring.getId(), ex.toString());
            }
        }
    }

    /** Generates the days {@code recurring} owes, then records how far it got. */
    private void materialize(RecurringExpense recurring, LocalDate today) {
        if (!recurring.isActive()) return;

        LocalDate from = firstDateOwed(recurring, today);
        if (from.isAfter(today)) return;

        // A previous run may have been interrupted after inserting some days but
        // before recording its progress, so trust the expenses, not the marker.
        Set<LocalDate> alreadyGenerated = new HashSet<>(
                expenseRepository.findGeneratedDates(recurring.getId(), from));

        List<Expense> generated = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1)) {
            if (recurring.getFrequency().matches(date) && !alreadyGenerated.contains(date)) {
                generated.add(toExpense(recurring, date));
            }
        }

        if (!generated.isEmpty()) expenseRepository.saveAll(generated);

        recurring.setLastGeneratedDate(today);
        recurringExpenseRepository.save(recurring);
    }

    private LocalDate firstDateOwed(RecurringExpense recurring, LocalDate today) {
        LocalDate from = recurring.getStartDate();

        LocalDate lastGenerated = recurring.getLastGeneratedDate();
        if (lastGenerated != null && lastGenerated.plusDays(1).isAfter(from)) {
            from = lastGenerated.plusDays(1);
        }

        LocalDate earliest = today.minusDays(MAX_CATCH_UP_DAYS);
        return from.isBefore(earliest) ? earliest : from;
    }

    /**
     * Copies the template's frozen figures onto the day's expense. Deliberately
     * does not re-convert: a fixed RM 5 fee must stay RM 5 rather than drift with
     * the exchange rate.
     */
    private Expense toExpense(RecurringExpense recurring, LocalDate date) {
        return Expense.builder()
                .userId(recurring.getUserId())
                .amount(recurring.getAmount())
                .originalAmount(recurring.getOriginalAmount())
                .originalCurrency(recurring.getOriginalCurrency())
                .exchangeRate(recurring.getExchangeRate())
                .rateSnapshot(recurring.getRateSnapshot())
                .category(recurring.getCategory())
                .description(recurring.getDescription())
                .expenseDate(date)
                .recurringId(recurring.getId())
                .build();
    }

    /** Converts the submitted amount to base and freezes the rates behind it. */
    private void applyAmount(RecurringExpense recurring, RecurringExpenseRequest request) {
        CurrencyCode currency = currencyOf(request);

        // One snapshot, so the stored rate and the stored snapshot cannot disagree.
        Map<CurrencyCode, BigDecimal> snapshot = exchangeRateService.snapshot();
        BigDecimal baseAmount = exchangeRateService.toBaseAt(request.amount(), currency, snapshot);

        if (baseAmount.signum() <= 0) {
            throw new BadRequestException("amount is too small to record in " + CurrencyCode.BASE);
        }

        recurring.setAmount(baseAmount);
        recurring.setOriginalAmount(request.amount());
        recurring.setOriginalCurrency(currency);
        recurring.setExchangeRate(exchangeRateService.rateAt(snapshot, currency));
        recurring.setRateSnapshot(snapshot);
    }

    private RecurringExpense find(UUID userId, UUID id) {
        return recurringExpenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Fixed expense not found"));
    }

    private RecurringExpenseResponse toResponse(RecurringExpense recurring, CurrencyCode currency) {
        return RecurringExpenseResponse.from(recurring, currency, displayAmount(recurring, currency));
    }

    /** Mirrors ExpenseService: valued at the rates captured when it was set. */
    private BigDecimal displayAmount(RecurringExpense recurring, CurrencyCode currency) {
        if (currency == recurring.getOriginalCurrency()) {
            return recurring.getOriginalAmount().setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        }
        return exchangeRateService.fromBaseAt(recurring.getAmount(), currency, recurring.getRateSnapshot());
    }

    /**
     * The client's own date, which is the only one that knows when its day rolled
     * over. Bounded the same way expense entry is: the largest offset in use is
     * UTC+14, so one day past the UTC date accepts every legitimate local today
     * while refusing a date that could not be today anywhere.
     */
    private LocalDate resolveToday(LocalDate clientToday) {
        LocalDate utcToday = LocalDate.now(ZoneOffset.UTC);
        if (clientToday == null) return utcToday;

        LocalDate latestPlausible = utcToday.plusDays(1);
        return clientToday.isAfter(latestPlausible) ? latestPlausible : clientToday;
    }

    private CurrencyCode currencyOf(RecurringExpenseRequest request) {
        return request.currency() == null ? CurrencyCode.BASE : request.currency();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
