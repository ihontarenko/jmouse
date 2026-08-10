package org.jmouse.access;

/**
 * Why a request was refused — the model's side of what {@code AccessReason} spells out for this
 * product.
 *
 * <p>The third and last of the vocabularies the engine used to contain outright, after
 * {@link ScopeKind} and {@link AxisKind}, and the one that gives the game away fastest:
 * {@code AccessReason} lists {@code UNAUTHENTICATED} and {@code NO_PERMISSION}, which are what "no"
 * means anywhere, right beside {@code ENTITLEMENT}, {@code ALLOWANCE} and {@code MODULE_OFF}, which
 * belong to one product's plans and per-place switches. A product with neither cannot use an
 * engine whose refusal type insists both exist.
 *
 * <p><strong>The HTTP status is deliberately not here.</strong> A refusal is a fact about
 * authorization; a status code is a fact about a transport, and an engine that could be called from a
 * queue consumer or a scheduled job has no business holding one. A product's mapping is typically
 * {@code AccessReason.status()}, read by {@code GlobalExceptionHandler} — one table, on the product's
 * side of the line.
 *
 * <p>Implement it with an enum, for the reasons the other two are enums: a fixed set of refusals is
 * exactly what a product wants, and the ability to {@code switch} over them is worth keeping.
 */
public interface RefusalReason {

    /** The constant's own name — what a metric counts and an audit line records. */
    String name();

    /**
     * The axis this refusal comes out of.
     *
     * <p>Which is what makes a verdict readable: the engine stops at the first refusal, so the axis a
     * reason names is the <em>outermost</em> thing standing in the way, and telling somebody to ask
     * for a permission when the plan is what blocks them sends them to somebody who cannot help.
     */
    AxisKind axis();

    /**
     * The heading a person reads, distinct per reason.
     *
     * <p>Distinct on purpose: "Access denied" over every one of these is how a reader learns that the
     * product refuses without knowing why. The detail carries the axis's own words; this is what a
     * toast can show without them.
     */
    String title();

    /** The machine-readable value carried beside the prose, so a client branches on a value. */
    default String wireName() {
        return name().toLowerCase().replace('_', '-');
    }
}
