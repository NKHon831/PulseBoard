package com.pulseboard.expense;

import com.pulseboard.common.exception.NotFoundException;
import com.pulseboard.expense.dto.ExpenseRequest;
import com.pulseboard.expense.dto.ExpenseResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public List<ExpenseResponse> list(UUID userId) {
        return expenseRepository.findByUserIdOrderByExpenseDateDescCreatedAtDesc(userId).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    public ExpenseResponse create(UUID userId, ExpenseRequest request) {
        Expense expense = new Expense(
                userId,
                request.amount(),
                request.category().trim(),
                blankToNull(request.description()),
                request.expenseDate());
        expenseRepository.save(expense);
        return ExpenseResponse.from(expense);
    }

    public void delete(UUID userId, UUID expenseId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new NotFoundException("Expense not found"));
        expenseRepository.delete(expense);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
