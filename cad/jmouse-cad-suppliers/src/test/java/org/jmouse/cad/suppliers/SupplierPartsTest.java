package org.jmouse.cad.suppliers;

import org.jmouse.cad.document.part.PartCandidate;
import org.jmouse.cad.document.part.ProviderState;
import org.jmouse.suppliers.PriceBreak;
import org.jmouse.suppliers.SupplierAccess;
import org.jmouse.suppliers.SupplierCredentials;
import org.jmouse.suppliers.SupplierDriver;
import org.jmouse.suppliers.SupplierFailure;
import org.jmouse.suppliers.SupplierOffer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierPartsTest {

    private static final SupplierOffer OFFER = new SupplierOffer("mouser", "595-LM358DR", "LM358DR", "Texas Instruments", "Op amp",
            null, "Amplifiers", "Active", null, 12400L, 5L, "42 Days", "EUR", List.of(new PriceBreak(1, new BigDecimal("0.176"))),
            "https://mouser.example/lm358", "https://mouser.example/lm358.pdf", null);

    private static final SupplierOffer NAMELESS = new SupplierOffer("mouser", null, null, null, "Nothing to look up", null, null,
            null, null, null, null, null, null, List.of(), null, null, null);

    private final List<Integer> asked = new ArrayList<>();
    private SupplierFailure failure;

    private final SupplierDriver driver = new SupplierDriver() {
        @Override
        public String kind() {
            return "mouser";
        }

        @Override
        public String name() {
            return "Mouser";
        }

        @Override
        public URI defaultAddress() {
            return URI.create("https://mouser.example");
        }

        @Override
        public int mostPerSearch() {
            return 50;
        }

        @Override
        public List<SupplierOffer> search(SupplierAccess access, String query, int limit) {
            asked.add(limit);
            if (failure != null) {
                throw failure;
            }
            return List.of(OFFER, NAMELESS);
        }
    };

    private SupplierParts parts(String key, int limit) {
        return new SupplierParts(driver, new SupplierAccess("Mouser", null, new SupplierCredentials.ApiKey(key), null), () -> limit,
                "MOUSER_API_KEY is not set.");
    }

    @Test
    void withoutItsKeyItIsListedAsNotConfiguredAndAsksNobody() {
        SupplierParts parts = parts("", 10);
        assertEquals(ProviderState.Status.NOT_CONFIGURED, parts.state().status());
        assertEquals("MOUSER_API_KEY is not set.", parts.state().detail());
        assertEquals(List.of(), parts.search("LM358", 10));
        assertTrue(asked.isEmpty());
    }

    @Test
    void anOfferBecomesACandidateShownNotKept() {
        List<PartCandidate> found = parts("key", 10).search(" LM358 ", 20);
        assertEquals(1, found.size(), "an offer naming no part has nothing to look up");
        PartCandidate candidate = found.getFirst();
        assertEquals("mouser", candidate.provider());
        assertEquals("595-LM358DR", candidate.identifier());
        assertEquals("LM358DR", candidate.title());
        assertEquals(PartCandidate.Kind.PART, candidate.kind());
        assertFalse(candidate.fetchable());
        assertNull(candidate.slug());
        assertEquals(new PartCandidate.Link("Mouser", "https://mouser.example/lm358"), candidate.link());
        assertEquals(12400L, candidate.supply().stock());
        assertEquals(List.of(new PartCandidate.Price(1, new BigDecimal("0.176"))), candidate.supply().prices());
    }

    @Test
    void asksTheFewestOfWhatTheReaderTheSettingAndTheDistributorAllow() {
        parts("key", 10).search("a", 20);
        parts("key", 80).search("a", 200);
        parts("key", 80).search("a", 3);
        assertEquals(List.of(10, 50, 3), asked);
    }

    @Test
    void aFailureStaysAFailureAndTheNextSearchStillAsks() {
        SupplierParts parts = parts("key", 10);
        failure = new SupplierFailure(SupplierFailure.Reason.RATE_LIMITED, "Mouser answered HTTP 429.");
        assertEquals("Mouser answered HTTP 429.", assertThrows(IllegalStateException.class, () -> parts.search("a", 10)).getMessage());
        assertTrue(parts.state().ready(), "one failure must not switch the place off: search skips a place that is not ready");
        failure = null;
        assertEquals(1, parts.search("a", 10).size());
    }
}
