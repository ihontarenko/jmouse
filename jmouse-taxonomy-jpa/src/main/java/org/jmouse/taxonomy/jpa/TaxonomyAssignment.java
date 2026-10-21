package org.jmouse.taxonomy.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * 📌 One entity filed in one category: {@code (category, entity type, entity id)}, at most once.
 *
 * <p>The taxonomy is denormalised onto the row so "every assignment of this taxonomy" — what a shelf reads to filter
 * by — is one indexed read rather than a join through the categories. The database removes a category's assignments
 * with it (a foreign key with {@code ON DELETE CASCADE}), which is what keeps a deleted subtree from leaving rows that
 * point at nothing.</p>
 */
@Entity
@Table(
    name = "taxonomy_assignments",
    uniqueConstraints = @UniqueConstraint(name = "unique_taxonomy_assignments_entity",
                                          columnNames = {"category_id", "entity_type", "entity_id"})
)
public class TaxonomyAssignment {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "taxonomy_id", length = 36, nullable = false, updatable = false)
    private String taxonomyId;

    @Column(name = "category_id", length = 36, nullable = false, updatable = false)
    private String categoryId;

    @Column(name = "entity_type", length = 64, nullable = false, updatable = false)
    private String entityType;

    @Column(name = "entity_id", length = 128, nullable = false, updatable = false)
    private String entityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TaxonomyAssignment() {
    }

    public TaxonomyAssignment(String id, String taxonomyId, String categoryId, String entityType, String entityId) {
        this.id = id;
        this.taxonomyId = taxonomyId;
        this.categoryId = categoryId;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    @PrePersist
    void created() {
        createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getTaxonomyId() {
        return taxonomyId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
