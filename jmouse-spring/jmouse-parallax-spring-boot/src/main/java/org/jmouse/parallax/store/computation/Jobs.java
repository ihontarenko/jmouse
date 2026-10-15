package org.jmouse.parallax.store.computation;

import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.owner.ParallaxOwner;
import org.jmouse.parallax.authority.ParallaxPlace;
import org.jmouse.parallax.authority.ParallaxPlaces;
import org.jmouse.parallax.jpa.computation.ComputationJob;
import org.jmouse.parallax.jpa.computation.JobQueue;
import org.jmouse.parallax.jpa.computation.DatasetCache;
import org.jmouse.parallax.store.computation.ComputationViews.CacheStatistics;
import org.jmouse.parallax.store.computation.ComputationViews.JobView;
import org.jmouse.parallax.store.computation.ComputationViews.Ready;
import org.jmouse.parallax.limits.ParallaxLimits;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.store.support.ParallaxPage;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.EnumSet;

/** The job queue as people see it: a workspace's jobs, one job and its dataset, cancel and retry; the cache's upkeep. */
public class Jobs {

    private final JobQueue jobs;
    private final DatasetCache datasets;
    private final Computing computing;
    private final ParallaxAuthority authority;
    private final ParallaxPlaces places;
    private final ParallaxLimits limits;
    private final Clock clock;

    public Jobs(JobQueue jobs, DatasetCache datasets, Computing computing,
                ParallaxAuthority authority, ParallaxPlaces places, ParallaxLimits limits, Clock clock) {
        this.places = places;
        this.jobs = jobs;
        this.datasets = datasets;
        this.computing = computing;
        this.authority = authority;
        this.limits = limits;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ParallaxPage<JobView> inRealm(ParallaxOwner realm, int page, int size) {
        authority.require(ParallaxCapability.JOB_READ, places.requireOwner(realm));
        ParallaxPage.requireBounds(page, size);
        return ParallaxPage.of(jobs.inRealm(realm, page, size), JobView::of);
    }

    @Transactional(readOnly = true)
    public ParallaxPage<JobView> all(ComputationJob.Status status, int page, int size) {
        authority.requireGlobally(ParallaxCapability.JOB_ADMINISTER);
        ParallaxPage.requireBounds(page, size);
        return ParallaxPage.of(status == null ? jobs.all(page, size)
                : jobs.withStatus(EnumSet.of(status), page, size), JobView::of);
    }

    @Transactional(readOnly = true)
    public JobView get(String jobId) {
        return JobView.of(requireReadable(jobId));
    }

    public Ready dataset(String jobId) {
        ComputationJob job = requireReadable(jobId);
        if (job.getDatasetId() == null) {
            throw ParallaxProblem.conflict("This job has no dataset yet: it is " + job.getStatus().name().toLowerCase() + ".");
        }
        return computing.dataset(job.getDatasetId());
    }

    @Transactional
    public JobView cancel(String jobId) {
        ComputationJob job = requireReadable(jobId);
        requireManageable(job, ParallaxCapability.JOB_CANCEL);
        if (!job.isFinished()) {
            job.requestCancel(clock.instant());
        }
        return JobView.of(job);
    }

    @Transactional
    public JobView retry(String jobId) {
        ComputationJob job = requireReadable(jobId);
        requireManageable(job, ParallaxCapability.JOB_CANCEL);
        if (job.getStatus() != ComputationJob.Status.FAILED && job.getStatus() != ComputationJob.Status.CANCELLED) {
            throw ParallaxProblem.conflict("Only a failed or cancelled job is retried.");
        }
        job.requeue(clock.instant());
        return JobView.of(job);
    }

    @Transactional(readOnly = true)
    public CacheStatistics statistics() {
        authority.requireGlobally(ParallaxCapability.MANAGEMENT_READ);
        DatasetCache.Totals totals = datasets.totals();
        return new CacheStatistics(totals.datasets(), totals.bytes(),
                jobs.countByStatus(ComputationJob.Status.QUEUED), jobs.countByStatus(ComputationJob.Status.RUNNING),
                jobs.countByStatus(ComputationJob.Status.FAILED));
    }

    /**
     * Datasets nobody has asked for within the retention go; whatever is asked for again is recomputed. Run nightly
     * by {@link ComputationSchedule}.
     */
    @Transactional
    public void forgetUnusedDatasets() {
        datasets.forgetUnusedSince(clock.instant().minus(limits.datasetRetention()));
    }

    private ComputationJob requireReadable(String jobId) {
        ComputationJob job = jobs.find(jobId).orElseThrow(() -> ParallaxProblem.notFound("The job"));
        // A catalogue job computes a published object: its progress and dataset are as public as the object.
        if (job.placement().isEmpty()) {
            return job;
        }
        if (!authority.permitsGlobally(ParallaxCapability.JOB_ADMINISTER)
                && !authority.permits(ParallaxCapability.JOB_READ, realmPlaceOf(job))) {
            throw ParallaxProblem.notFound("The job");
        }
        return job;
    }

    private void requireManageable(ComputationJob job, ParallaxCapability capability) {
        if (!authority.permitsGlobally(ParallaxCapability.JOB_ADMINISTER)) {
            if (job.placement().isEmpty()) {
                throw ParallaxProblem.refused("Only an administrator manages the catalogue's queue.");
            }
            authority.require(capability, realmPlaceOf(job));
        }
    }

    /** Where an owned job is decided: its realm (the workspace), as the host places it. */
    private ParallaxPlace realmPlaceOf(ComputationJob job) {
        return places.requireOwner(job.placement().orElseThrow().realm());
    }
}
