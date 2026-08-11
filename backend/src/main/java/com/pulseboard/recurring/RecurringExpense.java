package com.pulseboard.recurring;

import com.pulseboard.common.currency.CurrencyCode;
import com.pulseboard.common.currency.RateSnapshotConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * A fixed expense the user enters once and that is then written into their
 * expenses on every matching day. Kept separate from {@link
 * com.pulseboard.expense.Expense}: this is the rule, those are the money.
 */
@Entity
@Table(name = "recurring_expenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RecurringExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Always denominated in {@link CurrencyCode#BASE}. */
    @Column(nullable = false)
    private BigDecimal amount;

    /** What the user actually typed, in {@link #originalCurrency}. */
    @Column(name = "original_amount", nullable = false)
    private BigDecimal originalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_currency", nullable = false, length = 3)
    private CurrencyCode originalCurrency;

    /** Base -> originalCurrency rate applied when this template was last saved. */
    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate;

    /**
     * Rates in force when the template was last saved. Copied onto every expense
     * it generates, so a fixed amount keeps the value it was set at rather than
     * being re-priced every day.
     */
    @Convert(converter = RateSnapshotConverter.class)
    @Column(name = "rate_snapshot")
    private Map<CurrencyCode, BigDecimal> rateSnapshot;

    @Column(nullable = false)
    private String category;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    /** Paused templates keep their history but stop generating. */
    @Column(nullable = false)
    private boolean active;

    /** No expense is ever generated for a date before this. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Last date generation has caught up to; null until the first run. */
    @Column(name = "last_generated_date")
    private LocalDate lastGeneratedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
