package org.jmouse.ai.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jmouse.ai.agent.AgentConnection;
import org.jmouse.ai.agent.AgentConnections;
import org.jmouse.ai.agent.RefreshTokens;
import org.jmouse.ai.jpa.entity.AiAgentConnection;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Connections, in {@code ai_agent_connections}.
 *
 * <p>⚠️ <strong>The renewal credential is hashed on the way in and never on the way out.</strong> Every
 * method taking one calls {@link RefreshTokens#digest(String)} before it touches a query, so the raw
 * value exists only as a parameter and never as a column, a log line or a returned field. There is no
 * method here that could hand one back, and {@link AgentConnection} has nowhere to put one.
 *
 * <p>Its own transaction, like everything else in this module — see {@link JpaAgentDirectory} for what
 * that costs and the one ordering rule that follows from it.
 */
public final class JpaAgentConnections implements AgentConnections {

    private final EntityManagerFactory entityManagerFactory;

    public JpaAgentConnections(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public AgentConnection open(
            String agentId, String clientName, String refreshToken, Instant refreshExpiresAt) {

        String digest = RefreshTokens.digest(refreshToken);

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiAgentConnection opened = new AiAgentConnection(
                    UUID.randomUUID().toString(),
                    agentId,
                    requireClientName(clientName),
                    digest,
                    refreshExpiresAt,
                    Instant.now());

            entityManager.persist(opened);

            return describe(opened);
        });
    }

    @Override
    public AgentConnection rotate(String connectionId, String refreshToken, Instant refreshExpiresAt) {
        String digest = RefreshTokens.digest(refreshToken);

        return OwnTransaction.call(entityManagerFactory, entityManager -> {
            AiAgentConnection connection = require(entityManager, connectionId);

            // Refused rather than rotated: handing a fresh credential to whoever presented a revoked
            // one's renewal half would undo the revocation, which is the one thing revocation is for.
            if (connection.getRevokedAt() != null) {
                throw new RefusedException(
                        "This connection was ended on " + connection.getRevokedAt() + " and cannot be "
                        + "renewed. Authorize again to open a new one.");
            }

            connection.rotateTo(digest, refreshExpiresAt);

            return describe(connection);
        });
    }

    @Override
    public Optional<AgentConnection> find(String connectionId) {
        return OwnTransaction.call(entityManagerFactory,
                entityManager -> row(entityManager, connectionId).map(JpaAgentConnections::describe));
    }

    /**
     * ⚠️ Answers a revoked or expired connection rather than empty, as the port promises: <em>unknown
     * credential</em> and <em>the connection you are renewing was ended</em> are different sentences to
     * whoever is holding it, and only the caller knows which of the two it wants to say.
     */
    @Override
    public Optional<AgentConnection> byRefreshToken(String refreshToken) {
        String digest = RefreshTokens.digest(refreshToken);

        return OwnTransaction.call(entityManagerFactory, entityManager -> entityManager.createQuery("""
                                select connection
                                  from AiAgentConnection connection
                                 where connection.refreshTokenHash = :digest
                                """, AiAgentConnection.class)
                        .setParameter("digest", digest)
                        .setMaxResults(1)
                        .getResultList()
                        .stream()
                        .findFirst()
                        .map(JpaAgentConnections::describe));
    }

    @Override
    public List<AgentConnection> of(String agentId) {
        return OwnTransaction.call(entityManagerFactory, entityManager -> entityManager.createQuery("""
                                select connection
                                  from AiAgentConnection connection
                                 where connection.agentId = :agentId
                                 order by connection.issuedAt desc
                                """, AiAgentConnection.class)
                        .setParameter("agentId", agentId)
                        .getResultList())
                .stream()
                .map(JpaAgentConnections::describe)
                .toList();
    }

    /** Ending an already-ended connection keeps the first timestamp: when it stopped working is a fact. */
    @Override
    public void revoke(String connectionId) {
        OwnTransaction.run(entityManagerFactory, entityManager -> {
            AiAgentConnection connection = require(entityManager, connectionId);

            if (connection.getRevokedAt() == null) {
                connection.setRevokedAt(Instant.now());
            }
        });
    }

    @Override
    public void revokeAllOf(String agentId) {
        OwnTransaction.run(entityManagerFactory, entityManager -> entityManager.createQuery("""
                        update AiAgentConnection connection
                           set connection.revokedAt = :now
                         where connection.agentId = :agentId
                           and connection.revokedAt is null
                        """)
                .setParameter("now", Instant.now())
                .setParameter("agentId", agentId)
                .executeUpdate());
    }

    /**
     * ⚠️ Silent about a connection that no longer exists, for the reason its sibling on the directory
     * gives: this runs after work that already succeeded.
     */
    @Override
    public void stampUsed(String connectionId, Instant when) {
        OwnTransaction.run(entityManagerFactory, entityManager -> entityManager.createQuery("""
                        update AiAgentConnection connection
                           set connection.lastUsedAt = :when
                         where connection.id = :connectionId
                        """)
                .setParameter("when", when)
                .setParameter("connectionId", connectionId)
                .executeUpdate());
    }

    private static String requireClientName(String clientName) {
        if (clientName == null || clientName.isBlank()) {
            throw new RefusedException(
                    "A connection has to record what the client called itself. It is the only thing "
                    + "distinguishing one of an agent's connections from another on the screen somebody "
                    + "revokes them from.");
        }

        return clientName.trim();
    }

    private AiAgentConnection require(EntityManager entityManager, String connectionId) {
        return row(entityManager, connectionId).orElseThrow(() -> new RefusedException(
                "No connection with id '" + connectionId + "' exists. It may have gone with the agent "
                + "it belonged to."));
    }

    private Optional<AiAgentConnection> row(EntityManager entityManager, String connectionId) {
        return Optional.ofNullable(entityManager.find(AiAgentConnection.class, connectionId));
    }

    private static AgentConnection describe(AiAgentConnection connection) {
        return new AgentConnection(
                connection.getId(),
                connection.getAgentId(),
                connection.getClientName(),
                connection.getIssuedAt(),
                connection.getRefreshExpiresAt(),
                connection.getLastUsedAt(),
                connection.getRevokedAt());
    }
}
