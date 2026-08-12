package org.jmouse.access;

/**
 * The words for the refusals the <em>engine itself</em> raises, as opposed to the ones its axes do.
 *
 * <p>Almost every refusal in the product comes out of an axis, and an axis is a bean the product
 * wrote — so it already has its own vocabulary and needs nothing from here. The engine is a
 * dispatcher and raises exactly one refusal of its own: the row a call names does not resolve.
 *
 * <p>That one still has to be said in words, and words are the product's. Rather than let the
 * dispatcher reach for a constant — which is how {@code AccessEngine} ended up naming
 * {@code AccessReason} at all — it is handed the reason the same way it is handed its scopes and its
 * axes. A record of one field looks like ceremony until the alternative is the last import standing
 * between the engine and a jar.
 *
 * <p>A second one arrived with published actions, and it is the same kind of thing: a refusal raised
 * before any axis runs, because the call could not be described truthfully enough to decide about.
 *
 * @param noSuchRow       what a call naming a row nobody can resolve is refused with. It must read as
 *                        <em>"there is no such row"</em> rather than as <em>"you may not"</em>: for a
 *                        direct-by-identifier read of somebody else's row, saying "forbidden"
 *                        confirms the row exists to somebody who had no way of knowing
 * @param undeclaredValue what a call is refused with when the route <em>promised</em> to publish a
 *                        value and the caller supplied nothing for it. ⚠️ It must read as <em>"this
 *                        call cannot be decided about"</em>: a rule may have been written against
 *                        that value, and proceeding would silently mean the rule does not apply —
 *                        which for a conditional deny is an open door
 */
public record EngineRefusals(RefusalReason noSuchRow, RefusalReason undeclaredValue) {

    public EngineRefusals {
        if (noSuchRow == null) {
            throw new IllegalArgumentException(
                    "The engine needs a reason to refuse an unresolvable row with. Without one it "
                    + "would have to let the call through as an unscoped request, and an unscoped "
                    + "request passes every axis that is about a place.");
        }
        if (undeclaredValue == null) {
            undeclaredValue = noSuchRow;
        }
    }

    /**
     * ⚠️ The one-reason wiring, for a product that publishes no actions.
     *
     * <p>It reads a broken value promise back as <em>"no such row"</em>, which is the wrong sentence
     * and deliberately so: a product that never writes {@code @AccessContext} can never reach it, and
     * a product that does should register a reason of its own rather than inherit one that will
     * confuse the first person to meet it.
     */
    public EngineRefusals(RefusalReason noSuchRow) {
        this(noSuchRow, null);
    }
}
