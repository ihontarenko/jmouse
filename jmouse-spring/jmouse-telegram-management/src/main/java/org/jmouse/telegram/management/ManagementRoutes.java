package org.jmouse.telegram.management;

import org.jmouse.core.management.ManagementEndpoints;

/**
 * Where the Telegram administration API is served.
 *
 * <p>Default {@code /jmouse/telegram/api}. Two levels of override, as every jMouse management module has:
 *
 * <table>
 *   <tr><th>Property</th><th>Moves</th></tr>
 *   <tr><td>{@code jmouse.management.prefix}</td><td>every library management surface at once</td></tr>
 *   <tr><td>{@code jmouse.telegram.management.prefix}</td><td>this one alone</td></tr>
 * </table>
 *
 * <p>⚠️ <strong>The placeholder is not a path.</strong> A product's security rules, its dev-server proxy
 * and any client must be told the same value again; when they drift, every call 404s and the surface
 * looks like an empty installation. {@link #DEFAULT_PREFIX} is for anything with no environment to
 * resolve against.
 */
public final class ManagementRoutes {

    public static final String SEGMENT = "/telegram/api";

    /** What the prefix resolves to when nothing overrides it. */
    public static final String DEFAULT_PREFIX = ManagementEndpoints.DEFAULT_ROOT + SEGMENT;

    /**
     * The placeholder controllers are mapped under.
     *
     * <p>⚠️ The property name is written out rather than built with {@code ManagementEndpoints.propertyFor}:
     * an annotation takes only a constant, and a method call is not one.
     */
    public static final String PREFIX = "${jmouse.telegram.management.prefix:" + ManagementEndpoints.ROOT + SEGMENT + "}";

    public static final int DEFAULT_LIMIT = 50;

    public static final int MAXIMUM_LIMIT = 500;

    private ManagementRoutes() {
    }

    /** A requested page size, defaulted and capped — a listing is for reading, not for export. */
    public static int boundedLimit(int requested) {
        if (requested <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(requested, MAXIMUM_LIMIT);
    }
}
