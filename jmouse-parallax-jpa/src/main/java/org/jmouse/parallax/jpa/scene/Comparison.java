package org.jmouse.parallax.jpa.scene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.jmouse.parallax.owner.ParallaxOwner;

import java.time.Instant;

/** Compare mode: slots side by side, with explicit switches for sharing the camera and the parameters. */
@Entity(name = "Comparison")
@Table(name = "comparisons")
public class Comparison {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "owner_type", nullable = false, length = 64, updatable = false)
    private String ownerType;

    @Column(name = "owner_id", nullable = false, length = 64, updatable = false)
    private String ownerId;

    @Column(name = "realm_type", nullable = false, length = 64, updatable = false)
    private String realmType;

    @Column(name = "realm_id", nullable = false, length = 64, updatable = false)
    private String realmId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ComparisonLayout layout;

    @Column(name = "synchronize_camera", nullable = false)
    private boolean synchronizeCamera;

    @Column(name = "synchronize_parameters", nullable = false)
    private boolean synchronizeParameters;

    @Column(name = "created_by", nullable = false, length = 36, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Comparison() {
    }

    public Comparison(String id, ParallaxOwner owner, ParallaxOwner realm, String createdBy, Instant now) {
        this.id = id;
        this.ownerType = owner.type();
        this.ownerId = owner.id();
        this.realmType = realm.type();
        this.realmId = realm.id();
        this.createdBy = createdBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void describe(String title, String description, ComparisonLayout layout, boolean synchronizeCamera,
                         boolean synchronizeParameters, Instant now) {
        this.title = title;
        this.description = description;
        this.layout = layout;
        this.synchronizeCamera = synchronizeCamera;
        this.synchronizeParameters = synchronizeParameters;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    /** What it belongs to: a project. */
    public ParallaxOwner owner() {
        return ParallaxOwner.of(ownerType, ownerId);
    }

    /** The outermost place it sits in: the project's workspace. */
    public ParallaxOwner realm() {
        return ParallaxOwner.of(realmType, realmId);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ComparisonLayout getLayout() {
        return layout;
    }

    public boolean isSynchronizeCamera() {
        return synchronizeCamera;
    }

    public boolean isSynchronizeParameters() {
        return synchronizeParameters;
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

    public long getVersion() {
        return version;
    }
}
