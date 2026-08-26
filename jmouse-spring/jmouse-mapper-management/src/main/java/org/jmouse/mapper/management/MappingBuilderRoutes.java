package org.jmouse.mapper.management;

import org.jmouse.core.management.ManagementEndpoints;

/**
 * Where these controllers answer.
 *
 * <p>The same two-level shape every other library management surface uses:
 * {@code jmouse.management.prefix} moves all of them at once,
 * {@code jmouse.mapper.management.prefix} moves this one alone. The default composes to
 * {@code /jmouse/mapper/api}.</p>
 *
 * <p>⚠️ A library's own corner of a URL space rather than an official-looking {@code /admin/mapping}
 * that would collide with whatever the product already calls its administration area. A product
 * mounting these somewhere it means to sets the property; one that forgets gets a working screen at an
 * address that reads as borrowed.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class MappingBuilderRoutes {

    /** This module's own segment under the shared root. */
    public static final String SEGMENT = "/mapper/api";

    /**
     * The address these answer at when nothing overrides it — {@code /jmouse/mapper/api}.
     *
     * <p>⚠️ Named because {@link #PREFIX} is a <strong>placeholder</strong>, and anything with no Spring
     * environment to resolve it against would otherwise print {@code ${…}} — or, worse, write the
     * default out a second time by hand. A second copy of an address is an address that drifts, and this
     * one drifts into a screen that renders as an empty installation rather than as an error.</p>
     */
    public static final String DEFAULT_PREFIX = ManagementEndpoints.ROOT + SEGMENT;

    public static final String PREFIX = "${jmouse.mapper.management.prefix:" + DEFAULT_PREFIX + "}";

    private MappingBuilderRoutes() {
    }
}
