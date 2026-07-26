package com.pulseboard.expense.dto;

import com.pulseboard.common.currency.CurrencyCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        // The amount as typed by the user, denominated in `currency`.
        @NotNull(message = "is required")
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        BigDecimal amount,

        // Currency of `amount`; defaults to the base currency when omitted.
        CurrencyCode currency,

        // Keep in sync with CATEGORIES in frontend/src/pages/Expenses.tsx
        @Pattern(regexp = "Breakfast|Lunch|Dinner|Others", message = "must be one of Breakfast, Lunch, Dinner, Others")
        String category,

        @Size(max = 255, message = "must be at most 255 characters")
        String description,

        @NotNull(message = "is required")
        @PastOrPresent(message = "cannot be in the future")
        LocalDate expenseDate) {
}
