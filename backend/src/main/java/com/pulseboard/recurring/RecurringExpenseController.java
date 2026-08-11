package com.pulseboard.recurring;

import com.pulseboard.common.currency.CurrencyCode;
import com.pulseboard.recurring.dto.RecurringExpenseRequest;
import com.pulseboard.recurring.dto.RecurringExpenseResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Fixed expenses that repeat on a schedule. {@code today} is the client's local
 * date: only it knows when the user's day rolled over, and generation is bounded
 * by it.
 */
@RestController
@RequestMapping("/api/recurring-expenses")
@RequiredArgsConstructor
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    /** Amounts are returned already converted into {@code currency}. */
    @GetMapping
    public List<RecurringExpenseResponse> list(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = CurrencyCode.BASE_CODE) CurrencyCode currency) {
        return recurringExpenseService.list(userId, currency);
    }

    @PostMapping
    public ResponseEntity<RecurringExpenseResponse> create(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RecurringExpenseRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringExpenseService.create(userId, request, today));
    }

    @PutMapping("/{id}")
    public RecurringExpenseResponse update(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody RecurringExpenseRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {
        return recurringExpenseService.update(userId, id, request, today);
    }

    /** Pause or resume. Separate from the full update so the amount is untouched. */
    @PatchMapping("/{id}/active")
    public RecurringExpenseResponse setActive(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody ActiveRequest request,
            @RequestParam(defaultValue = CurrencyCode.BASE_CODE) CurrencyCode currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {
        return recurringExpenseService.setActive(userId, id, request.active(), currency, today);
    }

    public record ActiveRequest(@NotNull(message = "is required") Boolean active) {}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        recurringExpenseService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
