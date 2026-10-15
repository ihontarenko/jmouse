package org.jmouse.parallax.jpa.scene;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** A named moment of a revision: where the timeline stood, the camera, the parameter values. */
@Entity(name = "SceneSnapshot")
@Table(name = "scene_snapshots")
public class SceneSnapshot {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "scene_id", nullable = false, length = 36, updatable = false)
    private String sceneId;

    @Column(name = "revision_number", nullable = false, updatable = false)
    private int revisionNumber;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "timeline_position", nullable = false)
    private double timelinePosition;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(length = Length.LONG32, nullable = false)
    private String camera;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "parameter_values", length = Length.LONG32, nullable = false)
    private String parameterValues;

    @Column(name = "created_by", nullable = false, length = 36, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SceneSnapshot() {
    }

    public SceneSnapshot(String id, String sceneId, int revisionNumber, String title, double timelinePosition,
                         String camera, String parameterValues, String createdBy, Instant now) {
        this.id = id;
        this.sceneId = sceneId;
        this.revisionNumber = revisionNumber;
        this.title = title;
        this.timelinePosition = timelinePosition;
        this.camera = camera;
        this.parameterValues = parameterValues;
        this.createdBy = createdBy;
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

    public String getTitle() {
        return title;
    }

    public double getTimelinePosition() {
        return timelinePosition;
    }

    public String getCamera() {
        return camera;
    }

    public String getParameterValues() {
        return parameterValues;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
