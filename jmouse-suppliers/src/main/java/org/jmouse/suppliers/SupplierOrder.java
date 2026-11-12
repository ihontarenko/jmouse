package org.jmouse.suppliers;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What buying a number of a part from one offer comes to: how many it sells then, at what price, and whether it holds them.
 *
 * @param offer        the offer
 * @param needed       how many were wanted
 * @param quantity     how many have to be bought: what was wanted, or the fewest it sells when that is more
 * @param unitPrice    what each costs at that quantity; null when the offer states no price
 * @param availability whether it holds them
 */
public record SupplierOrder(SupplierOffer offer, long needed, long quantity, BigDecimal unitPrice, Availability availability) {

    /** Whether an offer holds what is bought, the better first. */
    public enum Availability {
        /** It holds at least that many. */
        IN_STOCK,
        /** It said how many it holds, and that is fewer. */
        SHORT,
        /** It did not say. */
        UNSTATED
    }

    private static final Comparator<SupplierOrder> BETTER_FIRST = Comparator.comparing(SupplierOrder::availability)
            .thenComparing(SupplierOrder::total, Comparator.nullsLast(Comparator.naturalOrder()));

    public SupplierOrder {
        Objects.requireNonNull(offer, "offer");
        Objects.requireNonNull(availability, "availability");
    }

    /** @param needed how many are wanted, at least one */
    public static SupplierOrder of(SupplierOffer offer, long needed) {
        if (needed < 1) {
            throw new IllegalArgumentException("An order is of one part at least: " + needed);
        }
        List<PriceBreak> breaks = offer.priceBreaks().stream().sorted(Comparator.comparingLong(PriceBreak::quantity)).toList();
        long quantity = needed;
        if (offer.minimumOrder() != null) {
            quantity = Math.max(quantity, offer.minimumOrder());
        }
        if (!breaks.isEmpty()) {
            quantity = Math.max(quantity, breaks.getFirst().quantity());
        }
        BigDecimal unitPrice = null;
        for (PriceBreak priceBreak : breaks) {
            if (priceBreak.quantity() <= quantity) {
                unitPrice = priceBreak.unitPrice();
            }
        }
        return new SupplierOrder(offer, needed, quantity, unitPrice, availabilityOf(offer.stock(), quantity));
    }

    /**
     * The order to place for exactly this part: the offers naming it — a search answers with similar parts as well, and
     * buying one of those is a wrong board —, those holding enough first, the cheapest of them.
     *
     * @return empty when no offer names this part
     */
    public static Optional<SupplierOrder> best(List<SupplierOffer> offers, String partNumber, long needed) {
        return offers.stream().filter(offer -> partNumber.equalsIgnoreCase(offer.partNumber()))
                .map(offer -> of(offer, needed)).min(BETTER_FIRST);
    }

    /** What the whole order costs; null when the offer states no price. */
    public BigDecimal total() {
        return unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private static Availability availabilityOf(Long stock, long quantity) {
        if (stock == null) {
            return Availability.UNSTATED;
        }
        return stock >= quantity ? Availability.IN_STOCK : Availability.SHORT;
    }
}
