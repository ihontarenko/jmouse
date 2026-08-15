package org.jmouse.ai.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One client's standing permission to act as one {@link AiAgent}.
 *
 * <p>⚠️ <strong>{@link #refreshTokenHash} is a digest and the column can hold nothing else.</strong>
 * Sixty-four characters is exactly a SHA-256 in hexadecimal, so a raw credential written here would be
 * rejected by the column rather than stored quietly — which is the one place a length limit is doing
 * security rather than housekeeping.
 *
 * <p><strong>{@code agent_id} is a plain column rather than a {@code @ManyToOne}.</strong> Every use of
 * it is either a lookup by agent or a write of the identifier a caller already holds, and an association
 * would turn each of those into an extra select for a row nobody reads. The foreign key is real and lives
 * in the migration, where it belongs — the constraint is the database's job and the mapping is not what
 * enforces it.
 */
@Entity
@Table(name = AiAgentConnection.TABLE_NAME)
public class AiAgentConnection {

    public static final String TABLE_NAME = "ai_agent_connections";

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "agent_id", length = 36, nullable = false, updatable = false)
    private String agentId;

    /** What the client called itself when it registered. ⚠️ A claim, shown as one, never an identity. */
    @Column(name = "client_name", length = 255, nullable = false)
    private String clientName;

    @Column(name = "refresh_token_hash", length = 64, nullable = false)
    private String refreshTokenHash;

    @Column(name = "refresh_expires_at", nullable = false)
    private Instant refreshExpiresAt;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /** Set once and never unset: a connection somebody ended does not come back. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AiAgentConnection() {
    }

    public AiAgentConnection(
            String  id,
            String  agentId,
            String  clientName,
            String  refreshTokenHash,
            Instant refreshExpiresAt,
            Instant issuedAt) {

        this.id               = id;
        this.agentId          = agentId;
        this.clientName       = clientName;
        this.refreshTokenHash = refreshTokenHash;
        this.refreshExpiresAt = refreshExpiresAt;
        this.issuedAt         = issuedAt;
    }

    public String getId() {
        return id;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getClientName() {
        return clientName;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public Instant getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    /** Renewing replaces the credential rather than extending the old one's life. */
    public void rotateTo(String refreshTokenHash, Instant refreshExpiresAt) {
        this.refreshTokenHash = refreshTokenHash;
        this.refreshExpiresAt = refreshExpiresAt;
    }
}
