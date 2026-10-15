package org.jmouse.parallax.jpa.computation;

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
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.jpa.NativeValues;
import org.jmouse.parallax.owner.ParallaxOwner;

import java.time.Instant;
import java.util.Optional;

/**
 * ⏳ A heavy computation waiting for, or taken by, a worker.
 *
 * <p>Claimed under a lease (see {@link JobQueue}); a job of the catalogue's public queue has no placement at all.</p>
 */
@Entity(name = "ComputationJob")
@Table(name = "computation_jobs")
public class ComputationJob {

    public enum Status { QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED }

    public enum FailureReason { INVALID_REQUEST, COMPUTATION_ERROR, LIMIT_EXCEEDED, WORKER_LOST, CANCELLED }

    /** The length of {@code progress_message}. */
    public static final int PROGRESS_MESSAGE_LENGTH = 500;

    /** The length of {@code failure_detail}. */
    public static final int FAILURE_DETAIL_LENGTH = 2000;

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "owner_type", length = 64, updatable = false)
    private String ownerType;

    @Column(name = "owner_id", length = 64, updatable = false)
    private String ownerId;

    @Column(name = "realm_type", length = 64, updatable = false)
    private String realmType;

    @Column(name = "realm_id", length = 64, updatable = false)
    private String realmId;

    @Column(name = "scene_id", length = 36, updatable = false)
    private String sceneId;

    @Column(name = "object_id", length = 36, updatable = false)
    private String objectId;

    @Column(name = "requested_by", length = 36, updatable = false)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "computation_kind", nullable = false, length = 40, updatable = false)
    private RepresentationKind representation;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(length = Length.LONG32, nullable = false, updatable = false)
    private String request;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 64, updatable = false)
    private String fingerprint;

    @Column(nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(nullable = false)
    private double progress;

    @Column(name = "progress_message", length = PROGRESS_MESSAGE_LENGTH)
    private String progressMessage;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "maximum_attempts", nullable = false)
    private int maximumAttempts;

    @Column(name = "cancel_requested", nullable = false)
    private boolean cancelRequested;

    @Column(name = "worker_identifier", length = 160)
    private String workerIdentifier;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 40)
    private FailureReason failureReason;

    @Column(name = "failure_detail", length = FAILURE_DETAIL_LENGTH)
    private String failureDetail;

    @Column(name = "dataset_id", length = 36)
    private String datasetId;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Version
    private long version;

    protected ComputationJob() {
    }

    /**
     * @param placement what the job is computed for and the realm it counts against; null for the catalogue's
     *                  public queue
     */
    public ComputationJob(String id, JobPlacement placement, String sceneId, String objectId,
                          String requestedBy, RepresentationKind representation, String request, String fingerprint,
                          int priority, int maximumAttempts, Instant now) {
        this.id = id;
        if (placement != null) {
            this.ownerType = placement.owner().type();
            this.ownerId = placement.owner().id();
            this.realmType = placement.realm().type();
            this.realmId = placement.realm().id();
        }
        this.sceneId = sceneId;
        this.objectId = objectId;
        this.requestedBy = requestedBy;
        this.representation = representation;
        this.request = request;
        this.fingerprint = fingerprint;
        this.priority = priority;
        this.maximumAttempts = maximumAttempts;
        this.status = Status.QUEUED;
        this.queuedAt = now;
    }

    public boolean isFinished() {
        return status == Status.SUCCEEDED || status == Status.FAILED || status == Status.CANCELLED;
    }

    public void requestCancel(Instant now) {
        if (status == Status.QUEUED) {
            finish(Status.CANCELLED, FailureReason.CANCELLED, "Cancelled before it started.", now);
        } else if (status == Status.RUNNING) {
            cancelRequested = true;
        }
    }

    public void requeue(Instant now) {
        this.status = Status.QUEUED;
        this.progress = 0;
        this.progressMessage = null;
        this.cancelRequested = false;
        this.workerIdentifier = null;
        this.leaseExpiresAt = null;
        this.failureReason = null;
        this.failureDetail = null;
        this.finishedAt = null;
        this.queuedAt = now;
        this.attempts = 0;
    }

    public void succeed(String datasetId, Instant now) {
        this.datasetId = datasetId;
        this.progress = 1;
        finish(Status.SUCCEEDED, null, null, now);
    }

    public void finish(Status status, FailureReason reason, String detail, Instant now) {
        this.status = status;
        this.failureReason = reason;
        this.failureDetail = NativeValues.truncated(detail, FAILURE_DETAIL_LENGTH);
        this.finishedAt = now;
        this.leaseExpiresAt = null;
    }

    public String getId() {
        return id;
    }

    /** What the job was computed for and where it counts; empty for a job of the catalogue's public queue. */
    public Optional<JobPlacement> placement() {
        if (ownerType == null) {
            return Optional.empty();
        }
        return Optional.of(new JobPlacement(ParallaxOwner.of(ownerType, ownerId), ParallaxOwner.of(realmType, realmId)));
    }

    public String getSceneId() {
        return sceneId;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public RepresentationKind getRepresentation() {
        return representation;
    }

    public String getRequest() {
        return request;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public int getPriority() {
        return priority;
    }

    public Status getStatus() {
        return status;
    }

    public double getProgress() {
        return progress;
    }

    public String getProgressMessage() {
        return progressMessage;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    public String getWorkerIdentifier() {
        return workerIdentifier;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public FailureReason getFailureReason() {
        return failureReason;
    }

    public String getFailureDetail() {
        return failureDetail;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public long getVersion() {
        return version;
    }
}
