package com.pulseboard.common.currency;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RateSnapshotConverterTest {

    private final RateSnapshotConverter converter = new RateSnapshotConverter();

    @Test
    void ratesSurviveARoundTripThroughTheColumn() {
        Map<CurrencyCode, BigDecimal> rates = new EnumMap<>(CurrencyCode.class);
        rates.put(CurrencyCode.JPY, new BigDecimal("40.045"));

        String encoded = converter.convertToDatabaseColumn(rates);

        assertThat(encoded).isEqualTo("JPY=40.045");
        assertThat(converter.convertToEntityAttribute(encoded))
                .containsEntry(CurrencyCode.JPY, new BigDecimal("40.045"));
    }

    @Test
    void emptyAndNullRatesStoreAsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(Map.of())).isNull();
    }

    @Test
    void missingColumnReadsAsAnEmptyMapRatherThanNull() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
        assertThat(converter.convertToEntityAttribute("   ")).isEmpty();
    }

    @Test
    void unparseableEntriesAreSkippedInsteadOfFailing() {
        // An unknown currency, a bad number and a malformed pair must not stop the
        // rest of the snapshot from loading -- callers fall back to live rates.
        Map<CurrencyCode, BigDecimal> rates =
                converter.convertToEntityAttribute("XYZ=1.5;JPY=abc;=9;JPY=40.045");

        assertThat(rates).containsExactly(Map.entry(CurrencyCode.JPY, new BigDecimal("40.045")));
    }

    @Test
    void plainStringNotationAvoidsScientificNotation() {
        Map<CurrencyCode, BigDecimal> rates = Map.of(CurrencyCode.JPY, new BigDecimal("0.00000001"));

        assertThat(converter.convertToDatabaseColumn(rates)).isEqualTo("JPY=0.00000001");
    }
}
