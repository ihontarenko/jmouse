package org.jmouse.taxonomy.management;

import org.jmouse.core.management.ManagementEndpoints;

/**
 * 🛣️ Where the taxonomy routes live: under the library's own prefix, never a product's {@code /api} — the reasoning
 * that moved the storage and Parallax routes to theirs.
 *
 * <p>{@link #PREFIX} is a Spring placeholder, resolved when the handler is registered: {@code jmouse.management.prefix}
 * moves every library at once, {@code jmouse.taxonomy.management.prefix} this one alone. The default composes to
 * {@code /jmouse/taxonomy/api}. ⚠️ A product's security chain and its interface are told the same value again; nothing
 * checks that they agree. Authorization does not depend on it: {@link TaxonomyAccessRules} gates by class and method.</p>
 */
public final class TaxonomyRoutes {

    /** This module's own segment under the shared root. */
    public static final String SEGMENT = "/taxonomy/api";

    /** Where the controller answers. */
    public static final String PREFIX = "${jmouse.taxonomy.management.prefix:" + ManagementEndpoints.ROOT + SEGMENT + "}";

    private TaxonomyRoutes() {
    }
}
