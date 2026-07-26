package com.pulseboard.common.currency.dto;

import java.math.BigDecimal;
import java.util.Map;

/** Rates from {@code base} to each supported currency, for display only. */
public record RatesResponse(String base, Map<String, BigDecimal> rates) {}
