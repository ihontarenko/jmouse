package org.jmouse.access.spi;

import org.jmouse.access.ScopeReference;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Which wider places a narrower one sits inside — the containment the engine deliberately does not
 * know.
 *
 * <p>A grant at {@code ORGANIZATION:{id}} has to cover the rows in that account's workspaces, or a
 * scope that reads as a floor behaves like a leaf. Somebody has to know that a workspace belongs to
 * an account. It is not the feature — a form knows it is in a workspace and nothing else — and it is
 * emphatically not {@code platform.access}, whose whole point is that it names no place.
 *
 * <p>So it is a seam, and the direction is the established one (ADR-0010): the engine declares it,
 * and whoever owns the containment implements it. One product registers workspace → organisation. A
 * product with departments registers department → division → tenant, and nothing above this
 * interface changes; a product with one flat level registers nothing at all and every implementation
 * of this returns empty.
 *
 * <p>Two directions, because the two questions have different shapes and a check cannot be a filter:
 *
 * <ul>
 *   <li>{@link #containing} — <em>a check</em> has a row, so it asks which wider places that row's
 *       place is in, and adds them to the covering chain.
 *   <li>{@link #within} — <em>a filter</em> has no row yet, so it asks the opposite: which narrower
 *       places these wider ones contain, and matches the query against those. Without it an
 *       organisation administrator would hold a permission over every workspace of their account and
 *       see none of their rows in any listing.
 * </ul>
 *
 * <p>Deliberately uncached here. One indexed lookup on an identifier is cheap, and a map on a
 * singleton would be a cache with no expiry over a fact that a workspace transfer changes — the sort
 * of staleness that shows up as one customer seeing another's rows. What memoisation this needs it
 * gets from {@code AccessContext}, which is request-scoped and holds the resolved answer rather than
 * this input to it.
 */
public interface ScopeHierarchy {

    /** Every wider place this one sits inside, widest first. Empty where it sits inside none. */
    List<ScopeReference> containing(ScopeReference place);

    /** Every narrower place these ones contain — what a listing needs, and in one query. */
    Set<ScopeReference> within(Collection<ScopeReference> places);

    /**
     * Containment for a product that has none — <strong>nothing is inside anything.</strong>
     *
     * <p>The engine's floor case, and it has to be free. An application whose authorization is
     * <em>"these people hold these roles"</em>, with no places at all, must not have to write a class
     * with two empty methods to say so; every axis above works unchanged, because a covering chain of
     * one place and a filter that widens to nothing are exactly what a flat installation means.
     *
     * <p>The mirror of {@code EntitlementStore.empty()}, and for the same reason: what a product has
     * not adopted must cost it nothing.
     */
    static ScopeHierarchy flat() {
        return new ScopeHierarchy() {

            @Override
            public List<ScopeReference> containing(ScopeReference place) {
                return List.of();
            }

            @Override
            public Set<ScopeReference> within(Collection<ScopeReference> places) {
                return Set.of();
            }
        };
    }
}
