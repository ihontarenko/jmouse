package org.jmouse.vault.jpa;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** 🗃️ A structured credential's definition: kind, plain fields, and the names of its secret fields. */
@Entity
@Table(name = "vault_credentials")
public class VaultCredentialRecord {

    private static final String SEPARATOR = ",";

    @Id
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "kind", nullable = false, length = 64)
    private String kind;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "secret_fields", nullable = false, length = 1000)
    private String secretFields;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "vault_credential_fields", joinColumns = @JoinColumn(name = "credential_name"))
    @MapKeyColumn(name = "field_name", length = 64)
    @Column(name = "field_value", nullable = false, length = 2000)
    private Map<String, String> plainFields = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    protected VaultCredentialRecord() {
        // Jakarta Persistence.
    }

    VaultCredentialRecord(String name, LocalDateTime createdAt) {
        this.name      = name;
        this.createdAt = createdAt;
    }

    void define(String kind, String description, Map<String, String> plainFields, Set<String> secretFields,
                LocalDateTime at, String actor) {
        this.kind         = kind;
        this.description  = description;
        this.secretFields = String.join(SEPARATOR, secretFields);
        this.plainFields.clear();
        this.plainFields.putAll(plainFields);
        this.updatedAt    = at;
        this.updatedBy    = actor;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getSecretFields() {
        if (secretFields == null || secretFields.isBlank()) {
            return Set.of();
        }

        return new LinkedHashSet<>(Arrays.asList(secretFields.split(SEPARATOR)));
    }

    public Map<String, String> getPlainFields() {
        return Map.copyOf(plainFields);
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
