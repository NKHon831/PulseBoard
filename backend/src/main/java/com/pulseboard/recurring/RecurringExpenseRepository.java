package com.pulseboard.recurring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, UUID> {

    List<RecurringExpense> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Only active templates generate; paused ones are skipped by the catch-up. */
    List<RecurringExpense> findByUserIdAndActiveTrue(UUID userId);

    Optional<RecurringExpense> findByIdAndUserId(UUID id, UUID userId);
}
