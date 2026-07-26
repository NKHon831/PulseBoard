package com.pulseboard.common.currency;

import com.pulseboard.common.currency.dto.RatesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final ExchangeRateService exchangeRateService;

    /**
     * Exposes the rates the backend is currently converting with. Clients use
     * this to show the rate — they never convert amounts themselves.
     */
    @GetMapping("/rates")
    public RatesResponse rates() {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        exchangeRateService.rates().forEach((code, rate) -> rates.put(code.name(), rate));
        return new RatesResponse(CurrencyCode.BASE.name(), rates);
    }
}
