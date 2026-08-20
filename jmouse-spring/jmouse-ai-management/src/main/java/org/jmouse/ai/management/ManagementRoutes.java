package org.jmouse.ai.management;

import org.jmouse.core.management.ManagementEndpoints;

/**
 * Where these controllers answer, and how much of that this module gets to decide.
 *
 * <p>⚠️ How many entries a listing returns is capped here rather than left to the caller. An endpoint
 * over a trail with a caller-supplied limit and no ceiling is a table scan anybody can ask for.
 */
public final class ManagementRoutes {

    /**
     * This module's own segment under the shared root.
     *
     * <p>Obviously a library's own corner of a URL space rather than an official-looking
     * {@code /admin/ai} that would collide with whatever the product already calls its administration
     * area. A product mounting these somewhere it means to sets the property; one that forgets gets a
     * working page at an address that reads as borrowed.
     */
    public static final String SEGMENT = "/ai/api";

    /**
     * Where the controllers answer. Resolved by Spring, so a product only sets it in configuration.
     *
     * <p>Two levels, and a product may use either — see {@link ManagementEndpoints}:
     * {@code jmouse.management.prefix} moves every library management surface at once,
     * {@code jmouse.ai.management.prefix} moves this one alone. The default composes to
     * {@code /jmouse/ai/api}.
     *
     * <p>⚠️ <strong>It was {@code /jmouse-ai}, and the shape rather than the string is what changed.</strong>
     * Every product that had configured this already set the property — Kiwi to {@code /jmai/api} — so
     * nothing that was working moves. What a product gets now is the choice of moving ALL of them in one
     * line instead of remembering each library's own name.
     */
    public static final String PREFIX =
            "${jmouse.ai.management.prefix:" + ManagementEndpoints.ROOT + SEGMENT + "}";

    /** What a listing returns when nothing is asked for. */
    public static final int DEFAULT_LIMIT = 50;

    /** The most any listing returns, however large a number is asked for. */
    public static final int MAXIMUM_LIMIT = 500;

    private ManagementRoutes() {
    }

    /** A caller's requested limit, brought inside the bounds without refusing them. */
    public static int boundedLimit(int requested) {
        if (requested <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(requested, MAXIMUM_LIMIT);
    }
}
