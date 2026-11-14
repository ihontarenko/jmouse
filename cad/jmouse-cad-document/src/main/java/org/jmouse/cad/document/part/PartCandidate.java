package org.jmouse.cad.document.part;

import java.math.BigDecimal;
import java.util.List;

/**
 * A part a provider found.
 *
 * @param provider     the provider's {@link PartSearchProvider#id()}
 * @param identifier   how the provider names it, to be {@linkplain PartSearchProvider#fetch fetched} by:
 *                     {@code symbol:Device:R}, a catalogue slug, a supplier's part id
 * @param kind         what it is
 * @param title        its name to a reader
 * @param partNumber   the manufacturer's part number, when it is a real part; null otherwise
 * @param manufacturer who makes it; null when nobody in particular does
 * @param description  what it says of itself
 * @param datasheet    where its datasheet is; null when none is known
 * @param link         the source's own page for it, named as the source names it — a supplier's terms may require it
 *                     shown —; null when none
 * @param license      the license its documents come under once fetched; null when they are ours
 * @param fetchable    whether {@link PartSearchProvider#fetch} makes it the reader's own
 * @param supply       what a distributor has of it right now; null where the source sells nothing
 */
public record PartCandidate(String provider, String identifier, Kind kind, String title, String partNumber,
                            String manufacturer, String description, String datasheet, Link link, String license,
                            boolean fetchable, Supply supply) {

    /** A page of the source's own: what it is called — {@code Octopart} — and where it is. */
    public record Link(String title, String address) {
    }

    /**
     * What a distributor has of a part right now. Whatever it did not say is null, never zero: a stock of zero is a
     * statement.
     *
     * @param orderNumber  its own number for the part, the one it is ordered by
     * @param status       its life cycle, in the distributor's words
     * @param stock        how many it holds
     * @param minimumOrder the fewest it sells
     * @param leadTime     how long it takes to get more, in the distributor's words
     * @param currency     what {@code prices} are in
     * @param prices       what one costs, by how many are bought
     */
    public record Supply(String orderNumber, String status, Long stock, Long minimumOrder, String leadTime, String currency,
                         List<Price> prices) {

        public Supply {
            prices = prices == null ? List.of() : List.copyOf(prices);
        }
    }

    /** What one costs when at least {@code quantity} are bought. */
    public record Price(long quantity, BigDecimal unitPrice) {
    }

    public enum Kind {
        /** A part with pins and, maybe, a model. */
        COMPONENT,
        /** How a part is drawn on a sheet. */
        SYMBOL,
        /** How a part sits on a pcb. */
        FOOTPRINT,
        /** A manufacturer's part as a supplier lists it: a part number, not yet any drawing. */
        PART
    }
}
