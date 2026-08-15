package org.jmouse.ai.agent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * The clients connected to an {@link Agent}: opening one, renewing it, and ending it.
 *
 * <p><strong>Where this stops and a product begins: minting.</strong> Nothing here signs a token, sets a
 * lifetime, or decides what a credential is confined to — products disagree about all three on purpose,
 * one signing with a secret only it holds and another issuing an ordinary credential refused elsewhere
 * by a declared check. This port stores the <em>connection</em>, which is the half they agree about
 * completely.
 *
 * <h2>The renewal credential, and why it never comes back out</h2>
 *
 * <p>⚠️ It goes in as itself and is stored as a SHA-256 digest — see {@link RefreshTokens}. A readable
 * renewal credential in a table is a credential anybody with a database session can use as its holder; a
 * digest is something they can only compare against. There is no accessor that returns one, no field on
 * {@link AgentConnection} that could hold one, and {@link #byRefreshToken(String)} is the only way to get
 * from a credential back to a row.
 *
 * <p>⚠️ <strong>Renewing rotates.</strong> {@link #rotate} replaces the digest rather than extending the
 * old one's life, so a leaked credential stops working the moment its legitimate holder renews. A
 * product that reuses one credential forever gets none of that and the row cannot tell.
 */
public interface AgentConnections {

    /** Something this port will not do, in a sentence somebody can act on. */
    class RefusedException extends RuntimeException {

        public RefusedException(String message) {
            super(message);
        }
    }

    /**
     * Records an approval somebody has just given, against a credential the product has just minted.
     *
     * @param clientName what the client called itself — ⚠️ a claim, never an identity
     */
    AgentConnection open(String agentId, String clientName, String refreshToken, Instant refreshExpiresAt);

    /** Replaces the renewal credential with a new one, which is what renewing means. */
    AgentConnection rotate(String connectionId, String refreshToken, Instant refreshExpiresAt);

    Optional<AgentConnection> find(String connectionId);

    /**
     * The connection a renewal credential belongs to, matched on its digest.
     *
     * <p>⚠️ Answers a revoked or expired connection rather than empty. "Unknown credential" and "the
     * connection you are renewing was ended" are different sentences to whoever is holding it, and a
     * port that collapsed them would force every caller to guess which had happened.
     */
    Optional<AgentConnection> byRefreshToken(String refreshToken);

    /** Every client connected to one agent, newest first. Includes revoked ones — a screen shows history. */
    List<AgentConnection> of(String agentId);

    /** Ends one client. The agent and its other connections carry on. */
    void revoke(String connectionId);

    /** Ends every client of one agent, without disabling the agent itself. */
    void revokeAllOf(String agentId);

    /**
     * Stamps a connection as used, so one nobody has touched in months is visible.
     *
     * <p>Cheap enough to call on every request, and worth it: this is the field that answers <em>which of
     * these four clients can I safely revoke</em>, which is the only question anybody asks of this
     * screen.
     */
    void stampUsed(String connectionId, Instant when);
}
