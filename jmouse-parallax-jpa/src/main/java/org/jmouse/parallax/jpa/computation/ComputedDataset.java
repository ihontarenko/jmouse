package org.jmouse.parallax.jpa.computation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jmouse.parallax.RepresentationKind;

import java.time.Instant;

/** A computed dataset, kept compressed and keyed by the fingerprint of everything that determined it. */
@Entity(name = "ComputedDataset")
@Table(name = "computed_datasets")
public class ComputedDataset {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 64, updatable = false)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "computation_kind", nullable = false, length = 40, updatable = false)
    private RepresentationKind representation;

    @Column(name = "dataset_format", nullable = false, length = 40, updatable = false)
    private String format;

    @Column(name = "object_id", length = 36, updatable = false)
    private String objectId;

    @JdbcTypeCode(SqlTypes.LONG32VARBINARY)
    @Column(length = Length.LONG32, nullable = false, updatable = false)
    private byte[] payload;

    @Column(name = "payload_bytes", nullable = false, updatable = false)
    private long payloadBytes;

    @Column(name = "element_count", nullable = false, updatable = false)
    private long elementCount;

    @Column(name = "compute_millis", nullable = false, updatable = false)
    private long computeMillis;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "use_count", nullable = false)
    private long useCount;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected ComputedDataset() {
    }

    public ComputedDataset(String id, String fingerprint, RepresentationKind representation, String format,
                           String objectId, byte[] payload, long payloadBytes, long elementCount, long computeMillis,
                           Instant now) {
        this.id = id;
        this.fingerprint = fingerprint;
        this.representation = representation;
        this.format = format;
        this.objectId = objectId;
        this.payload = payload;
        this.payloadBytes = payloadBytes;
        this.elementCount = elementCount;
        this.computeMillis = computeMillis;
        this.createdAt = now;
        this.lastUsedAt = now;
        this.useCount = 1;
    }

    public String getId() {
        return id;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public RepresentationKind getRepresentation() {
        return representation;
    }

    public String getFormat() {
        return format;
    }

    public String getObjectId() {
        return objectId;
    }

    public byte[] getPayload() {
        return payload;
    }

    public long getPayloadBytes() {
        return payloadBytes;
    }

    public long getElementCount() {
        return elementCount;
    }

    public long getComputeMillis() {
        return computeMillis;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public long getUseCount() {
        return useCount;
    }
}
