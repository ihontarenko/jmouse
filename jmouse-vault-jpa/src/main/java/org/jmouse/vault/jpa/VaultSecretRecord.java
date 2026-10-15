package org.jmouse.vault.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 🗃️ One sealed secret. ⚠️ No accessor opens it and {@link #toString} never shows it: an entity loaded into
 * a persistence context must not be able to leak a key into a log line or a JSON response.
 *
 * <p>Map it with {@code @EntityScan({"<product package>", "org.jmouse.vault.jpa"})}.
 */
@Entity
@Table(name = "vault_secrets")
public class VaultSecretRecord {

    @Id
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "sealed_value", nullable = false, columnDefinition = "TEXT")
    private String sealedValue;

    @Column(name = "key_id", nullable = false, length = 32)
    private String keyIdentifier;

    @Column(name = "fingerprint", nullable = false, length = 16)
    private String fingerprint;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    protected VaultSecretRecord() {
        // Jakarta Persistence.
    }

    VaultSecretRecord(String name, LocalDateTime createdAt) {
        this.name      = name;
        this.createdAt = createdAt;
    }

    void seal(String sealedValue, String keyIdentifier, String fingerprint, LocalDateTime at, String actor) {
        this.sealedValue   = sealedValue;
        this.keyIdentifier = keyIdentifier;
        this.fingerprint   = fingerprint;
        touch(at, actor);
    }

    void reseal(String sealedValue, String keyIdentifier) {
        this.sealedValue   = sealedValue;
        this.keyIdentifier = keyIdentifier;
    }

    void describe(String description, LocalDateTime at, String actor) {
        this.description = description;
        touch(at, actor);
    }

    private void touch(LocalDateTime at, String actor) {
        this.updatedAt = at;
        this.updatedBy = actor;
    }

    public String getName() {
        return name;
    }

    String getSealedValue() {
        return sealedValue;
    }

    public String getKeyIdentifier() {
        return keyIdentifier;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public String toString() {
        return "VaultSecretRecord[name=%s, key=%s, fingerprint=%s]".formatted(name, keyIdentifier, fingerprint);
    }
}
