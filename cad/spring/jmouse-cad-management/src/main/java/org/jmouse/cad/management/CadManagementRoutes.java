package org.jmouse.cad.management;

import org.jmouse.core.management.ManagementEndpoints;

/**
 * 🧭 Where jmCAD's routes answer: {@code /jmouse/cad/api} unless a product moves them
 * ({@code jmouse.cad.management.prefix}), and how an owner is addressed below it.
 */
public final class CadManagementRoutes {

    /** The library's segment below the shared management root. */
    public static final String SEGMENT = "/cad/api";

    /** The routes' prefix, as a placeholder the handler mapping resolves. */
    public static final String PREFIX = "${jmouse.cad.management.prefix:" + ManagementEndpoints.ROOT + SEGMENT + "}";

    /** The path variable holding an owner's type. */
    public static final String OWNER_TYPE = "ownerType";

    /** The path variable holding an owner's identifier. */
    public static final String OWNER_ID = "ownerId";

    /** Whatever belongs to an owner: {@code /owners/PROJECT/42}. */
    public static final String OWNER = "/owners/{" + OWNER_TYPE + "}/{" + OWNER_ID + "}";

    private CadManagementRoutes() {
    }
}
