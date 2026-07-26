package com.pulseboard.expense;

import com.pulseboard.common.currency.CurrencyCode;
import com.pulseboard.common.currency.ExchangeRateService;
import com.pulseboard.common.exception.BadRequestException;
import com.pulseboard.common.exception.NotFoundException;
import com.pulseboard.expense.dto.ExpenseRequest;
import com.pulseboard.expense.dto.ExpenseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        CurrencyCode currency = request.currency() == null ? CurrencyCode.BASE : request.currency();

        // Take one snapshot and derive everything from it, so the stored rate and
        // the stored snapshot can never disagree.
        Map<CurrencyCode, BigDecimal> snapshot = exchangeRateService.snapshot();
        BigDecimal rate = exchangeRateService.rateAt(snapshot, currency);
        BigDecimal baseAmount = exchangeRateService.toBaseAt(request.amount(), currency, snapshot);

        // A tiny foreign amount can round away to nothing in the base currency.
        if (baseAmount.signum() <= 0) {
            throw new BadRequestException("amount is too small to record in " + CurrencyCode.BASE);
        }

        Expense expense = Expense.builder()
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
        expenseRepository.save(expense);
        return toResponse(expense, currency);
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

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
