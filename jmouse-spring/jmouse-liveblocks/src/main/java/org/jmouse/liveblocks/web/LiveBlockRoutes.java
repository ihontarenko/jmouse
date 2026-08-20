package org.jmouse.liveblocks.web;

/**
 * Where the contract lives on the wire.
 *
 * <p>⚠️ <strong>A constant rather than a property, deliberately.</strong> The whole value of this module
 * is that a consumer needs one address and no per-product configuration — it holds a namespace and an
 * <em>origin</em>, and appends this. A product that could move the path would be a product whose
 * registration needs a second field, and the map that says "issue lives at :8100" would stop being the
 * whole answer.
 */
public final class LiveBlockRoutes {

    /** The prefix every route in this module hangs off. */
    public static final String PREFIX = "/api/blocks";

    /** The one route a consumer calls, whole — useful to products writing security matchers. */
    public static final String RESOLVE = PREFIX + "/resolve";

    private LiveBlockRoutes() {
    }

}
