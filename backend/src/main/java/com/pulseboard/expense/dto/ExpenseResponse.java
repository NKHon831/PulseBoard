package com.pulseboard.expense.dto;

import com.pulseboard.expense.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(
        UUID id, BigDecimal amount, String category, String description, LocalDate expenseDate) {
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getDescription(),
                expense.getExpenseDate());
    }
}
