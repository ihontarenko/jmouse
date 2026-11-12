package org.jmouse.suppliers;

import org.jmouse.http.client.HttpCall;
import org.jmouse.http.client.HttpReply;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DigiKeyDriverTest {

    private static final SupplierAccess ACCESS = new SupplierAccess("DigiKey", null,
            new SupplierCredentials.ClientCredentials("client", "se+cret"), null);

    private static final String PRODUCTS = """
            {"ProductsCount":1,"Products":[{"ManufacturerProductNumber":"LM358DR","Manufacturer":{"Id":1,"Name":"Texas Instruments"},
             "Description":{"ProductDescription":"IC OPAMP","DetailedDescription":"General Purpose Amplifier"},
             "ProductUrl":"https://digikey.example/lm358","DatasheetUrl":"https://digikey.example/lm358.pdf","PhotoUrl":null,
             "ProductStatus":{"Status":"Active"},"Category":{"Name":"Amplifiers"},
             "ProductVariations":[{"DigiKeyProductNumber":"296-LM358DRCT-ND","QuantityAvailableforPackageType":800,
               "MinimumOrderQuantity":1,"StandardPricing":[{"BreakQuantity":1,"UnitPrice":0.42},{"BreakQuantity":10,"UnitPrice":null},{"BreakQuantity":100,"UnitPrice":0.21}]}]},
             {"ManufacturerProductNumber":"BARE","ProductVariations":[]}]}""";

    private final List<HttpCall> calls = new ArrayList<>();
    private int searchStatus = 200;
    private String tokenAnswer = "{\"access_token\":\"token-1\",\"expires_in\":600}";
    private Instant now = Instant.parse("2026-11-02T00:00:00Z");

    private final Clock clock = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    };

    private final DigiKeyDriver driver = new DigiKeyDriver(call -> {
        calls.add(call);
        boolean tokenCall = call.uri().getPath().equals("/v1/oauth2/token");
        String body = tokenCall ? tokenAnswer : searchStatus == 200 ? PRODUCTS : "refused";
        return new HttpReply(call.uri(), tokenCall ? 200 : searchStatus, Map.of(), body.getBytes(StandardCharsets.UTF_8));
    }, clock, DigiKeyDriver.Locale.UNITED_STATES);

    private long tokenCalls() {
        return calls.stream().filter(call -> call.uri().getPath().equals("/v1/oauth2/token")).count();
    }

    @Test
    void asksForATokenThenSearchesWithItAndItsLocale() {
        driver.search(ACCESS, "LM358", 500);
        HttpCall token = calls.getFirst();
        assertEquals("https://api.digikey.com/v1/oauth2/token", token.uri().toString());
        assertEquals("grant_type=client_credentials&client_id=client&client_secret=se%2Bcret", new String(token.body().orElseThrow().bytes(), StandardCharsets.UTF_8));
        HttpCall search = calls.getLast();
        assertEquals("https://api.digikey.com/products/v4/search/keyword", search.uri().toString());
        String headers = search.headers().toString();
        assertTrue(headers.contains("Bearer token-1"), headers);
        assertTrue(headers.contains("client"), headers);
        assertTrue(headers.contains("USD"), headers);
        assertTrue(new String(search.body().orElseThrow().bytes(), StandardCharsets.UTF_8).contains("\"limit\":50"));
    }

    @Test
    void readsAnOfferFromItsFirstVariation() {
        List<SupplierOffer> offers = driver.search(ACCESS, "LM358", 10);
        SupplierOffer offer = offers.getFirst();
        assertEquals("296-LM358DRCT-ND", offer.supplierPartNumber());
        assertEquals("Texas Instruments", offer.manufacturer());
        assertEquals(800L, offer.stock());
        assertEquals("USD", offer.currency());
        assertEquals("Active", offer.status());
        assertEquals(List.of(new PriceBreak(1, new BigDecimal("0.42")), new PriceBreak(100, new BigDecimal("0.21"))), offer.priceBreaks());
        assertNull(offer.image());
        SupplierOffer bare = offers.get(1);
        assertNull(bare.stock());
        assertNull(bare.supplierPartNumber());
    }

    @Test
    void keepsTheTokenUntilShortlyBeforeItExpires() {
        driver.search(ACCESS, "a", 10);
        now = now.plus(Duration.ofSeconds(539));
        driver.search(ACCESS, "b", 10);
        assertEquals(1, tokenCalls());
        now = now.plus(Duration.ofSeconds(1));
        driver.search(ACCESS, "c", 10);
        assertEquals(2, tokenCalls());
    }

    @Test
    void aTokenWithNoLifetimeIsTrustedBriefly() {
        tokenAnswer = "{\"access_token\":\"token-1\"}";
        driver.search(ACCESS, "a", 10);
        now = now.plus(Duration.ofSeconds(240));
        driver.search(ACCESS, "b", 10);
        assertEquals(2, tokenCalls());
    }

    @Test
    void aTokenIsNeverSharedBetweenCredentials() {
        driver.search(ACCESS, "a", 10);
        driver.search(new SupplierAccess("DigiKey", null, new SupplierCredentials.ClientCredentials("client", "rotated"), null), "b", 10);
        assertEquals(2, tokenCalls());
    }

    @Test
    void aRefusedTokenIsEvictedSoTheNextSearchAsksAgain() {
        searchStatus = 401;
        assertEquals(SupplierFailure.Reason.CREDENTIALS_REFUSED, assertThrows(SupplierFailure.class, () -> driver.search(ACCESS, "a", 10)).reason());
        searchStatus = 200;
        driver.search(ACCESS, "b", 10);
        assertEquals(2, tokenCalls());
    }

    @Test
    void anotherRefusalKeepsTheToken() {
        searchStatus = 429;
        assertEquals(SupplierFailure.Reason.RATE_LIMITED, assertThrows(SupplierFailure.class, () -> driver.search(ACCESS, "a", 10)).reason());
        searchStatus = 200;
        driver.search(ACCESS, "b", 10);
        assertEquals(1, tokenCalls());
    }

    @Test
    void noTokenIssuedIsAFailureAndNamesNoSecret() {
        tokenAnswer = "{\"error\":\"invalid_client\"}";
        SupplierFailure failure = assertThrows(SupplierFailure.class, () -> driver.search(ACCESS, "a", 10));
        assertEquals(SupplierFailure.Reason.UNREADABLE, failure.reason());
        assertFalse(failure.getMessage().contains("se+cret"));
    }
}
