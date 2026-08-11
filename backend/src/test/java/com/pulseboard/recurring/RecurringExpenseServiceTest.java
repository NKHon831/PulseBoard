package com.pulseboard.recurring;

import com.pulseboard.common.currency.CurrencyCode;
import com.pulseboard.common.currency.ExchangeRateService;
import com.pulseboard.expense.Expense;
import com.pulseboard.expense.ExpenseRepository;
import com.pulseboard.recurring.dto.RecurringExpenseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers which days a fixed expense generates for, and the promise that it keeps
 * the amount it was set at. The exchange rate service is real but has no
 * provider configured, so "today's rate" is the static fallback of 40; the
 * snapshots below stand in for "the rate back then".
 */
@ExtendWith(MockitoExtension.class)
class RecurringExpenseServiceTest {

    /** 2024-01-01 was a Monday, so this week runs Monday to Sunday. */
    private static final LocalDate MONDAY = LocalDate.of(2024, 1, 1);
    private static final LocalDate SUNDAY = LocalDate.of(2024, 1, 7);

    private static final BigDecimal RATE_WHEN_SET = new BigDecimal("50");

    @Mock
    private RecurringExpenseRepository recurringExpenseRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    private RecurringExpenseService service;

    @BeforeEach
    void setUp() {
        service = new RecurringExpenseService(
                recurringExpenseRepository, expenseRepository, new ExchangeRateService(""));
    }

    private RecurringExpense template(RecurrenceFrequency frequency, LocalDate startDate) {
        return RecurringExpense.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(new BigDecimal("80.00"))
                .originalAmount(new BigDecimal("4000"))
                .originalCurrency(CurrencyCode.JPY)
                .exchangeRate(RATE_WHEN_SET)
                .rateSnapshot(Map.of(CurrencyCode.JPY, RATE_WHEN_SET))
                .category("Lunch")
                .description("Office parking")
                .frequency(frequency)
                .active(true)
                .startDate(startDate)
                .build();
    }

    /** Runs the catch-up for one template and returns what it wrote. */
    private List<Expense> generate(RecurringExpense recurring, LocalDate today) {
        when(recurringExpenseRepository.findByUserIdAndActiveTrue(any())).thenReturn(List.of(recurring));

        service.materializeDueFor(recurring.getUserId(), today);

        ArgumentCaptor<List<Expense>> saved = ArgumentCaptor.captor();
        verify(expenseRepository).saveAll(saved.capture());
        return saved.getValue();
    }

    @Test
    void generatesEveryDayFromTheStartDateUpToToday() {
        List<Expense> generated = generate(template(RecurrenceFrequency.DAILY, MONDAY), SUNDAY);

        assertThat(generated).hasSize(7);
        assertThat(generated).extracting(Expense::getExpenseDate).startsWith(MONDAY).endsWith(SUNDAY);
    }

    @Test
    void skipsWeekendsForAWeekdayOnlyExpense() {
        List<Expense> generated = generate(template(RecurrenceFrequency.WEEKDAYS, MONDAY), SUNDAY);

        assertThat(generated).extracting(Expense::getExpenseDate)
                .containsExactly(MONDAY, MONDAY.plusDays(1), MONDAY.plusDays(2), MONDAY.plusDays(3), MONDAY.plusDays(4));
    }

    @Test
    void generatesOnlySaturdayAndSundayForAWeekendOnlyExpense() {
        List<Expense> generated = generate(template(RecurrenceFrequency.WEEKENDS, MONDAY), SUNDAY);

        assertThat(generated).extracting(Expense::getExpenseDate).containsExactly(SUNDAY.minusDays(1), SUNDAY);
    }

    @Test
    void doesNotGenerateAgainForDaysItHasAlreadyWritten() {
        RecurringExpense recurring = template(RecurrenceFrequency.DAILY, MONDAY);
        // A previous run wrote the first two days, then stopped before recording
        // how far it got, so the marker cannot be trusted on its own.
        when(expenseRepository.findGeneratedDates(any(), any())).thenReturn(List.of(MONDAY, MONDAY.plusDays(1)));

        assertThat(generate(recurring, SUNDAY)).extracting(Expense::getExpenseDate)
                .doesNotContain(MONDAY, MONDAY.plusDays(1))
                .hasSize(5);
    }

    @Test
    void resumesFromWhereTheLastRunLeftOff() {
        RecurringExpense recurring = template(RecurrenceFrequency.DAILY, MONDAY);
        recurring.setLastGeneratedDate(MONDAY.plusDays(4));

        assertThat(generate(recurring, SUNDAY)).extracting(Expense::getExpenseDate)
                .containsExactly(MONDAY.plusDays(5), MONDAY.plusDays(6));
    }

    @Test
    void keepsTheAmountItWasSetAtRatherThanRepricingItEachDay() {
        Expense generated = generate(template(RecurrenceFrequency.DAILY, SUNDAY), SUNDAY).getFirst();

        // Re-converting 4000 JPY at today's fallback rate of 40 would make this
        // 100.00 -- the fixed amount must not drift with the exchange rate.
        assertThat(generated.getAmount()).isEqualByComparingTo("80.00");
        assertThat(generated.getOriginalAmount()).isEqualByComparingTo("4000");
        assertThat(generated.getOriginalCurrency()).isEqualTo(CurrencyCode.JPY);
        assertThat(generated.getRateSnapshot()).containsEntry(CurrencyCode.JPY, RATE_WHEN_SET);
        assertThat(generated.getCategory()).isEqualTo("Lunch");
        assertThat(generated.getDescription()).isEqualTo("Office parking");
    }

    @Test
    void marksGeneratedExpensesAsComingFromTheirTemplate() {
        RecurringExpense recurring = template(RecurrenceFrequency.DAILY, SUNDAY);

        assertThat(generate(recurring, SUNDAY).getFirst().getRecurringId()).isEqualTo(recurring.getId());
    }

    @Test
    void doesNotReachBackFurtherThanAYearWhenCatchingUp() {
        RecurringExpense recurring = template(RecurrenceFrequency.DAILY, MONDAY.minusYears(5));

        List<Expense> generated = generate(recurring, SUNDAY);

        assertThat(generated).hasSize(367);
        assertThat(generated.getFirst().getExpenseDate()).isEqualTo(SUNDAY.minusDays(366));
    }

    @Test
    void recordsHowFarItGotSoTheNextRunHasNothingToRepeat() {
        RecurringExpense recurring = template(RecurrenceFrequency.DAILY, MONDAY);

        generate(recurring, SUNDAY);

        assertThat(recurring.getLastGeneratedDate()).isEqualTo(SUNDAY);
        verify(recurringExpenseRepository).save(recurring);
    }

    @Test
    void aPausedExpenseGeneratesNothing() {
        RecurringExpenseRequest request = new RecurringExpenseRequest(
                new BigDecimal("5"), CurrencyCode.MYR, "Others", "Parking",
                RecurrenceFrequency.DAILY, MONDAY, false);

        service.create(UUID.randomUUID(), request, SUNDAY);

        verify(expenseRepository, never()).saveAll(anyList());
    }

    @Test
    void aNewFixedExpenseGeneratesTheDaysItAlreadyOwesStraightAway() {
        RecurringExpenseRequest request = new RecurringExpenseRequest(
                new BigDecimal("5"), CurrencyCode.MYR, "Others", "Parking",
                RecurrenceFrequency.DAILY, MONDAY, null);

        service.create(UUID.randomUUID(), request, MONDAY.plusDays(2));

        ArgumentCaptor<List<Expense>> saved = ArgumentCaptor.captor();
        verify(expenseRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(3);
    }
}
