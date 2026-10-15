package org.jmouse.vault.management;

import org.jmouse.core.management.ManagementEndpoints;

/**
 * 🧭 Where the vault's management API is mounted: {@code jmouse.vault.management.prefix}, by default
 * {@code /jmouse/vault/api}.
 *
 * <p>⚠️ A secret's name holds slashes, so it travels as the {@code name} query parameter rather than as
 * a path segment — a servlet container decodes {@code %2F} inconsistently, and a path that means two
 * things is not one to authorize.
 */
public final class ManagementRoutes {

    public static final String SEGMENT = "/vault/api";

    public static final String DEFAULT_PREFIX = ManagementEndpoints.DEFAULT_ROOT + SEGMENT;

    public static final String PREFIX = "${jmouse.vault.management.prefix:" + ManagementEndpoints.ROOT + SEGMENT + "}";

    public static final int DEFAULT_LIMIT = 100;

    public static final int MAXIMUM_LIMIT = 500;

    private ManagementRoutes() {
    }

    public static int boundedLimit(int requested) {
        if (requested <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(requested, MAXIMUM_LIMIT);
    }
}
