package com.pulseboard.recurring.dto;

import com.pulseboard.common.currency.CurrencyCode;
import com.pulseboard.recurring.RecurrenceFrequency;
import com.pulseboard.recurring.RecurringExpense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A fixed expense rendered in the currency the client asked for. As with
 * expenses, {@code amount} arrives already converted.
 */
public record RecurringExpenseResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        String category,
        String description,
        RecurrenceFrequency frequency,
        boolean active,
        LocalDate startDate,
        LocalDate lastGeneratedDate) {

    public static RecurringExpenseResponse from(
            RecurringExpense recurring, CurrencyCode displayCurrency, BigDecimal displayAmount) {
        return new RecurringExpenseResponse(
                recurring.getId(),
                displayAmount,
                displayCurrency.name(),
                recurring.getCategory(),
                recurring.getDescription(),
                recurring.getFrequency(),
                recurring.isActive(),
                recurring.getStartDate(),
                recurring.getLastGeneratedDate());
    }
}
