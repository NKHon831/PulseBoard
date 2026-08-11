package com.pulseboard.recurring.dto;

import com.pulseboard.common.currency.CurrencyCode;
import com.pulseboard.expense.dto.ExpenseRequest;
import com.pulseboard.recurring.RecurrenceFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringExpenseRequest(
        // The amount as typed by the user, denominated in `currency`.
        @NotNull(message = "is required")
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        BigDecimal amount,

        // Currency of `amount`; defaults to the base currency when omitted.
        CurrencyCode currency,

        @NotNull(message = "is required")
        @Pattern(regexp = ExpenseRequest.CATEGORY_PATTERN, message = ExpenseRequest.CATEGORY_MESSAGE)
        String category,

        @Size(max = 255, message = "must be at most 255 characters")
        String description,

        @NotNull(message = "is required")
        RecurrenceFrequency frequency,

        // The first day this can generate an expense for. Defaults to the
        // client's today when omitted, so a new fixed expense starts now rather
        // than backfilling a history the user never spent.
        LocalDate startDate,

        // Null on create means active.
        Boolean active) {
}
