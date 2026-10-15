package org.jmouse.parallax.jpa.catalogue;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jmouse.parallax.object.ObjectKind;
import org.jmouse.parallax.owner.ParallaxOwner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The index of a catalogue object or of an owned one: what lists, searches and authorization need without reading a
 * document. The object itself is its jMX text in {@link ObjectSources}; every column here except identity, owner and
 * status is derived from the head revision when it is saved, and never edited on its own. An object with no owner is
 * the shared catalogue's; an owned one belongs to whatever the host names (a workspace).
 */
@Entity
@Table(name = "objects")
public class MathematicalObject {

    public enum Status { DRAFT, PUBLISHED, ARCHIVED }

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 120, updatable = false)
    private String slug;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 500)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ObjectKind kind;

    @Column(name = "category_code", nullable = false, length = 40)
    private String categoryCode;

    @Column(nullable = false)
    private int revision;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "source_checksum", nullable = false, length = 64)
    private String sourceChecksum;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "shipped_checksum", length = 64)
    private String shippedChecksum;

    @Column(name = "shipped_update_pending", nullable = false)
    private boolean shippedUpdatePending;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(nullable = false)
    private boolean featured;

    @Column(name = "owner_type", length = 64, updatable = false)
    private String ownerType;

    @Column(name = "owner_id", length = 64, updatable = false)
    private String ownerId;

    @Column(name = "created_by", length = 36, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Version
    private long version;

    @ElementCollection
    @CollectionTable(name = "object_relations", joinColumns = @JoinColumn(name = "object_id"))
    @OrderBy("position")
    private List<Relation> relations = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "object_tags", joinColumns = @JoinColumn(name = "object_id"))
    @Column(name = "tag", length = 64)
    private Set<String> tags = new LinkedHashSet<>();

    protected MathematicalObject() {
    }

    /**
     * @param owner     null for the shared catalogue
     * @param createdBy the actor who wrote it; null for the shipped catalogue
     */
    public MathematicalObject(String id, String slug, ParallaxOwner owner, String createdBy, Instant now) {
        this.id = id;
        this.slug = slug;
        this.ownerType = owner == null ? null : owner.type();
        this.ownerId = owner == null ? null : owner.id();
        this.createdBy = createdBy;
        this.status = Status.DRAFT;
        this.revision = 0;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Moves the object to a new head revision and takes the index columns from what that revision describes.
     *
     * @param checksum the new head text's checksum
     */
    public void indexRevision(int revision, String checksum, ObjectKind kind, String title, String summary,
                              String categoryCode, boolean featured, Instant now) {
        this.revision = revision;
        this.sourceChecksum = checksum;
        this.kind = kind;
        this.title = title;
        this.summary = summary;
        this.categoryCode = categoryCode;
        this.featured = featured;
        this.updatedAt = now;
    }

    /** The shipped catalogue's file was taken as it is: nothing newer is waiting. */
    public void recordShippedText(String checksum) {
        this.shippedChecksum = checksum;
        this.shippedUpdatePending = false;
    }

    /** The shipped file has a newer text, kept back because a curator has edited the object since. */
    public void recordShippedUpdateKept(String checksum) {
        this.shippedChecksum = checksum;
        this.shippedUpdatePending = true;
    }

    public void replaceRelations(List<Relation> relations) {
        this.relations.clear();
        this.relations.addAll(relations);
    }

    public void replaceTags(Set<String> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    public void changeStatus(Status status, Instant now) {
        if (status == Status.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = now;
        }
        this.status = status;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public ObjectKind getKind() {
        return kind;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    /** The head revision of the object's text: what {@code slug@revision} names, and what a dataset was computed from. */
    public int getRevision() {
        return revision;
    }

    public String getSourceChecksum() {
        return sourceChecksum;
    }

    public String getShippedChecksum() {
        return shippedChecksum;
    }

    public boolean isShippedUpdatePending() {
        return shippedUpdatePending;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isFeatured() {
        return featured;
    }

    /** Who the object belongs to; empty for the shared catalogue. */
    public Optional<ParallaxOwner> owner() {
        return ownerType == null ? Optional.empty() : Optional.of(ParallaxOwner.of(ownerType, ownerId));
    }

    /** Whether the object is the shared catalogue's rather than an owner's. */
    public boolean isCatalogue() {
        return ownerType == null;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public long getVersion() {
        return version;
    }

    public List<Relation> getRelations() {
        return relations;
    }

    public Set<String> getTags() {
        return tags;
    }

    /** One related object, derived from the text: a relation field of {@code meta}, a pinned part, a symbol. */
    @Embeddable
    public static class Relation {

        @Column(name = "related_object_id", nullable = false, length = 36)
        private String relatedObjectId;

        @Column(name = "relation_kind", nullable = false, length = 24)
        private String kind;

        @Column(nullable = false)
        private int position;

        protected Relation() {
        }

        public Relation(String relatedObjectId, String kind, int position) {
            this.relatedObjectId = relatedObjectId;
            this.kind = kind;
            this.position = position;
        }

        public String getRelatedObjectId() {
            return relatedObjectId;
        }

        public String getKind() {
            return kind;
        }

        public int getPosition() {
            return position;
        }
    }
}
