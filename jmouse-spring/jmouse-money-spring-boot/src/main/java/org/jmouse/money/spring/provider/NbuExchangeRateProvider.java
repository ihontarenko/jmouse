package org.jmouse.money.spring.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jmouse.money.CurrencyCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 🇺🇦 The National Bank of Ukraine's published rates.
 *
 * <p>A public endpoint, no key, no documented rate limit. Everything is quoted against the hryvnia,
 * which is therefore this provider's pivot — and the hryvnia itself is not in the feed, because a
 * currency is not quoted against itself.</p>
 *
 * <p>⚠️ <strong>Unreadable codes are dropped here rather than stored.</strong> The feed carries entries
 * for things that are not ISO currencies at all — bullion, accounting units — and a row keyed on one of
 * those is a row nothing will ever match while looking exactly like data. Dropped and logged once,
 * counted rather than listed, because the list is the same every time and would fill a log daily.</p>
 */
public class NbuExchangeRateProvider implements ExchangeRateProvider {

    /** 🏷️ What this provider is called in configuration and in logs. */
    public static final String NAME = "nbu";

    /** 🌐 Where the rates are published. */
    public static final String DEFAULT_BASE_URL = "https://bank.gov.ua";

    private static final String EXCHANGE_PATH = "/NBUStatService/v1/statdirectory/exchange?json";

    private static final Logger LOGGER = LoggerFactory.getLogger(NbuExchangeRateProvider.class);

    private final RestClient   restClient;
    private final CurrencyCode pivot;

    public NbuExchangeRateProvider(RestClient.Builder restClientBuilder, String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.pivot      = CurrencyCode.required("UAH");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CurrencyCode pivot() {
        return pivot;
    }

    @Override
    public Map<CurrencyCode, BigDecimal> fetchRatesToPivot() {
        NbuEntry[] entries = restClient.get().uri(EXCHANGE_PATH).retrieve().body(NbuEntry[].class);

        if (entries == null) {
            LOGGER.warn("💱 The NBU feed answered with nothing at all — no rate was changed");
            return Map.of();
        }

        Map<CurrencyCode, BigDecimal> rates = new LinkedHashMap<>();
        int                           unreadable = 0;

        for (NbuEntry entry : Arrays.stream(entries).filter(candidate -> candidate.isUsable()).toList()) {
            Optional<CurrencyCode> currency = CurrencyCode.of(entry.currencyCode());

            if (currency.isEmpty()) {
                unreadable++;
                continue;
            }

            // ⚠️ First wins. The feed has been seen to repeat a code; taking the later one would make the
            // stored rate depend on the order a bank happened to serialise its array in.
            rates.putIfAbsent(currency.get(), entry.rate());
        }

        LOGGER.info("💱 NBU published {} rate(s) against {}{}", rates.size(), pivot,
                    unreadable == 0 ? "" : " — " + unreadable + " entr(y/ies) named nothing this library recognises");

        return rates;
    }

    /**
     * 📄 One line of the feed.
     *
     * <p>⚠️ Unknown properties are ignored: the feed carries a handful of fields nobody here reads, and
     * a bank adding one more must not stop an application from syncing.</p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NbuEntry(@JsonProperty("cc") String currencyCode, @JsonProperty("rate") BigDecimal rate) {

        /** ⚠️ A null or non-positive rate is not a rate. Dividing by one later would be the real damage. */
        boolean isUsable() {
            return currencyCode != null && rate != null && rate.signum() > 0;
        }
    }
}
