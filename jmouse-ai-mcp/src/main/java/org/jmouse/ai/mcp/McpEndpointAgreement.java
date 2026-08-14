package org.jmouse.ai.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Two unrelated things have to name the same path, and one startup check that says so.
 *
 * <p>The protocol is served somewhere. A credential issued for the protocol is confined somewhere.
 * <strong>If those two paths ever disagree, the protocol is being served outside the boundary that was
 * meant to contain it</strong> — every tool still reachable, the authentication filter still perfectly
 * correct about a path nothing is listening on. Nothing logs it, nothing fails, and a review of either
 * side alone looks right.
 *
 * <p>Which is why the duplication is made safe rather than removed. The two values genuinely live in
 * two places — a transport takes one as configuration, a security filter takes the other — and a
 * library cannot own either. What it can own is the refusal to start when they differ.
 *
 * <p>The check is the product's to call, with both of its own values, because only the product knows
 * what its filter was configured with.
 */
public final class McpEndpointAgreement {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpEndpointAgreement.class);

    private McpEndpointAgreement() {
    }

    /**
     * @param servedAt               where the transport publishes the protocol
     * @param credentialsConfinedTo  where authentication accepts a protocol credential and nowhere else
     * @throws IllegalStateException naming both values and what their disagreement would mean
     */
    public static void require(String servedAt, String credentialsConfinedTo) {
        if (servedAt != null && servedAt.equals(credentialsConfinedTo)) {
            LOGGER.info("Model Context Protocol served at {}, credentials confined to the same path",
                    servedAt);
            return;
        }

        throw new IllegalStateException(
                "The Model Context Protocol is served at '" + servedAt + "' but credentials for it are "
                + "confined to '" + credentialsConfinedTo + "'. These must be the same path. As they "
                + "stand, the protocol is reachable somewhere authentication was never configured to "
                + "confine a credential to — which moves the whole feature outside its security "
                + "boundary, and does so without failing, logging, or looking wrong from either side.");
    }
}
