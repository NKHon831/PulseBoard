package com.pulseboard.expense.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Several expenses submitted together, so a day's worth of entries can be keyed
 * in one go. They are saved atomically and share a single rate snapshot.
 */
public record ExpenseBatchRequest(
        @NotEmpty(message = "must contain at least one expense")
        @Size(max = MAX_ENTRIES, message = "must contain at most " + MAX_ENTRIES + " expenses")
        List<@Valid ExpenseRequest> expenses) {

    /** Generous for hand entry, but bounded so one request cannot flood the table. */
    public static final int MAX_ENTRIES = 50;
}
