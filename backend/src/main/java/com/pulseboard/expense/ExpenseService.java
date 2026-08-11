package com.pulseboard.expense;

import com.pulseboard.common.currency.CurrencyCode;
import com.pulseboard.common.currency.ExchangeRateService;
import com.pulseboard.common.exception.BadRequestException;
import com.pulseboard.common.exception.NotFoundException;
import com.pulseboard.expense.dto.ExpenseRequest;
import com.pulseboard.expense.dto.ExpenseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExchangeRateService exchangeRateService;

    /** Lists the user's expenses converted into {@code currency}. */
    public List<ExpenseResponse> list(UUID userId, CurrencyCode currency) {
        return expenseRepository.findByUserIdOrderByExpenseDateDescCreatedAtDesc(userId).stream()
                .map(expense -> toResponse(expense, currency))
                .toList();
    }

    /**
     * Converts the submitted amount into the base currency and stores it with
     * the rates in force right now, so the expense keeps this value for good.
     */
    public ExpenseResponse create(UUID userId, ExpenseRequest request) {
        // Take one snapshot and derive everything from it, so the stored rate and
        // the stored snapshot can never disagree.
        Map<CurrencyCode, BigDecimal> snapshot = exchangeRateService.snapshot();
        Expense expense = build(userId, request, snapshot);
        expenseRepository.save(expense);
        return toResponse(expense, currencyOf(request));
    }

    /**
     * Saves several expenses in one go, so a day's entries need not be keyed one
     * at a time. All or nothing: if any entry is rejected, none are stored, and
     * they all share a single rate snapshot so entries made together cannot end
     * up valued at slightly different rates.
     */
    @Transactional
    public List<ExpenseResponse> createAll(UUID userId, List<ExpenseRequest> requests) {
        Map<CurrencyCode, BigDecimal> snapshot = exchangeRateService.snapshot();

        List<Expense> expenses = requests.stream()
                .map(request -> build(userId, request, snapshot))
                .toList();

        expenseRepository.saveAll(expenses);

        // Each entry reads back in the currency it was typed in, as create() does.
        return IntStream.range(0, expenses.size())
                .mapToObj(i -> toResponse(expenses.get(i), currencyOf(requests.get(i))))
                .toList();
    }

    private Expense build(UUID userId, ExpenseRequest request, Map<CurrencyCode, BigDecimal> snapshot) {
        requireNotFutureDated(request.expenseDate());

        CurrencyCode currency = currencyOf(request);
        BigDecimal rate = exchangeRateService.rateAt(snapshot, currency);
        BigDecimal baseAmount = exchangeRateService.toBaseAt(request.amount(), currency, snapshot);

        // A tiny foreign amount can round away to nothing in the base currency.
        if (baseAmount.signum() <= 0) {
            throw new BadRequestException("amount is too small to record in " + CurrencyCode.BASE);
        }

        return Expense.builder()
                .userId(userId)
                .amount(baseAmount)
                .originalAmount(request.amount())
                .originalCurrency(currency)
                .exchangeRate(rate)
                .rateSnapshot(snapshot)
                .category(request.category().trim())
                .description(blankToNull(request.description()))
                .expenseDate(request.expenseDate())
                .build();
    }

    private CurrencyCode currencyOf(ExpenseRequest request) {
        return request.currency() == null ? CurrencyCode.BASE : request.currency();
    }

    public void delete(UUID userId, UUID expenseId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new NotFoundException("Expense not found"));
        expenseRepository.delete(expense);
    }

    private ExpenseResponse toResponse(Expense expense, CurrencyCode currency) {
        return ExpenseResponse.from(expense, currency, displayAmount(expense, currency));
    }

    /**
     * Values an expense using the rates captured when it was entered, never the
     * current ones, so a logged amount does not drift as the market moves.
     */
    private BigDecimal displayAmount(Expense expense, CurrencyCode currency) {
        // Viewed in the currency it was typed in, it should read back exactly as
        // typed -- no rounding through the base currency.
        if (currency == expense.getOriginalCurrency()) {
            return expense.getOriginalAmount().setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        }
        return exchangeRateService.fromBaseAt(expense.getAmount(), currency, expense.getRateSnapshot());
    }

    /**
     * Rejects dates that cannot be "today" anywhere on earth.
     *
     * <p>The server does not know the client's timezone, and its own is not a
     * safe proxy: a UTC server would reject a perfectly valid "today" from a
     * UTC+8 client for the first eight hours of every day. Since the largest UTC
     * offset in use is +14, allowing one day beyond the UTC date accepts every
     * legitimate local today while still blocking genuinely future entries. The
     * client enforces the exact rule, because only it knows the user's timezone.
     */
    private void requireNotFutureDated(LocalDate expenseDate) {
        if (expenseDate.isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(1))) {
            throw new BadRequestException("expenseDate cannot be in the future");
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
