package org.jmouse.ai.agent;

import java.time.Instant;

/**
 * One client's standing permission to act as one {@link Agent}.
 *
 * <p><strong>Not the access token.</strong> A token is short-lived and, where a product signs its own,
 * never stored at all. This is the <em>connection</em>: the approval somebody gave, the credential that
 * renews it, and the switch that ends it. A token naming this row is what makes a self-contained
 * credential safe to hand out — without it, "revoke" cannot mean anything until the token expires on its
 * own.
 *
 * <p>⚠️ <strong>The renewal credential is never in this record.</strong> It is stored as a digest and
 * compared, never read back, so there is no field here that could hold one and no accessor that could
 * leak one. {@link AgentConnections#byRefreshToken(String)} is how a holder proves itself.
 *
 * @param clientName what the client called itself when it registered. ⚠️ A claim, shown as one — it is
 *                   for a log line and a screen, and is never an identity
 * @param revokedAt  set once and never unset: a connection somebody ended does not come back
 */
public record AgentConnection(
        String  id,
        String  agentId,
        String  clientName,
        Instant issuedAt,
        Instant refreshExpiresAt,
        Instant lastUsedAt,
        Instant revokedAt
) {

    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Whether the renewal half is still usable.
     *
     * <p>The access token's own expiry is in its claims and is not this row's business — asking here
     * would be asking the wrong table a question it cannot answer correctly.
     */
    public boolean canBeRenewed(Instant now) {
        return !isRevoked() && now.isBefore(refreshExpiresAt);
    }

    /** Whether anything has ever come through it — an approval nobody used looks exactly like this. */
    public boolean hasEverBeenUsed() {
        return lastUsedAt != null;
    }
}
