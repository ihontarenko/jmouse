package org.jmouse.taxonomy.jpa;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 🗂️ One named tree of categories — «Catalogue», «Sources», «Lessons» — and the kinds of thing it may classify.
 *
 * <p>Several taxonomies classify the same entities independently: a component sits under «Semiconductors / Diodes»
 * in one and under «KiCad / Device» in another. Each is its own nested set, numbered apart ({@link TaxonomyCategory}
 * is scoped by its taxonomy).</p>
 */
@Entity
@Table(name = "taxonomies")
public class Taxonomy {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    /** How hosts and links name it: {@code catalogue}. Never changes once made. */
    @Column(name = "code", length = 64, nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "title", length = 160, nullable = false)
    private String title;

    @Column(name = "description", length = 1024)
    private String description;

    /** The registered entity types its categories may hold; an assignment of another type is refused. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "taxonomy_entity_types", joinColumns = @JoinColumn(name = "taxonomy_id"))
    @Column(name = "entity_type", length = 64, nullable = false)
    private Set<String> entityTypes = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Taxonomy() {
    }

    public Taxonomy(String id, String code, String title, String description, Set<String> entityTypes) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.description = description;
        this.entityTypes = new LinkedHashSet<>(entityTypes);
    }

    @PrePersist
    void created() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void updated() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getEntityTypes() {
        return Set.copyOf(entityTypes);
    }

    public void setEntityTypes(Set<String> entityTypes) {
        this.entityTypes.clear();
        this.entityTypes.addAll(entityTypes);
    }

    /** Whether its categories may hold entities of this type. */
    public boolean classifies(String entityType) {
        return entityTypes.contains(entityType);
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
