package com.pulseboard.expense;

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

@Entity
@Table(name = "expenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Expense {

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

    /** Base -> originalCurrency rate applied when this expense was saved. */
    @Column(name = "exchange_rate", nullable = false)
    private BigDecimal exchangeRate;

    /**
     * Every base -> currency rate in force when this expense was saved, so it can
     * be shown in any currency at the value it had then rather than today's.
     * Null for rows created before snapshots existed.
     */
    @Convert(converter = RateSnapshotConverter.class)
    @Column(name = "rate_snapshot")
    private Map<CurrencyCode, BigDecimal> rateSnapshot;

    @Column(nullable = false)
    private String category;

    private String description;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
