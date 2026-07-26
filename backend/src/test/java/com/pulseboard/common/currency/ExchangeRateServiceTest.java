package com.pulseboard.common.currency;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the conversion math against the static fallback rates by leaving the
 * provider URL blank, so these assertions never depend on a live rate.
 */
class ExchangeRateServiceTest {

    private final ExchangeRateService service = new ExchangeRateService("");

    @Test
    void baseCurrencyConvertsToItselfUnchanged() {
        assertThat(service.rateFor(CurrencyCode.MYR)).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(service.toBase(new BigDecimal("42.50"), CurrencyCode.MYR)).isEqualByComparingTo("42.50");
        assertThat(service.fromBase(new BigDecimal("42.50"), CurrencyCode.MYR)).isEqualByComparingTo("42.50");
    }

    @Test
    void foreignAmountIsConvertedIntoTheBaseCurrencyOnTheWayIn() {
        // 4000 JPY at the fallback rate of 40 is 100 MYR -- not 4000 MYR, which
        // was the bug this refactor removes.
        assertThat(service.toBase(new BigDecimal("4000"), CurrencyCode.JPY)).isEqualByComparingTo("100.00");
    }

    @Test
    void storedBaseAmountIsConvertedBackOutForDisplay() {
        assertThat(service.fromBase(new BigDecimal("100.00"), CurrencyCode.JPY)).isEqualByComparingTo("4000");
    }

    @Test
    void enteringAndReadingBackInTheSameCurrencyRoundTrips() {
        BigDecimal entered = new BigDecimal("4000");
        BigDecimal stored = service.toBase(entered, CurrencyCode.JPY);
        assertThat(service.fromBase(stored, CurrencyCode.JPY)).isEqualByComparingTo(entered);
    }

    @Test
    void displayAmountsUseTheCurrencyScale() {
        // JPY is quoted without decimals, MYR with two.
        assertThat(service.fromBase(new BigDecimal("100.00"), CurrencyCode.JPY).scale()).isZero();
        assertThat(service.fromBase(new BigDecimal("100"), CurrencyCode.MYR).scale()).isEqualTo(2);
    }

    @Test
    void fallbackRatesAreUsedWhenNoProviderIsConfigured() {
        assertThat(service.rates())
                .containsEntry(CurrencyCode.MYR, BigDecimal.ONE)
                .containsEntry(CurrencyCode.JPY, CurrencyCode.JPY.getFallbackRate());
    }

    @Test
    void snapshotCoversEveryNonBaseCurrency() {
        assertThat(service.snapshot())
                .containsOnlyKeys(CurrencyCode.JPY)
                .containsEntry(CurrencyCode.JPY, CurrencyCode.JPY.getFallbackRate());
    }

    @Test
    void storedSnapshotRateWinsOverTheCurrentRate() {
        // The expense was entered when 1 MYR bought 50 JPY; today's rate is 40.
        Map<CurrencyCode, BigDecimal> snapshot = Map.of(CurrencyCode.JPY, new BigDecimal("50"));

        assertThat(service.rateAt(snapshot, CurrencyCode.JPY)).isEqualByComparingTo("50");
        assertThat(service.fromBaseAt(new BigDecimal("100.00"), CurrencyCode.JPY, snapshot))
                .isEqualByComparingTo("5000");
        assertThat(service.toBaseAt(new BigDecimal("5000"), CurrencyCode.JPY, snapshot))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void currentRateIsUsedWhenTheSnapshotHasNothingUsable() {
        // Rows saved before snapshots existed, or a currency added later.
        assertThat(service.fromBaseAt(new BigDecimal("100.00"), CurrencyCode.JPY, null))
                .isEqualByComparingTo("4000");
        assertThat(service.fromBaseAt(new BigDecimal("100.00"), CurrencyCode.JPY, Map.of()))
                .isEqualByComparingTo("4000");
    }

    @Test
    void baseCurrencyIgnoresTheSnapshotEntirely() {
        Map<CurrencyCode, BigDecimal> snapshot = Map.of(CurrencyCode.JPY, new BigDecimal("50"));

        assertThat(service.rateAt(snapshot, CurrencyCode.MYR)).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(service.fromBaseAt(new BigDecimal("42.50"), CurrencyCode.MYR, snapshot))
                .isEqualByComparingTo("42.50");
    }
}
