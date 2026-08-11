package com.pulseboard.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByUserIdOrderByExpenseDateDescCreatedAtDesc(UUID userId);

    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Days a fixed expense has already been generated for, so a catch-up that
     * was interrupted part way through can resume without duplicating them.
     */
    @Query("SELECT e.expenseDate FROM Expense e "
            + "WHERE e.recurringId = :recurringId AND e.expenseDate >= :from")
    List<LocalDate> findGeneratedDates(@Param("recurringId") UUID recurringId, @Param("from") LocalDate from);
}
