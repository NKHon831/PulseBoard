package com.pulseboard.expense.dto;

import com.pulseboard.common.currency.CurrencyCode;
import com.pulseboard.expense.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * An expense rendered in the currency the client asked for. {@code amount} is
 * already converted by the backend, so clients only need to format it.
 */
public record ExpenseResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        String category,
        String description,
        LocalDate expenseDate,
        /** True when a fixed-expense template put this row here rather than the user. */
        boolean recurring) {

    public static ExpenseResponse from(Expense expense, CurrencyCode displayCurrency, BigDecimal displayAmount) {
        return new ExpenseResponse(
                expense.getId(),
                displayAmount,
                displayCurrency.name(),
                expense.getCategory(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getRecurringId() != null);
    }
}
