package org.jmouse.ai.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One wording this installation keeps for one AI setting — its prompt, most obviously.
 *
 * <p><strong>Several rows per setting, one of them {@link #inForce}.</strong> The same shape
 * {@link AiProviderSettings} has, because it is the same situation: keeping the long prompt while
 * trying the short one, and switching back with a press.
 *
 * <p>⚠️ <strong>Seeded from the product's declaration, never from a migration.</strong> A library has no
 * idea what an application's assistant should say — {@code JpaAiPreferences} fills a setting that has no
 * rows at all, once, from what the product ships. An edited row is never overwritten by a restart.
 *
 * <p>⚠️ {@link #application} is the lookup key, exactly as it is for provider settings: one
 * installation's table may serve several applications, and a name that travelled in a request would be
 * a screen able to rewrite somebody else's prompt.
 */
@Entity
@Table(name = AiPreference.TABLE_NAME)
public class AiPreference {

    public static final String TABLE_NAME = "ai_preferences";

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "application", length = 64, nullable = false, updatable = false)
    private String application;

    /** Which setting this is a value for; matched against a declared {@code PreferenceDefinition}. */
    @Column(name = "name", length = 128, nullable = false, updatable = false)
    private String name;

    /** What somebody calls this wording — "Extended", "Compact", "Ours". */
    @Column(name = "label", length = 128, nullable = false)
    private String label;

    /**
     * ⚠️ <strong>The length is not decoration</strong> — see {@link AiPendingConfirmation} for the
     * whole of it. {@code @Lob} on a {@code String} is sized from the column's length, and left unset
     * that is 255: MySQL would be told {@code TINYTEXT} while the migration writes {@code LONGTEXT},
     * and under {@code ddl-auto: validate} the application does not start. Under a product that does
     * not validate, a prompt is silently cut to 255 characters instead.
     */
    @Lob
    @Column(name = "value", nullable = false, length = Integer.MAX_VALUE)
    private String value;

    @Column(name = "in_force", nullable = false)
    private boolean inForce;

    /**
     * Which shipped variant this row started as, or null for one somebody wrote here.
     *
     * <p>Provenance only — nothing reads it at runtime. What it buys is <em>put this back to what the
     * build ships</em>, which is the difference between experimenting and losing the original.
     */
    @Column(name = "seed_key", length = 64, updatable = false)
    private String seedKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AiPreference() {
    }

    public AiPreference(
            String        id,
            String        application,
            String        name,
            String        label,
            String        value,
            boolean       inForce,
            String        seedKey,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        this.id          = id;
        this.application = application;
        this.name        = name;
        this.label       = label;
        this.value       = value;
        this.inForce     = inForce;
        this.seedKey     = seedKey;
        this.createdAt   = createdAt;
        this.updatedAt   = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getApplication() {
        return application;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public boolean isInForce() {
        return inForce;
    }

    public String getSeedKey() {
        return seedKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ── What a change may touch ──────────────────────────────────────────────────
    //
    // The wording, its name, whether it is the one in force, and when that last
    // happened. Which setting this is a value for, whose application it belongs to
    // and what it was seeded from are what the row *is* — moving any of them would
    // be deleting one row and creating another.

    public void setLabel(String label) {
        this.label = label;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * ⚠️ At most one row per setting should carry this, and nothing here enforces it — the constraint
     * spans rows, so it belongs to whatever administers them. {@code JpaAiPreferences} takes the
     * previous one out of force in the same transaction.
     */
    public void setInForce(boolean inForce) {
        this.inForce = inForce;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
