package org.jmouse.access.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * How much one subject has used of one meter, in one window.
 *
 * <h2>Why a row per window rather than a sum over events</h2>
 *
 * <p>Asking <em>"how many tokens in the last three hours"</em> is one primary-key lookup rather than an
 * aggregate over every call since the account opened. Resolution runs on the decision path; an
 * aggregate there gets slower every day the product is successful.
 *
 * <h2>⚠️ {@code subject_type} is a string column, not an enumeration</h2>
 *
 * <p>Deliberately, and it is the difference between this table and the product one it replaces. A
 * counter may be about a person, an agent, a group, a tenant or something a product invents later; the
 * library never reads the value, so nothing here has to change when a new kind appears.
 */
@Entity
@Table(name = "access_consumption_counters")
public class AccessConsumptionCounter {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "subject_type", length = 64, nullable = false)
    private String subjectType;

    @Column(name = "subject_id", length = 36, nullable = false)
    private String subjectId;

    @Column(name = "meter", length = 64, nullable = false)
    private String meter;

    /** Which window this counts: {@code 2026-08}, {@code 2026-08-16-04}, {@code ever}. */
    @Column(name = "window_key", length = 32, nullable = false)
    private String windowKey;

    @Column(name = "consumed", nullable = false)
    private long consumed;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccessConsumptionCounter() {
    }

    public AccessConsumptionCounter(
            String id, String subjectType, String subjectId, String meter, String windowKey,
            long consumed, Instant updatedAt) {

        this.id          = id;
        this.subjectType = subjectType;
        this.subjectId   = subjectId;
        this.meter       = meter;
        this.windowKey   = windowKey;
        this.consumed    = consumed;
        this.updatedAt   = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getMeter() {
        return meter;
    }

    public String getWindowKey() {
        return windowKey;
    }

    public long getConsumed() {
        return consumed;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
