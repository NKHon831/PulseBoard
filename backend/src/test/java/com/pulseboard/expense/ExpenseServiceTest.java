package com.pulseboard.expense;

import com.pulseboard.common.currency.CurrencyCode;
import com.pulseboard.common.currency.ExchangeRateService;
import com.pulseboard.common.exception.BadRequestException;
import com.pulseboard.expense.dto.ExpenseRequest;
import com.pulseboard.expense.dto.ExpenseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the promise that an expense keeps the value it had when it was entered,
 * even after the exchange rate moves. The exchange rate service is real but has
 * no provider configured, so "today's rate" is the static fallback of 40; the
 * snapshots below stand in for "the rate back then".
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final BigDecimal RATE_WHEN_ENTERED = new BigDecimal("50");

    @Mock
    private ExpenseRepository expenseRepository;

    private ExpenseService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseService(expenseRepository, new ExchangeRateService(""));
    }

    /** 4000 JPY entered when 1 MYR bought 50 JPY, i.e. 80 MYR at the time. */
    private Expense enteredInYen() {
        return Expense.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("80.00"))
                .originalAmount(new BigDecimal("4000"))
                .originalCurrency(CurrencyCode.JPY)
                .exchangeRate(RATE_WHEN_ENTERED)
                .rateSnapshot(Map.of(CurrencyCode.JPY, RATE_WHEN_ENTERED))
                .category("Lunch")
                .expenseDate(LocalDate.now())
                .build();
    }

    private Expense enteredInRinggit(Map<CurrencyCode, BigDecimal> snapshot) {
        return Expense.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .originalAmount(new BigDecimal("100.00"))
                .originalCurrency(CurrencyCode.MYR)
                .exchangeRate(BigDecimal.ONE)
                .rateSnapshot(snapshot)
                .category("Dinner")
                .expenseDate(LocalDate.now())
                .build();
    }

    private List<ExpenseResponse> listAs(Expense stored, CurrencyCode currency) {
        when(expenseRepository.findByUserIdOrderByExpenseDateDescCreatedAtDesc(any()))
                .thenReturn(List.of(stored));
        return service.list(UUID.randomUUID(), currency);
    }

    @Test
    void showsExactlyWhatWasTypedWhenViewedInTheEntryCurrency() {
        // At today's fallback rate of 40, 80 MYR would read as 3200 JPY. It must
        // still read as the 4000 JPY that was actually entered.
        assertThat(listAs(enteredInYen(), CurrencyCode.JPY)).singleElement().satisfies(response -> {
            assertThat(response.amount()).isEqualByComparingTo("4000");
            assertThat(response.currency()).isEqualTo("JPY");
        });
    }

    @Test
    void showsTheHistoricalBaseValueWhenViewedInTheBaseCurrency() {
        assertThat(listAs(enteredInYen(), CurrencyCode.MYR)).singleElement().satisfies(response -> {
            assertThat(response.amount()).isEqualByComparingTo("80.00");
            assertThat(response.currency()).isEqualTo("MYR");
        });
    }

    @Test
    void valuesABaseCurrencyExpenseUsingTheRateCapturedWhenItWasEntered() {
        Expense stored = enteredInRinggit(Map.of(CurrencyCode.JPY, RATE_WHEN_ENTERED));

        // 100 MYR at the captured rate of 50, not at today's fallback of 40.
        assertThat(listAs(stored, CurrencyCode.JPY)).singleElement().satisfies(
                response -> assertThat(response.amount()).isEqualByComparingTo("5000"));
    }

    @Test
    void fallsBackToCurrentRatesForRowsSavedBeforeSnapshotsExisted() {
        assertThat(listAs(enteredInRinggit(null), CurrencyCode.JPY)).singleElement().satisfies(
                response -> assertThat(response.amount()).isEqualByComparingTo("4000"));
    }

    @Test
    void newExpensesFreezeTheRatesInForceAtTheMomentTheyAreSaved() {
        ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("4000"), CurrencyCode.JPY, "Lunch", "  ", LocalDate.now());

        ExpenseResponse response = service.create(UUID.randomUUID(), request);

        assertThat(response.amount()).isEqualByComparingTo("4000");

        ArgumentCaptor<Expense> saved = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(saved.capture());
        Expense stored = saved.getValue();

        assertThat(stored.getOriginalAmount()).isEqualByComparingTo("4000");
        assertThat(stored.getOriginalCurrency()).isEqualTo(CurrencyCode.JPY);
        assertThat(stored.getAmount()).isEqualByComparingTo("100.00");
        assertThat(stored.getExchangeRate()).isEqualByComparingTo(CurrencyCode.JPY.getFallbackRate());
        assertThat(stored.getRateSnapshot())
                .containsEntry(CurrencyCode.JPY, CurrencyCode.JPY.getFallbackRate());
        assertThat(stored.getDescription()).isNull();
    }

    @Test
    void acceptsTodayInEveryClientTimezone() {
        // A UTC server must not reject a UTC+8 client's "today", which is what a
        // naive @PastOrPresent on the request DTO used to do.
        LocalDate tomorrowInUtc = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("10"), CurrencyCode.MYR, "Breakfast", null, tomorrowInUtc);

        assertThat(service.create(UUID.randomUUID(), request)).isNotNull();
    }

    @Test
    void stillRejectsDatesThatCannotBeTodayAnywhere() {
        LocalDate wellIntoTheFuture = LocalDate.now(ZoneOffset.UTC).plusDays(2);
        ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("10"), CurrencyCode.MYR, "Breakfast", null, wellIntoTheFuture);

        assertThatThrownBy(() -> service.create(UUID.randomUUID(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be in the future");
    }
}
