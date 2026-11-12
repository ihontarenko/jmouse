package org.jmouse.suppliers;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupplierOrderTest {

    private static SupplierOffer offer(String number, String partNumber, Long stock, Long minimum, PriceBreak... breaks) {
        return new SupplierOffer("mouser", number, partNumber, "Texas Instruments", null, null, null, "Active", null, stock, minimum,
                null, "USD", List.of(breaks), null, null, null);
    }

    private static PriceBreak price(long quantity, String unitPrice) {
        return new PriceBreak(quantity, new BigDecimal(unitPrice));
    }

    @Test
    void thePriceIsTheBreakTheQuantityReaches() {
        SupplierOrder order = SupplierOrder.of(offer("a", "TL072CDR", 100L, null, price(1, "0.50"), price(10, "0.40"), price(100, "0.30")), 25);
        assertEquals(25, order.quantity());
        assertEquals(new BigDecimal("0.40"), order.unitPrice());
        assertEquals(new BigDecimal("10.00"), order.total());
        assertEquals(SupplierOrder.Availability.IN_STOCK, order.availability());
    }

    @Test
    void breaksListedInAnyOrderReadTheSame() {
        SupplierOrder order = SupplierOrder.of(offer("a", "TL072CDR", 100L, null, price(100, "0.30"), price(1, "0.50")), 2);
        assertEquals(new BigDecimal("0.50"), order.unitPrice());
    }

    @Test
    void itBuysTheFewestItSellsWhenThatIsMoreThanWanted() {
        SupplierOrder atMinimum = SupplierOrder.of(offer("a", "TL072CDR", 100L, 5L, price(1, "0.50")), 2);
        assertEquals(2, atMinimum.needed());
        assertEquals(5, atMinimum.quantity());
        assertEquals(new BigDecimal("2.50"), atMinimum.total());
        SupplierOrder atFirstBreak = SupplierOrder.of(offer("a", "TL072CDR", 100L, null, price(10, "0.40")), 2);
        assertEquals(10, atFirstBreak.quantity());
        assertEquals(new BigDecimal("0.40"), atFirstBreak.unitPrice());
    }

    @Test
    void whatItHoldsIsTheQuantityBoughtNotTheQuantityWanted() {
        assertEquals(SupplierOrder.Availability.SHORT, SupplierOrder.of(offer("a", "TL072CDR", 4L, 5L), 2).availability());
        assertEquals(SupplierOrder.Availability.SHORT, SupplierOrder.of(offer("a", "TL072CDR", 0L, null), 1).availability());
        assertEquals(SupplierOrder.Availability.UNSTATED, SupplierOrder.of(offer("a", "TL072CDR", null, null), 1).availability());
    }

    @Test
    void anOfferWithNoPriceHasNoTotalNotAFreeOne() {
        SupplierOrder order = SupplierOrder.of(offer("a", "TL072CDR", 4L, null), 2);
        assertNull(order.unitPrice());
        assertNull(order.total());
    }

    @Test
    void nothingIsAnOrder() {
        assertThrows(IllegalArgumentException.class, () -> SupplierOrder.of(offer("a", "TL072CDR", 4L, null), 0));
    }

    @Test
    void theBestIsOfExactlyThatPartHoldingEnoughThenTheCheapest() {
        List<SupplierOffer> offers = List.of(
                offer("similar", "TL072CP", 100L, null, price(1, "0.01")),
                offer("short", "TL072CDR", 1L, null, price(1, "0.10")),
                offer("dear", "TL072CDR", 100L, null, price(1, "0.90")),
                offer("cheap", "tl072cdr", 100L, null, price(1, "0.50")),
                offer("unpriced", "TL072CDR", 100L, null));
        assertEquals("cheap", SupplierOrder.best(offers, "TL072CDR", 2).orElseThrow().offer().supplierPartNumber());
    }

    @Test
    void noOfferNamingThePartIsNoOrder() {
        assertTrue(SupplierOrder.best(List.of(offer("similar", "TL072CP", 100L, null)), "TL072CDR", 2).isEmpty());
        assertTrue(SupplierOrder.best(List.of(offer("nameless", null, 100L, null)), "TL072CDR", 2).isEmpty());
    }
}
