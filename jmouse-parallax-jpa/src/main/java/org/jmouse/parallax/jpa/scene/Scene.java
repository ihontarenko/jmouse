package org.jmouse.parallax.jpa.scene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.jmouse.parallax.owner.ParallaxOwner;

import java.time.Instant;

/**
 * 🎬 A scene: a versioned document of objects, their representations, the camera and the timeline.
 *
 * <p>The row names its head; every saved state is a {@link SceneRevision}. It belongs to an owner the host names (a
 * project) inside a realm (its workspace) — both stored as pairs, never as references into the host's tables.</p>
 */
@Entity(name = "Scene")
@Table(name = "scenes")
public class Scene {

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

    @Column(name = "instrument_code", length = 40)
    private String instrumentCode;

    @Column(name = "head_revision_number", nullable = false)
    private int headRevisionNumber;

    @Column(name = "forked_from_scene_id", length = 36, updatable = false)
    private String forkedFromSceneId;

    @Column(name = "forked_from_revision", updatable = false)
    private Integer forkedFromRevision;

    @Column(name = "created_by", nullable = false, length = 36, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private long version;

    protected Scene() {
    }

    /**
     * @param owner what the scene belongs to (a project)
     * @param realm the outermost place it sits in (the project's workspace)
     */
    public Scene(String id, ParallaxOwner owner, ParallaxOwner realm, String title, String description,
                 String instrumentCode, String forkedFromSceneId, Integer forkedFromRevision, String createdBy,
                 Instant now) {
        this.id = id;
        this.ownerType = owner.type();
        this.ownerId = owner.id();
        this.realmType = realm.type();
        this.realmId = realm.id();
        this.title = title;
        this.description = description;
        this.instrumentCode = instrumentCode;
        this.forkedFromSceneId = forkedFromSceneId;
        this.forkedFromRevision = forkedFromRevision;
        this.createdBy = createdBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void describe(String title, String description, Instant now) {
        this.title = title;
        this.description = description;
        this.updatedAt = now;
    }

    public void advanceTo(int revisionNumber, Instant now) {
        this.headRevisionNumber = revisionNumber;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        this.archivedAt = now;
        this.updatedAt = now;
    }

    public void restore(Instant now) {
        this.archivedAt = null;
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

    public String getInstrumentCode() {
        return instrumentCode;
    }

    public int getHeadRevisionNumber() {
        return headRevisionNumber;
    }

    public String getForkedFromSceneId() {
        return forkedFromSceneId;
    }

    public Integer getForkedFromRevision() {
        return forkedFromRevision;
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

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public long getVersion() {
        return version;
    }
}
