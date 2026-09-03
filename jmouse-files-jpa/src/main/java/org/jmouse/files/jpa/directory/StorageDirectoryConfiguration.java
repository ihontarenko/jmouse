package org.jmouse.files.jpa.directory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jmouse.files.directory.DirectoryConfigurationKind;

import java.time.LocalDateTime;

/**
 * 🔧 One folder's answer to one question — a typed configuration document, filed by kind.
 *
 * <h3>⚠️ A table rather than columns on {@code storage_directories}, and the reasons keep</h3>
 *
 * <p>Somebody will propose four nullable columns again, so: a column per setting is a migration per
 * setting in a library whose migrations run inside three products, and the second setting costs exactly
 * what the first one did. {@code storage_directories} is the <em>tree</em> — its numbering is its
 * mechanics, and acceptance rules, retention windows and naming strategies are lodgers there, each one
 * making the row look less like a node. And "no rule" against "a rule admitting nothing" would need a
 * sentinel: four NULLs against an allowlist with empty lists is the distinction that gets read wrong
 * exactly once, permanently. Here it is <em>no row</em> against <em>a row</em>, and there is nothing to
 * misread.</p>
 *
 * <h3>⚠️ Inheritance is at read time; a row is never copied down</h3>
 *
 * <p>A new child gets no rows at all. What applies to it is resolved by walking up, so a parent whose
 * rule changes changes what its descendants get — which is the whole point. Copying a configuration
 * into every child would fill the tree with stale duplicates of a decision somebody has since
 * revised.</p>
 *
 * <p>Rows travel with a move untouched, because they are keyed by directory. What changes is what a
 * folder <em>without</em> a row of its own inherits, which is exactly what moving a folder means.</p>
 */
@Entity
@Table(
    name = "storage_directory_configurations",
    uniqueConstraints = @UniqueConstraint(name = "unique_storage_directory_configurations_kind",
                                          columnNames = {"directory_id", "kind"})
)
public class StorageDirectoryConfiguration {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "directory_id", length = 36, nullable = false, updatable = false)
    private String directoryId;

    /**
     * Which question this row answers — {@code upload} today.
     *
     * <p>⚠️ Owned by whoever contributes it, exactly like {@code OwnerReference.ownerType}, and checked
     * against {@code DirectoryConfigurationKinds} before anything is written. An unregistered kind is
     * refused rather than stored, because a row nothing can bind is a row nothing will ever read.</p>
     */
    @Column(name = "kind", length = DirectoryConfigurationKind.MAXIMUM_NAME_LENGTH,
            nullable = false, updatable = false)
    private String kind;

    /**
     * That kind's own document.
     *
     * <p>⚠️ {@code TEXT} rather than a native JSON column, on both dialects. This module maps with plain
     * Jakarta Persistence and no Hibernate annotations, and PostgreSQL refuses a {@code varchar} bound
     * into {@code jsonb} without a cast that only a vendor annotation can ask for. Queryability inside a
     * payload — "which folders admit html" — is answered by the effective-rule view instead, which has
     * to walk the tree for inheritance anyway and so could never have been one {@code WHERE} clause.</p>
     */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected StorageDirectoryConfiguration() {
    }

    /**
     * 🏗️ A folder's answer of one kind.
     *
     * @param id          identifier the caller minted
     * @param directoryId the folder it belongs to
     * @param kind        which question it answers
     * @param payload     that kind's document
     */
    public StorageDirectoryConfiguration(String id, String directoryId, String kind, String payload) {
        this.id          = id;
        this.directoryId = directoryId;
        this.kind        = kind;
        this.payload     = payload;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getDirectoryId() {
        return directoryId;
    }

    public String getKind() {
        return kind;
    }

    public String getPayload() {
        return payload;
    }

    /**
     * ✏️ Replace the document, keeping the row.
     *
     * @param payload the new document
     */
    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
