package org.jmouse.parallax.jpa.scene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** One saved state of a scene. Never changed once written: restoring an old one writes a new revision. */
@Entity(name = "SceneRevision")
@Table(name = "scene_revisions")
public class SceneRevision {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "scene_id", nullable = false, length = 36, updatable = false)
    private String sceneId;

    @Column(name = "revision_number", nullable = false, updatable = false)
    private int revisionNumber;

    @Column(name = "format_version", nullable = false, updatable = false)
    private int formatVersion;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(length = Length.LONG32, nullable = false, updatable = false)
    private String document;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "document_hash", nullable = false, length = 64, updatable = false)
    private String documentHash;

    @Column(length = 500, updatable = false)
    private String message;

    @Column(name = "author_id", nullable = false, length = 36, updatable = false)
    private String authorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SceneRevision() {
    }

    public SceneRevision(String id, String sceneId, int revisionNumber, int formatVersion, String document,
                         String documentHash, String message, String authorId, Instant now) {
        this.id = id;
        this.sceneId = sceneId;
        this.revisionNumber = revisionNumber;
        this.formatVersion = formatVersion;
        this.document = document;
        this.documentHash = documentHash;
        this.message = message;
        this.authorId = authorId;
        this.createdAt = now;
    }

    public String getId() {
        return id;
    }

    public String getSceneId() {
        return sceneId;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public String getDocument() {
        return document;
    }

    public String getDocumentHash() {
        return documentHash;
    }

    public String getMessage() {
        return message;
    }

    public String getAuthorId() {
        return authorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
