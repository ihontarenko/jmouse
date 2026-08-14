package org.jmouse.ai.management;

/**
 * Where these controllers answer, and how much of that this module gets to decide.
 *
 * <p>⚠️ How many entries a listing returns is capped here rather than left to the caller. An endpoint
 * over a trail with a caller-supplied limit and no ceiling is a table scan anybody can ask for.
 */
public final class ManagementRoutes {

    /**
     * Where they answer when a product does not say.
     *
     * <p>Obviously a library's own corner of a URL space rather than an official-looking
     * {@code /admin/ai} that would collide with whatever the product already calls its administration
     * area. A product mounting these somewhere it means to sets the property; one that forgets gets a
     * working page at an address that reads as borrowed.
     */
    public static final String DEFAULT_PREFIX = "/jmouse-ai";

    /**
     * Where the controllers answer. Resolved by Spring, so a product only sets it in configuration.
     *
     * <p>Composed from {@link #DEFAULT_PREFIX} — still a compile-time constant, so it is usable in an
     * annotation, and there is one place the default is written rather than two that can disagree.
     */
    public static final String PREFIX = "${jmouse.ai.management.prefix:" + DEFAULT_PREFIX + "}";

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
