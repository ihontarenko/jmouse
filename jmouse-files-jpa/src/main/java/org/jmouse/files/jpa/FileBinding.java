package org.jmouse.files.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.jmouse.files.OwnerReference;

import java.time.LocalDateTime;

/**
 * 🔗 One statement that a file is filed against something.
 *
 * <h3>Why this is its own table rather than a column on the file</h3>
 *
 * <p>Because a file is not always in exactly one place, and the products that assumed it was ended up
 * unable to say otherwise. A picture inserted into two pages, a document attached to an issue and
 * also filed in a directory, a file whose place is being moved without losing the old one until the
 * move commits — none of those fit a single {@code owner_id} column, and all of them are ordinary.</p>
 *
 * <h3>⚠️ The one-place case is a rule the product applies, not a shape this enforces</h3>
 *
 * <p>A tree wants exactly one directory per file, and gets it by re-binding rather than by the schema
 * refusing a second row — see {@code FileBindings.refile}, which replaces every binding of the same
 * kind. Expressing "one place" as a unique constraint here would make the many-place products
 * unrepresentable, which is the more expensive of the two mistakes.</p>
 *
 * <h3>⚠️ {@code fileId} is an identifier, not a foreign key</h3>
 *
 * <p>Deliberately — and it was wrong once. The first version constrained it to {@link ManagedFile},
 * which quietly made this table unusable by the one product that already had a file table of its own:
 * it could not bind a row it did not own without replacing that table first, which is a fifteen-file
 * change to buy a join. The constraint was also incoherent, since {@code ownerId} has no key and cannot
 * have one, being polymorphic. A binding is polymorphic at both ends or at neither, which is what both
 * products that independently built this shape ({@code entity_categories}) already concluded.</p>
 *
 * <p>So the three pieces of this library separate as they were meant to — the tree, the binding and the
 * file row are three things, and a product takes the ones it needs.</p>
 *
 * <p>⚠️ The cost is accepted rather than avoided: a binding can outlive what it names. The sweeper does
 * not care, because it unions identifiers rather than joining; and every listing joins, so a stale
 * binding renders as nothing rather than as a broken row.</p>
 */
@Entity
@Table(name = "file_bindings")
@IdClass(FileBindingId.class)
public class FileBinding {

    @Id
    @Column(name = "file_id", length = 36, nullable = false, updatable = false)
    private String fileId;

    @Id
    @Column(name = "owner_type", length = OwnerReference.MAXIMUM_TYPE_LENGTH,
            nullable = false, updatable = false)
    private String ownerType;

    @Id
    @Column(name = "owner_id", length = OwnerReference.MAXIMUM_ID_LENGTH,
            nullable = false, updatable = false)
    private String ownerId;

    /**
     * Where this file sits among the others filed against the same owner.
     *
     * <p>Attachments on an issue have an order somebody chose; files in a directory usually do not.
     * Defaulted rather than nullable so a listing can always sort by it and fall back to the name.</p>
     */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FileBinding() {
    }

    /**
     * 🏗️ File one thing against another.
     *
     * @param fileId    the file
     * @param owner     what holds it
     * @param sortOrder where it sits among that owner's files
     */
    public FileBinding(String fileId, OwnerReference owner, int sortOrder) {
        this.fileId    = fileId;
        this.ownerType = owner.ownerType();
        this.ownerId   = owner.ownerId();
        this.sortOrder = sortOrder;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 🔗 The owner, read back as the value it was written from.
     *
     * @return the owner reference
     */
    public OwnerReference owner() {
        return OwnerReference.of(ownerType, ownerId);
    }

    public String getFileId() {
        return fileId;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "FileBinding[%s -> %s:%s]".formatted(fileId, ownerType, ownerId);
    }
}
