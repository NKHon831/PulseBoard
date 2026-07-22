package com.pulseboard.expense;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    List<Expense> findByUserIdOrderByExpenseDateDescCreatedAtDesc(UUID userId);

    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);
}
