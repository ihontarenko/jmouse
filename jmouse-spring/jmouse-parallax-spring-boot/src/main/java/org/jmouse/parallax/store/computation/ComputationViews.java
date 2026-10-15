package org.jmouse.parallax.store.computation;

import com.fasterxml.jackson.annotation.JsonRawValue;
import org.jmouse.parallax.jpa.computation.ComputationJob;
import org.jmouse.parallax.jpa.computation.JobPlacement;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.owner.ParallaxOwner;

import java.time.Instant;
import java.util.Optional;

public final class ComputationViews {

    private ComputationViews() {
    }

    /** What a computation request is answered with: the dataset now, or the job that will produce it. */
    public sealed interface ComputationAnswer permits Ready, Queued {
    }

    /** The dataset is embedded as stored, so a cache hit is answered without parsing it back. */
    public record Ready(String datasetId, String fingerprint, RepresentationKind representation, String format,
                        boolean cached, long computeMillis, long elementCount,
                        @JsonRawValue String dataset) implements ComputationAnswer {
    }

    public record Queued(JobView job) implements ComputationAnswer {
    }

    public record JobView(String id, ParallaxOwner realm, ParallaxOwner owner, String sceneId, String objectId,
                          String requestedBy, RepresentationKind representation, ComputationJob.Status status,
                          double progress, String progressMessage, int attempts, int maximumAttempts,
                          boolean cancelRequested, ComputationJob.FailureReason failureReason, String failureDetail,
                          String datasetId, Instant queuedAt, Instant startedAt, Instant finishedAt) {

        /** {@code realm} and {@code owner} are where the job was placed; both null for the catalogue's queue. */
        static JobView of(ComputationJob job) {
            Optional<JobPlacement> placement = job.placement();
            return new JobView(job.getId(), placement.map(JobPlacement::realm).orElse(null),
                    placement.map(JobPlacement::owner).orElse(null), job.getSceneId(), job.getObjectId(),
                    job.getRequestedBy(), job.getRepresentation(), job.getStatus(), job.getProgress(), job.getProgressMessage(),
                    job.getAttempts(), job.getMaximumAttempts(), job.isCancelRequested(), job.getFailureReason(),
                    job.getFailureDetail(), job.getDatasetId(), job.getQueuedAt(), job.getStartedAt(), job.getFinishedAt());
        }
    }

    public record CacheStatistics(long datasets, long bytes, long queued, long running, long failed) {
    }

    /**
     * What the server is doing now, for the activity indicator in the header: the catalogue warm-up, the job queue and
     * the circuits running live.
     */
    public record Activity(CatalogueWarmup.WarmupStatus warmup, long queued, long running, long failed, int liveCircuits) {
    }
}
