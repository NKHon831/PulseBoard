package com.pulseboard.common.currency;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Persists the set of base-to-currency rates captured when a record was saved,
 * as compact text such as {@code "JPY=40.045"}. Plain text keeps the mapping
 * portable and avoids depending on a JSON column type.
 */
@Converter
public class RateSnapshotConverter implements AttributeConverter<Map<CurrencyCode, BigDecimal>, String> {

    private static final String ENTRY_SEPARATOR = ";";
    private static final char KEY_VALUE_SEPARATOR = '=';

    @Override
    public String convertToDatabaseColumn(Map<CurrencyCode, BigDecimal> rates) {
        if (rates == null || rates.isEmpty()) return null;

        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<CurrencyCode, BigDecimal> entry : rates.entrySet()) {
            if (entry.getValue() == null) continue;
            if (!encoded.isEmpty()) encoded.append(ENTRY_SEPARATOR);
            encoded.append(entry.getKey().name())
                    .append(KEY_VALUE_SEPARATOR)
                    .append(entry.getValue().toPlainString());
        }
        return encoded.isEmpty() ? null : encoded.toString();
    }

    @Override
    public Map<CurrencyCode, BigDecimal> convertToEntityAttribute(String value) {
        Map<CurrencyCode, BigDecimal> rates = new EnumMap<>(CurrencyCode.class);
        if (value == null || value.isBlank()) return rates;

        for (String entry : value.split(ENTRY_SEPARATOR)) {
            int separator = entry.indexOf(KEY_VALUE_SEPARATOR);
            if (separator <= 0) continue;
            try {
                CurrencyCode code = CurrencyCode.valueOf(entry.substring(0, separator).trim());
                rates.put(code, new BigDecimal(entry.substring(separator + 1).trim()));
            } catch (IllegalArgumentException ex) {
                // A currency we no longer support, or a malformed number. Skip it
                // and let the caller fall back to the current rate.
            }
        }
        return rates;
    }
}
