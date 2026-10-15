package org.jmouse.parallax.jpa.collection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jmouse.parallax.owner.ParallaxOwner;

import java.time.Instant;
import java.util.Optional;

/**
 * 🗂️ A collection: a curated shelf every reader sees, one a person keeps, or a formula set.
 *
 * <p>Its items are rows of their own ({@link CollectionItem}), written through {@link CollectionStore}. A personal
 * collection is owned by its account; one of visibility {@code WORKSPACE} names that workspace as its realm.</p>
 */
@Entity(name = "ObjectCollection")
@Table(name = "collections")
public class ObjectCollection {

    /** Who keeps it: the portal's curators, or one person. */
    public enum Kind { CURATED, PERSONAL }

    /** Who sees it. */
    public enum Visibility { PRIVATE, WORKSPACE, PUBLIC }

    /** What it is for: a shelf of things to look at, or a formula set. */
    public enum Purpose { SHELF, FORMULA_SET }

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 500)
    private String summary;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(length = Length.LONG32)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private Kind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Visibility visibility;

    @Column(name = "owner_type", length = 64, updatable = false)
    private String ownerType;

    @Column(name = "owner_id", length = 64, updatable = false)
    private String ownerId;

    @Column(name = "realm_type", length = 64)
    private String realmType;

    @Column(name = "realm_id", length = 64)
    private String realmId;

    @Column(name = "cover_object_id", length = 36)
    private String coverObjectId;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ObjectCollection() {
    }

    /** @param owner the account a personal collection belongs to; null for a curated one */
    public ObjectCollection(String id, String slug, Kind kind, Purpose purpose, ParallaxOwner owner, Instant now) {
        this.id = id;
        this.slug = slug;
        this.kind = kind;
        this.purpose = purpose;
        this.ownerType = owner == null ? null : owner.type();
        this.ownerId = owner == null ? null : owner.id();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** @param realm the workspace a collection of visibility WORKSPACE is seen in; null for any other */
    public void describe(String title, String summary, String description, Visibility visibility, ParallaxOwner realm,
                         String coverObjectId, int position, Instant now) {
        this.title = title;
        this.summary = summary;
        this.description = description;
        this.visibility = visibility;
        this.realmType = realm == null ? null : realm.type();
        this.realmId = realm == null ? null : realm.id();
        this.coverObjectId = coverObjectId;
        this.position = position;
        this.updatedAt = now;
    }

    public void touch(Instant now) {
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

    public String getDescription() {
        return description;
    }

    public Kind getKind() {
        return kind;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    /** The account a personal collection belongs to; empty for a curated one. */
    public Optional<ParallaxOwner> owner() {
        return ownerType == null ? Optional.empty() : Optional.of(ParallaxOwner.of(ownerType, ownerId));
    }

    /** The workspace a collection of visibility WORKSPACE is seen in; empty otherwise. */
    public Optional<ParallaxOwner> realm() {
        return realmType == null ? Optional.empty() : Optional.of(ParallaxOwner.of(realmType, realmId));
    }

    public String getCoverObjectId() {
        return coverObjectId;
    }

    public int getPosition() {
        return position;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
