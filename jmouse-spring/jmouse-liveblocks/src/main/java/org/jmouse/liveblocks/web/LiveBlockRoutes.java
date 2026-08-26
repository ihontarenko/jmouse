package org.jmouse.liveblocks.web;

/**
 * Where the contract lives on the wire.
 *
 * <h2>⚠️ Under {@code /jmouse}, like every other library surface</h2>
 *
 * <p>It used to be {@code /api/blocks} — inside the product's own URL space, which is the one place a
 * library must not publish into: {@code ManagementEndpoints} tells the story of two modules that chose
 * {@code /api/files} and {@code /api/agents}, collided with the product's own routes, and could not be
 * switched on at all. This module got away with it only because no product happened to want that path.
 * Ivan, 2026-08-25: <em>«нехай всі бібліотечні будуть уніфіковані типу /jmouse/…»</em>.</p>
 *
 * <h2>⚠️ Still a CONSTANT, and that is not an oversight</h2>
 *
 * <p>Every other library surface takes a property so a product can move it. This one must not, and the
 * reason is the contract rather than the routing: a consumer holds a namespace and an <strong>origin</strong>
 * and appends this path — <em>"an issue lives at :8100"</em> is meant to be the whole registration. A
 * product that could move its blocks path would need a second field in every consumer that reads it, and
 * the day one product moved and one consumer did not, a live block would render as a miss rather than as
 * an error.</p>
 *
 * <p>⚠️ <strong>And the address is spelled out here rather than composed from {@code ManagementEndpoints}.</strong>
 * This module deliberately depends on nothing — that is what lets a consumer take it as a wire contract —
 * so it carries the literal and matches the shape by agreement: {@code /jmouse/<namespace>/api}.</p>
 *
 * <p>Moving it is a change to this constant, in one release, for every product at once — which is
 * exactly what a wire contract should cost.</p>
 */
public final class LiveBlockRoutes {

    /** The prefix every route in this module hangs off — the same shape as every library surface. */
    public static final String PREFIX = "/jmouse/blocks/api";

    /** The route a consumer calls to render a document — useful to products writing security matchers. */
    public static final String RESOLVE = PREFIX + "/resolve";

    /**
     * The route a picker calls to ask what there is to refer to.
     *
     * <p>⚠️ Named here beside {@link #RESOLVE} because a product writing a security matcher for one and
     * not the other has published a search it did not mean to.
     */
    public static final String SUGGEST = PREFIX + "/suggest";

    private LiveBlockRoutes() {
    }

}
