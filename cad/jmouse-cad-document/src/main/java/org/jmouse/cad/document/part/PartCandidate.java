package org.jmouse.cad.document.part;

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
 * @param slug         the catalogue object it already is; null when it is not ours yet
 * @param fetchable    whether {@link PartSearchProvider#fetch} makes it the reader's own
 */
public record PartCandidate(String provider, String identifier, Kind kind, String title, String partNumber,
                            String manufacturer, String description, String datasheet, Link link, String license,
                            String slug, boolean fetchable) {

    /** A page of the source's own: what it is called — {@code Octopart} — and where it is. */
    public record Link(String title, String address) {
    }

    public enum Kind {
        /** A part with pins and, maybe, a model. */
        COMPONENT,
        /** How a part is drawn on a sheet. */
        SYMBOL,
        /** How a part sits on a board. */
        FOOTPRINT,
        /** A circuit. */
        CIRCUIT,
        /** A manufacturer's part as a supplier lists it: a part number, not yet any drawing. */
        PART,
        /** Any other object of the catalogue. */
        OTHER
    }
}
