package org.jmouse.ai.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A preview that has been shown and not yet acted on.
 *
 * <p>For a product with nowhere else to put one. The in-memory store shipped with the guards is
 * correct and sufficient for a single process; the moment there are two, a token issued by one and
 * redeemed against the other is refused as unknown — and the user, who did exactly what they were
 * told, is told their confirmation expired.
 *
 * <p>⚠️ <strong>{@link #records} is the frozen set, serialised.</strong> That is the entire point of the
 * row: a preview is a contract about <em>specific records</em>, not an estimate of how many there might
 * be by the time somebody agrees. Storing the filter instead would let the confirming call touch records
 * the preview never showed, which is the bug two-step confirmation exists to prevent.
 *
 * <p>⚠️ It therefore contains a snapshot of somebody's data, and it lives as long as the token does and
 * no longer. Expired rows are swept, not kept: this table is a waiting room, not a trail.
 */
@Entity
@Table(name = AiPendingConfirmation.TABLE_NAME)
public class AiPendingConfirmation {

    public static final String TABLE_NAME = "ai_pending_confirmations";

    /** The token itself is the key: it is single-use, random, and never looked up by anything else. */
    @Id
    @Column(name = "token", length = 64, nullable = false, updatable = false)
    private String token;

    @Column(name = "operation_id", length = 32, nullable = false, updatable = false)
    private String operationId;

    @Column(name = "caller_id", length = 64, nullable = false, updatable = false)
    private String callerId;

    @Column(name = "published_name", length = 128, nullable = false, updatable = false)
    private String publishedName;

    /** What makes this call the same call — checked so changed arguments cannot redeem the preview. */
    @Column(name = "fingerprint", length = 64, nullable = false, updatable = false)
    private String fingerprint;

    /** Null for an action that is not confined to a scope, which is legal including for a write. */
    @Column(name = "scope_id", length = 64, updatable = false)
    private String scopeId;

    /**
     * The frozen record set, as JSON.
     *
     * <p>⚠️ <strong>The length is not decoration, and leaving it out is a startup failure.</strong>
     * {@code @Lob} on a {@code String} asks for a character large object, which a dialect sizes from
     * the column's length — unset, that is 255, so MySQL is told {@code TINYTEXT} while this module's
     * own migration writes {@code LONGTEXT}. Under {@code ddl-auto: validate} the two disagree and the
     * application does not start. Under a product that does not validate, a preview is silently
     * truncated to 255 characters, which is the same bug wearing a much worse face.
     *
     * <p>Stated this way rather than through a Hibernate annotation because this module is Jakarta
     * Persistence and nothing else — an architecture rule says so, and one convenient import is how
     * that stops being true. The length maps to {@code LONGTEXT} on MySQL and {@code TEXT} on
     * PostgreSQL, which is exactly what the two migrations create.
     */
    @Lob
    @Column(name = "records", nullable = false, updatable = false, length = Integer.MAX_VALUE)
    private String records;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    protected AiPendingConfirmation() {
    }

    public AiPendingConfirmation(
            String token, String operationId, String callerId, String publishedName,
            String fingerprint, String scopeId, String records, Instant expiresAt) {

        this.token         = token;
        this.operationId   = operationId;
        this.callerId      = callerId;
        this.publishedName = publishedName;
        this.fingerprint   = fingerprint;
        this.scopeId       = scopeId;
        this.records       = records;
        this.expiresAt     = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getCallerId() {
        return callerId;
    }

    public String getPublishedName() {
        return publishedName;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getScopeId() {
        return scopeId;
    }

    public String getRecords() {
        return records;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean hasExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
