package com.pulseboard.common.currency;

import java.math.BigDecimal;

/**
 * Currencies the app understands. Every amount is persisted in {@link #BASE};
 * all other currencies are derived from it using the rates supplied by
 * {@link ExchangeRateService}.
 *
 * <p>Keep in sync with CURRENCIES in frontend/src/lib/currencies.ts.
 */
public enum CurrencyCode {

    MYR(2, BigDecimal.ONE),
    // Fallback is a rough approximation only; the live rate wins whenever available.
    JPY(0, new BigDecimal("40"));

    /** Compile-time constant so it can be used as a {@code @RequestParam} default. */
    public static final String BASE_CODE = "MYR";

    /** The currency all stored amounts are denominated in. */
    public static final CurrencyCode BASE = valueOf(BASE_CODE);

    private final int decimalPlaces;
    private final BigDecimal fallbackRate;

    CurrencyCode(int decimalPlaces, BigDecimal fallbackRate) {
        this.decimalPlaces = decimalPlaces;
        this.fallbackRate = fallbackRate;
    }

    /** Digits this currency is conventionally quoted to — JPY has none. */
    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    /**
     * Rough rate from the base currency, used only when neither a live nor a
     * cached rate is available.
     */
    public BigDecimal getFallbackRate() {
        return fallbackRate;
    }

    public boolean isBase() {
        return this == BASE;
    }
}
