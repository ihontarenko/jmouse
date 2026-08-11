package org.jmouse.access;

/**
 * What shape a capability has — which is really the question <em>does it carry a number, and if so,
 * can that number be recounted?</em>
 *
 * <p>Three, and the third exists because of a real difference in what can be known. A standing count
 * is answered by counting what exists right now, so it cannot drift and needs no bookkeeping. A
 * consumed one cannot be recovered by counting anything — nothing existing tells you how much was
 * written last month — so it needs a counter, and a counter needs a window to be attributed to.
 *
 * <p>⚠️ The names are deliberately about <em>shape</em> rather than about what a product sells. A
 * library that had a {@code MODULE} kind would have learned one product's word for a feature area, and
 * the next product's layers are boards and sprints.
 */
public enum CapabilityKind {

    /**
     * Open or closed, with no number attached.
     *
     * <p>What a product's feature areas are: either this place has it or it does not. Innoventa's
     * modules are gates; Tessera's premium boards would be too.
     */
    GATE,

    /**
     * A standing count — how many may exist at once.
     *
     * <p>Seats, workspaces, projects. Resolved by counting what is there, so it has no period and
     * needs no counter.
     */
    LIMIT,

    /**
     * A consumed quantity, counted over a window.
     *
     * <p>Bytes written, automation runs. ⚠️ The only kind that requires the product to record
     * consumption — and that recording stays in the product, because it is a write on the hot path.
     */
    QUOTA;

    /** Whether this carries a number at all. */
    public boolean isMetered() {
        return this != GATE;
    }

    /** Whether a window is meaningful — true only where the quantity is consumed rather than counted. */
    public boolean hasPeriod() {
        return this == QUOTA;
    }
}
