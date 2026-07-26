package com.pulseboard.common.currency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Owns every currency conversion in the app. Amounts arrive from clients in
 * whatever currency the user picked, are converted to {@link CurrencyCode#BASE}
 * for storage, and are converted back on the way out.
 *
 * <p>Rates are fetched from the configured provider and cached in memory. If a
 * fetch fails we serve the last good rates, and only fall back to the static
 * approximations on {@link CurrencyCode} when we have never fetched successfully.
 */
@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    /** How long fetched rates are served before we try to refresh them. */
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    /**
     * Kept short: a refresh happens inline on a user request, so a slow provider
     * must not hold up saving an expense. On timeout we fall back to cached rates.
     */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

    private final RestClient restClient;
    private final String apiUrl;
    private final AtomicReference<Snapshot> cache = new AtomicReference<>();

    public ExchangeRateService(@Value("${app.exchange-rate.api-url:}") String apiUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(REQUEST_TIMEOUT);
        requestFactory.setReadTimeout(REQUEST_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.apiUrl = apiUrl;
    }

    /** Multiplier that turns a base-currency amount into {@code code}. */
    public BigDecimal rateFor(CurrencyCode code) {
        if (code.isBase()) return BigDecimal.ONE;
        BigDecimal rate = rates().get(code);
        return (rate == null || rate.signum() <= 0) ? code.getFallbackRate() : rate;
    }

    /** Current rate for every supported currency, keyed from the base currency. */
    public Map<CurrencyCode, BigDecimal> rates() {
        Snapshot cached = cache.get();
        if (cached != null && !cached.isStale()) return cached.rates();

        Map<CurrencyCode, BigDecimal> fetched = fetch();
        if (fetched != null) {
            cache.set(new Snapshot(fetched, Instant.now()));
            return fetched;
        }

        // Live fetch failed — prefer the last good rates over the static guesses.
        return cached != null ? cached.rates() : fallbackRates();
    }

    /**
     * The current rate for every non-base currency, to be frozen onto a record
     * as it is saved so it can be displayed later at the rate of that moment.
     */
    public Map<CurrencyCode, BigDecimal> snapshot() {
        Map<CurrencyCode, BigDecimal> snapshot = new EnumMap<>(CurrencyCode.class);
        rates().forEach((code, rate) -> {
            if (!code.isBase()) snapshot.put(code, rate);
        });
        return snapshot;
    }

    /**
     * The rate for {@code code} as captured in {@code snapshot}, falling back to
     * the current rate when the snapshot has nothing usable for it.
     */
    public BigDecimal rateAt(Map<CurrencyCode, BigDecimal> snapshot, CurrencyCode code) {
        if (code.isBase()) return BigDecimal.ONE;
        BigDecimal rate = (snapshot == null) ? null : snapshot.get(code);
        return (rate == null || rate.signum() <= 0) ? rateFor(code) : rate;
    }

    /** Convert an amount the user entered in {@code from} into the base currency. */
    public BigDecimal toBase(BigDecimal amount, CurrencyCode from) {
        return toBaseAt(amount, from, null);
    }

    /** As {@link #toBase} but pinned to the rates in {@code snapshot}. */
    public BigDecimal toBaseAt(BigDecimal amount, CurrencyCode from, Map<CurrencyCode, BigDecimal> snapshot) {
        if (from.isBase()) return scaleFor(amount, CurrencyCode.BASE);
        return amount.divide(rateAt(snapshot, from), CurrencyCode.BASE.getDecimalPlaces(), RoundingMode.HALF_UP);
    }

    /** Convert a stored base-currency amount into {@code to} for display. */
    public BigDecimal fromBase(BigDecimal baseAmount, CurrencyCode to) {
        return fromBaseAt(baseAmount, to, null);
    }

    /**
     * As {@link #fromBase} but pinned to the rates in {@code snapshot}, so an
     * expense keeps the value it had when it was entered.
     */
    public BigDecimal fromBaseAt(BigDecimal baseAmount, CurrencyCode to, Map<CurrencyCode, BigDecimal> snapshot) {
        if (to.isBase()) return scaleFor(baseAmount, to);
        return scaleFor(baseAmount.multiply(rateAt(snapshot, to)), to);
    }

    private BigDecimal scaleFor(BigDecimal amount, CurrencyCode code) {
        return amount.setScale(code.getDecimalPlaces(), RoundingMode.HALF_UP);
    }

    private Map<CurrencyCode, BigDecimal> fallbackRates() {
        Map<CurrencyCode, BigDecimal> rates = new EnumMap<>(CurrencyCode.class);
        for (CurrencyCode code : CurrencyCode.values()) {
            rates.put(code, code.isBase() ? BigDecimal.ONE : code.getFallbackRate());
        }
        return rates;
    }

    /** Returns null when the provider could not be reached or returned nothing usable. */
    private Map<CurrencyCode, BigDecimal> fetch() {
        String symbols = Arrays.stream(CurrencyCode.values())
                .filter(code -> !code.isBase())
                .map(Enum::name)
                .collect(Collectors.joining(","));

        if (apiUrl == null || apiUrl.isBlank() || symbols.isEmpty()) return null;

        try {
            ProviderRates body = restClient
                    .get()
                    .uri(apiUrl + "?base={base}&symbols={symbols}", CurrencyCode.BASE.name(), symbols)
                    .retrieve()
                    .body(ProviderRates.class);

            if (body == null || body.rates() == null || body.rates().isEmpty()) {
                log.warn("Exchange rate provider returned no rates; using cached or fallback rates");
                return null;
            }

            Map<CurrencyCode, BigDecimal> rates = new EnumMap<>(CurrencyCode.class);
            for (CurrencyCode code : CurrencyCode.values()) {
                if (code.isBase()) {
                    rates.put(code, BigDecimal.ONE);
                    continue;
                }
                BigDecimal rate = body.rates().get(code.name());
                rates.put(code, (rate != null && rate.signum() > 0) ? rate : code.getFallbackRate());
            }
            return rates;
        } catch (Exception ex) {
            log.warn("Exchange rate fetch failed ({}); using cached or fallback rates", ex.toString());
            return null;
        }
    }

    /** Shape of the provider response, e.g. {@code {"base":"MYR","rates":{"JPY":34.2}}}. */
    private record ProviderRates(Map<String, BigDecimal> rates) {}

    private record Snapshot(Map<CurrencyCode, BigDecimal> rates, Instant fetchedAt) {
        boolean isStale() {
            return Duration.between(fetchedAt, Instant.now()).compareTo(CACHE_TTL) > 0;
        }
    }
}
