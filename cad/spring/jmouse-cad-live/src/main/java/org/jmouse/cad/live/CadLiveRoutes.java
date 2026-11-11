package org.jmouse.cad.live;

import org.jmouse.core.management.ManagementEndpoints;

/**
 * 🛣️ Where the live socket answers: {@code jmouse.cad.live.path}, by default {@code /jmouse/cad/live} — beside the REST
 * API at {@code /jmouse/cad/api}, under the same shared root.
 *
 * <p>⚠️ A placeholder, like every library address here, but resolved by this module itself rather than by Spring MVC: a
 * JSR-356 endpoint is registered with the container, which knows nothing of placeholders. A product's security chain,
 * its dev-server proxy (with WebSocket forwarding on) and its interface must be told the same value.</p>
 */
public final class CadLiveRoutes {

    /** This module's own segment under the shared root. */
    public static final String SEGMENT = "/cad/live";

    /** Where the socket answers, before the placeholder is resolved. */
    public static final String PATH = "${jmouse.cad.live.path:" + ManagementEndpoints.ROOT + SEGMENT + "}";

    private CadLiveRoutes() {
    }
}
