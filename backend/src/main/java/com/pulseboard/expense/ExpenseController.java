package com.pulseboard.expense;

import com.pulseboard.common.currency.CurrencyCode;
import com.pulseboard.expense.dto.ExpenseBatchRequest;
import com.pulseboard.expense.dto.ExpenseRequest;
import com.pulseboard.expense.dto.ExpenseResponse;
import com.pulseboard.recurring.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final RecurringExpenseService recurringExpenseService;

    /**
     * Amounts are returned already converted into {@code currency}.
     *
     * <p>Any fixed expenses the user is owed are written in first, so the day's
     * entries exist by the time they are read. {@code today} is the client's own
     * date, since the server cannot know when the user's day rolled over.
     */
    @GetMapping
    public List<ExpenseResponse> list(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = CurrencyCode.BASE_CODE) CurrencyCode currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {
        recurringExpenseService.materializeDueFor(userId, today);
        return expenseService.list(userId, currency);
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(
            @AuthenticationPrincipal UUID userId, @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(userId, request));
    }

    /** Records several expenses at once; either all of them are saved or none are. */
    @PostMapping("/batch")
    public ResponseEntity<List<ExpenseResponse>> createBatch(
            @AuthenticationPrincipal UUID userId, @Valid @RequestBody ExpenseBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.createAll(userId, request.expenses()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        expenseService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
