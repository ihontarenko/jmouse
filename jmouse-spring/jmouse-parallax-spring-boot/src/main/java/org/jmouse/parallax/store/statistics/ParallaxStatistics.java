package org.jmouse.parallax.store.statistics;

import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.jpa.catalogue.ObjectStore;
import org.jmouse.parallax.jpa.computation.ComputationJob;
import org.jmouse.parallax.jpa.computation.DatasetCache;
import org.jmouse.parallax.jpa.computation.JobQueue;
import org.jmouse.parallax.jpa.scene.SceneStore;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 📊 What the machinery holds and how it is doing: the workers and their heartbeats, the queue, the cache, and what the
 * catalogue and the owners keep.
 *
 * <p>A host's system screen reads this rather than counting Parallax's tables itself — the tables are the library's,
 * and a host that queried them by name would break on the first migration that renamed one.</p>
 */
public class ParallaxStatistics {

    /** A worker silent for longer than this is reported as gone. */
    private static final Duration WORKER_SILENCE = Duration.ofMinutes(2);

    /** How far back a failed job still counts as a recent failure. */
    private static final Duration FAILURE_WINDOW = Duration.ofDays(1);

    private final JobQueue          jobs;
    private final DatasetCache      datasets;
    private final ObjectStore       objects;
    private final SceneStore        scenes;
    private final ParallaxAuthority authority;
    private final Clock             clock;

    /**
     * 🏗️ Statistics over the library's stores.
     *
     * @param jobs      the job queue
     * @param datasets  the dataset cache
     * @param objects   the objects
     * @param scenes    the scenes
     * @param authority who may read them
     * @param clock     the clock
     */
    public ParallaxStatistics(JobQueue jobs, DatasetCache datasets, ObjectStore objects, SceneStore scenes,
                              ParallaxAuthority authority, Clock clock) {
        this.jobs      = jobs;
        this.datasets  = datasets;
        this.objects   = objects;
        this.scenes    = scenes;
        this.authority = authority;
        this.clock     = clock;
    }

    /**
     * 👷 One worker as its last heartbeat left it.
     *
     * @param identifier  the worker
     * @param startedAt   when it started
     * @param lastSeenAt  when it last reported
     * @param runningJobs how many jobs it was running then
     * @param alive       whether it reported recently enough to be counted as running
     */
    public record WorkerView(String identifier, Instant startedAt, Instant lastSeenAt, int runningJobs, boolean alive) {
    }

    /**
     * 📊 The machinery at one moment.
     *
     * @param workers           every worker that ever reported, the most recently seen first
     * @param scenes            the scenes, archived ones included
     * @param catalogueObjects  the shared catalogue's objects, every status
     * @param ownedObjects      the objects that belong to an owner
     * @param jobsQueued        the jobs waiting for a worker
     * @param jobsRunning       the jobs a worker holds
     * @param jobsFailedLastDay the jobs that failed in the last day
     * @param cachedDatasets    the datasets in the cache
     * @param cachedBytes       their payload, in bytes
     * @param checkedAt         when this was read
     */
    public record Statistics(List<WorkerView> workers, long scenes, long catalogueObjects, long ownedObjects,
                             long jobsQueued, long jobsRunning, long jobsFailedLastDay, long cachedDatasets,
                             long cachedBytes, Instant checkedAt) {
    }

    /**
     * 📊 The machinery now.
     *
     * @return the statistics
     */
    @Transactional(readOnly = true)
    public Statistics current() {
        authority.requireGlobally(ParallaxCapability.MANAGEMENT_READ);

        Instant             now     = clock.instant();
        DatasetCache.Totals cached  = datasets.totals();
        List<WorkerView>    workers = jobs.heartbeats().stream()
                .map(heartbeat -> new WorkerView(heartbeat.workerIdentifier(), heartbeat.startedAt(),
                        heartbeat.lastSeenAt(), heartbeat.runningJobs(),
                        heartbeat.lastSeenAt().isAfter(now.minus(WORKER_SILENCE))))
                .toList();

        return new Statistics(workers, scenes.count(), objects.countByOwnership(false), objects.countByOwnership(true),
                jobs.countByStatus(ComputationJob.Status.QUEUED), jobs.countByStatus(ComputationJob.Status.RUNNING),
                jobs.countFailedSince(now.minus(FAILURE_WINDOW)), cached.datasets(), cached.bytes(), now);
    }
}
