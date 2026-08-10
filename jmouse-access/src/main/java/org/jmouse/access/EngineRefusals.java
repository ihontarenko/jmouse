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
 * @param noSuchRow what a call naming a row nobody can resolve is refused with. It must read as
 *                  <em>"there is no such row"</em> rather than as <em>"you may not"</em>: for a
 *                  direct-by-identifier read of somebody else's row, saying "forbidden" confirms the
 *                  row exists to somebody who had no way of knowing
 */
public record EngineRefusals(RefusalReason noSuchRow) {

    public EngineRefusals {
        if (noSuchRow == null) {
            throw new IllegalArgumentException(
                    "The engine needs a reason to refuse an unresolvable row with. Without one it "
                    + "would have to let the call through as an unscoped request, and an unscoped "
                    + "request passes every axis that is about a place.");
        }
    }
}
