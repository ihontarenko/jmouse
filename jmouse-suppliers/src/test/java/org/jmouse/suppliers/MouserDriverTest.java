package org.jmouse.suppliers;

import org.jmouse.http.client.HttpCall;
import org.jmouse.http.client.HttpCallException;
import org.jmouse.http.client.HttpReply;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MouserDriverTest {

    private static final String KEY = "se+cret key";

    private static final SupplierAccess ACCESS = new SupplierAccess("Mouser", null, new SupplierCredentials.ApiKey(KEY), null);

    private static final String ANSWER = """
            {"Errors":[],"SearchResults":{"NumberOfResult":2,"Parts":[
              {"Manufacturer":"Texas Instruments","ManufacturerPartNumber":"LM358DR","MouserPartNumber":"595-LM358DR",
               "Description":"Operational Amplifiers","DataSheetUrl":"https://mouser.example/lm358.pdf",
               "ProductDetailUrl":"https://mouser.example/lm358","Availability":"12,400 In Stock","LifecycleStatus":"New Product",
               "ROHSStatus":"RoHS Compliant","MinimumOrderQuantity":"5","LeadTime":"42 Days","ImagePath":"https://mouser.example/lm358.jpg",
               "Category":"Op Amps","PriceBreaks":[
                 {"Quantity":1,"Price":"0,176 €","Currency":"EUR"},
                 {"Quantity":10,"Price":"See quote","Currency":"EUR"},
                 {"Quantity":100,"Price":"0,090 €","Currency":"EUR"}]},
              {"ManufacturerPartNumber":"LM358X","Availability":null,"DataSheetUrl":"","PriceBreaks":[]}
            ]}}""";

    private final List<HttpCall> calls = new ArrayList<>();

    private HttpReply reply(int status, String body) {
        return new HttpReply(calls.getLast().uri(), status, Map.of(), body.getBytes(StandardCharsets.UTF_8));
    }

    private MouserDriver answering(int status, String body) {
        return new MouserDriver(call -> {
            calls.add(call);
            return reply(status, body);
        });
    }

    @Test
    void sendsTheKeyEncodedInTheAddressNeverAsAHeader() {
        answering(200, ANSWER).search(ACCESS, "LM358", 500);
        HttpCall call = calls.getFirst();
        assertEquals("https://api.mouser.com/api/v1/search/keyword?apiKey=se%2Bcret+key", call.uri().toString());
        assertFalse(call.headers().toString().contains(KEY));
        String body = new String(call.body().orElseThrow().bytes(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"keyword\":\"LM358\""), body);
        assertTrue(body.contains("\"records\":50"), "asks no more than Mouser answers: " + body);
    }

    @Test
    void readsAnOfferWithItsLocalePricesAndDropsAHalfBreak() {
        List<SupplierOffer> offers = answering(200, ANSWER).search(ACCESS, "LM358", 10);
        SupplierOffer offer = offers.getFirst();
        assertEquals("mouser", offer.supplier());
        assertEquals("595-LM358DR", offer.supplierPartNumber());
        assertEquals("LM358DR", offer.partNumber());
        assertEquals(12400L, offer.stock());
        assertEquals(5L, offer.minimumOrder());
        assertEquals("EUR", offer.currency());
        assertEquals(List.of(new PriceBreak(1, new BigDecimal("0.176")), new PriceBreak(100, new BigDecimal("0.090"))), offer.priceBreaks());
        assertEquals("https://mouser.example/lm358", offer.page());
    }

    @Test
    void whatItDoesNotSayIsNullNeverZero() {
        SupplierOffer bare = answering(200, ANSWER).search(ACCESS, "LM358", 10).get(1);
        assertNull(bare.stock());
        assertNull(bare.datasheet());
        assertNull(bare.currency());
        assertEquals(List.of(), bare.priceBreaks());
    }

    @Test
    void nothingFoundIsAnEmptyListButAReportedErrorIsARefusal() {
        assertEquals(List.of(), answering(200, "{\"Errors\":[],\"SearchResults\":{\"Parts\":[]}}").search(ACCESS, "x", 10));
        SupplierFailure refused = assertThrows(SupplierFailure.class, () -> answering(200,
                "{\"Errors\":[{\"Message\":\"Invalid key " + KEY + "\"}],\"SearchResults\":null}").search(ACCESS, "x", 10));
        assertEquals(SupplierFailure.Reason.REFUSED, refused.reason());
        assertEquals("Mouser refused the search: Invalid key [redacted]", refused.getMessage());
    }

    @Test
    void aRefusalNamesItsReasonAndNeverRepeatsTheKey() {
        record Case(int status, SupplierFailure.Reason reason) {
        }
        for (Case refusal : List.of(new Case(401, SupplierFailure.Reason.CREDENTIALS_REFUSED),
                new Case(403, SupplierFailure.Reason.CREDENTIALS_REFUSED), new Case(429, SupplierFailure.Reason.RATE_LIMITED),
                new Case(404, SupplierFailure.Reason.REFUSED), new Case(302, SupplierFailure.Reason.REFUSED))) {
            SupplierFailure failure = assertThrows(SupplierFailure.class,
                    () -> answering(refusal.status(), "bad key " + KEY).search(ACCESS, "x", 10));
            assertEquals(refusal.reason(), failure.reason());
            assertFalse(failure.getMessage().contains(KEY), failure.getMessage());
            assertTrue(failure.getMessage().contains("HTTP " + refusal.status()), failure.getMessage());
        }
    }

    @Test
    void whatCannotBeReadOrReachedIsAFailureNotAnEmptyList() {
        assertEquals(SupplierFailure.Reason.UNREADABLE, assertThrows(SupplierFailure.class,
                () -> answering(200, "<html>").search(ACCESS, "x", 10)).reason());
        MouserDriver silent = new MouserDriver(call -> {
            throw new HttpCallException(HttpCallException.Failure.TIMEOUT, "No answer from api.mouser.com.");
        });
        assertEquals(SupplierFailure.Reason.UNREACHABLE, assertThrows(SupplierFailure.class, () -> silent.search(ACCESS, "x", 10)).reason());
        MouserDriver interrupted = new MouserDriver(call -> {
            throw new HttpCallException(HttpCallException.Failure.INTERRUPTED, "Interrupted.");
        });
        assertEquals(SupplierFailure.Reason.INTERRUPTED, assertThrows(SupplierFailure.class, () -> interrupted.search(ACCESS, "x", 10)).reason());
    }

    @Test
    void anAccessWithoutItsKindOfCredentialsIsNotConfigured() {
        MouserDriver driver = answering(200, ANSWER);
        for (SupplierCredentials wrong : List.of(new SupplierCredentials.ApiKey(" "), new SupplierCredentials.ClientCredentials("a", "b"))) {
            SupplierAccess access = new SupplierAccess("Mouser", null, wrong, null);
            assertEquals(SupplierFailure.Reason.NOT_CONFIGURED, assertThrows(SupplierFailure.class, () -> driver.search(access, "x", 10)).reason());
        }
        assertTrue(calls.isEmpty());
    }

    @Test
    void aCredentialNeverPrintsItself() {
        assertFalse(ACCESS.toString().contains(KEY), ACCESS.toString());
        assertFalse(new SupplierCredentials.ClientCredentials("id", "secret-value").toString().contains("secret-value"));
    }
}
