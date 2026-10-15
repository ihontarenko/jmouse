package org.jmouse.parallax.management;

import org.jmouse.core.management.ManagementEndpoints;

/**
 * 🛣️ Where Parallax's routes live.
 *
 * <p>⚠️ <strong>Stated once and read by everything.</strong> The address appears in every controller here, in a
 * product's security configuration, in its dev-server proxy and in whatever its interface is built against — and when
 * those drift the failure is not an error, it is a screen that renders as an empty catalogue.</p>
 *
 * <h2>⚠️ Under a prefix of the library's own, and NOT under a product's {@code /api}</h2>
 *
 * <p>These routes answered at a product's {@code /api/v1} while they were the product's. A library that serves under
 * a product's prefix collides with the product's own routes the day both name {@code /catalogue}, and the context
 * refuses to start on an ambiguous mapping — the reason {@code jmouse-storage-management} moved to its own prefix.
 * An address nobody would invent by accident is a route a reader goes and looks up.</p>
 *
 * <h2>⚠️ How a constant can be configurable at all</h2>
 *
 * <p>{@link #PREFIX} is a Spring placeholder, and the concatenation is still a compile-time constant — which is what
 * an annotation attribute requires. {@code RequestMappingHandlerMapping} resolves {@code ${…}} in a mapping when it
 * registers the handler, so the value arrives from configuration and the annotation never notices.</p>
 *
 * <p>⚠️ <strong>The cost, said out loud.</strong> A placeholder is not a path: a product's security configuration, its
 * dev-server proxy and its interface cannot read this constant and must be told the same value again. Nothing checks
 * that they agree.</p>
 *
 * <p>⚠️ <strong>Authorization is NOT one of those places</strong>: {@link ParallaxAccessRules} gates by controller
 * class and method name, never by path, so a product that re-prefixes these routes cannot un-gate them by moving
 * them.</p>
 *
 * <h2>Owners in the address</h2>
 *
 * <p>Whatever belongs to somebody is addressed by its owner, as the host names it: {@code /owners/{type}/{id}/…} for
 * what the owner holds and {@code /realms/{type}/{id}/…} for what is merely seen there. Parallax keeps no table of
 * owners; the type is the host's word ({@code WORKSPACE}, {@code PROJECT}…) and the rules ask the host where it
 * sits.</p>
 */
public final class ParallaxManagementRoutes {

    /**
     * This module's own segment under the shared root — the library, then its API.
     *
     * <p>{@code /api} is part of the segment so that the live socket can sit beside it at {@code /parallax/live}
     * without either reading as a resource of the other.</p>
     */
    public static final String SEGMENT = "/parallax/api";

    /**
     * Where the controllers answer. Resolved by Spring, so a product only sets it in configuration.
     *
     * <p>Two levels, and a product may use either — see {@link ManagementEndpoints}: {@code jmouse.management.prefix}
     * moves every library at once, {@code jmouse.parallax.management.prefix} moves this one alone. The default
     * composes to {@code /jmouse/parallax/api}.</p>
     */
    public static final String PREFIX =
            "${jmouse.parallax.management.prefix:" + ManagementEndpoints.ROOT + SEGMENT + "}";

    /** The path variable an owner's type travels in. */
    public static final String OWNER_TYPE = "ownerType";

    /** The path variable an owner's identifier travels in. */
    public static final String OWNER_ID = "ownerId";

    /** The path variable a realm's type travels in. */
    public static final String REALM_TYPE = "realmType";

    /** The path variable a realm's identifier travels in. */
    public static final String REALM_ID = "realmId";

    /** What one owner holds: its objects, its jobs, its scenes and comparisons. Relative to {@link #PREFIX}. */
    public static final String OWNER = "/owners/{" + OWNER_TYPE + "}/{" + OWNER_ID + "}";

    /** What is seen in one realm without belonging to it. Relative to {@link #PREFIX}. */
    public static final String REALM = "/realms/{" + REALM_TYPE + "}/{" + REALM_ID + "}";

    /** The machinery's administration: every job, the cache, the warm-up, the evaluation runs. Relative to {@link #PREFIX}. */
    public static final String ADMINISTRATION = "/administration";

    private ParallaxManagementRoutes() {
    }
}
