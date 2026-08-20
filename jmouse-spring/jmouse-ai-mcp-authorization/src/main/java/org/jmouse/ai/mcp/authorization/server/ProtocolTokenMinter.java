package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.agent.Agent;
import org.jmouse.ai.agent.AgentConnection;

/**
 * How an access token is made, which is the one thing about a protocol credential that products
 * genuinely disagree about.
 *
 * <h2>⚠️ The first cut of this extraction put the seam in the wrong place</h2>
 *
 * <p>{@link CredentialOwners} was taken to be the whole difference, because Tessera and WiQ differed
 * only in which table an owner lives in. Innoventa then did not fit at all — and looking at why is what
 * showed the seam was one level too shallow.
 *
 * <p>Tessera and WiQ sign an <strong>HS256 token with a secret only that product holds</strong>, so the
 * credential is confined to the protocol endpoint by the fact that no other decoder can verify it.
 * Innoventa mints an <strong>ordinary product JWT</strong> and confines it with a gate on the route
 * instead. Both are correct, neither is a variation on the other, and everything <em>around</em> them —
 * enrolment, the four refusals, rotation, the sliding window, revocation — is the same in all three.
 *
 * <p>So this is the seam, and it is one method wide. {@link SignedTokenMinter} is the default, which is
 * what two of the three products want without writing anything.
 */
@FunctionalInterface
public interface ProtocolTokenMinter {

    /**
     * An access token for this agent, over this connection.
     *
     * @param connection the connection it is issued against — its identifier is what makes a
     *                   self-contained token revocable, so a minter that drops it produces a credential
     *                   nobody can take back before it expires
     */
    MintedToken mint(Agent agent, AgentConnection connection);

    /** What was minted, and for how long a client may act on it before renewing. */
    record MintedToken(String accessToken, long expiresInSeconds) {
    }
}
