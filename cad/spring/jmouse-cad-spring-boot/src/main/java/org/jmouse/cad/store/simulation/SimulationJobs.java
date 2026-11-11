package org.jmouse.cad.store.simulation;

import org.jmouse.cad.store.CadProperties;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.cad.store.simulation.SimulationViews.Job;
import org.jmouse.cad.store.simulation.SimulationViews.JobState;
import org.jmouse.cad.store.simulation.SimulationViews.SimulationResult;
import org.jmouse.numerics.ComputationProgress;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * The runs too large to answer at once, each run on one of {@code jmouse.cad.simulation.workers} threads and kept, with
 * its result, for {@code job-retention} after it ends — for the reader who started it, and no one else.
 *
 * <p>⚠️ Kept in this process, not in the database: a job lives as long as the application, and a restart forgets the
 * ones in flight. Enough for one instance; a second would want a shared queue, which nothing asks for yet.</p>
 *
 * <p>A job is handed only what a run needs — the circuit already elaborated, inside the request that had its
 * transaction —, so no thread here touches the catalogue.</p>
 */
public class SimulationJobs implements AutoCloseable {

    private final ExecutorService        workers;
    private final Map<String, JobRecord> jobs = new ConcurrentHashMap<>();
    private final Duration               retention;
    private final Clock                  clock;

    public SimulationJobs(CadProperties properties, Clock clock) {
        this.workers = Executors.newFixedThreadPool(properties.simulation().workers(), work -> {
            Thread thread = new Thread(work, "cad-simulation");
            thread.setDaemon(true);
            return thread;
        });
        this.retention = properties.simulation().jobRetention();
        this.clock = clock;
    }

    /**
     * Queues a run for an actor.
     *
     * @param run the run, reporting its progress and stopping when it is cancelled
     */
    public Job submit(String actor, Function<ComputationProgress, SimulationResult> run) {
        forgetExpired();
        JobRecord job = new JobRecord(UUID.randomUUID().toString(), actor, clock.instant());
        jobs.put(job.id, job);
        job.future = workers.submit(() -> job.run(run, clock));
        return job.view();
    }

    /** Every job kept, whoever started it, newest first: what an installation's activity lists. */
    public List<Job> all() {
        forgetExpired();
        return jobs.values().stream().sorted(Comparator.comparing((JobRecord job) -> job.queuedAt).reversed())
                .map(JobRecord::view).toList();
    }

    /** Where a job stands, for the actor who started it. */
    public Job job(String id, String actor) {
        return require(id, actor).view();
    }

    /** A finished job's result; refused while it runs, or when it failed or was cancelled. */
    public SimulationResult result(String id, String actor) {
        JobRecord job = require(id, actor);
        return switch (job.state) {
            case DONE -> job.result;
            case QUEUED, RUNNING -> throw CadProblem.conflict("The job is still " + job.state.name().toLowerCase() + ".");
            case FAILED -> throw new CadProblem.Unprocessable("simulation-failed", job.failure);
            case CANCELLED -> throw CadProblem.conflict("The job was cancelled.");
        };
    }

    /** Stops a job that has not ended; one that has is answered as it stands. */
    public Job cancel(String id, String actor) {
        JobRecord job = require(id, actor);
        job.cancelled = true;
        if (job.state == JobState.QUEUED && job.future != null && job.future.cancel(false)) {
            job.end(JobState.CANCELLED, null, clock);
        }
        return job.view();
    }

    @Override
    public void close() {
        workers.shutdownNow();
    }

    private JobRecord require(String id, String actor) {
        forgetExpired();
        JobRecord job = jobs.get(id);
        if (job == null || !job.actor.equals(actor)) {
            throw CadProblem.notFound("The job");
        }
        return job;
    }

    private void forgetExpired() {
        Instant oldest = clock.instant().minus(retention);
        jobs.values().removeIf(job -> job.finishedAt != null && job.finishedAt.isBefore(oldest));
    }

    /** One job as it changes: written by its worker, read by requests. */
    private static final class JobRecord {

        private final String id;
        private final String actor;
        private final Instant queuedAt;

        private volatile JobState         state = JobState.QUEUED;
        private volatile double           progress;
        private volatile boolean          cancelled;
        private volatile Instant          startedAt;
        private volatile Instant          finishedAt;
        private volatile String           failure;
        private volatile SimulationResult result;
        private volatile Future<?>        future;

        private JobRecord(String id, String actor, Instant queuedAt) {
            this.id = id;
            this.actor = actor;
            this.queuedAt = queuedAt;
        }

        private void run(Function<ComputationProgress, SimulationResult> run, Clock clock) {
            startedAt = clock.instant();
            state = JobState.RUNNING;
            try {
                SimulationResult answered = run.apply(new ComputationProgress() {
                    @Override
                    public void report(double fraction, String message) {
                        progress = fraction;
                    }

                    @Override
                    public boolean cancelled() {
                        return cancelled;
                    }
                });
                if (cancelled) {
                    end(JobState.CANCELLED, null, clock);
                } else {
                    result = answered;
                    progress = 1;
                    end(JobState.DONE, null, clock);
                }
            } catch (CadProblem refused) {
                end(cancelled ? JobState.CANCELLED : JobState.FAILED, refused.getMessage(), clock);
            } catch (RuntimeException failed) {
                end(cancelled ? JobState.CANCELLED : JobState.FAILED, failed.getMessage(), clock);
            }
        }

        private void end(JobState ended, String why, Clock clock) {
            failure = why;
            finishedAt = clock.instant();
            state = ended;
        }

        private Job view() {
            return new Job(id, state, progress, queuedAt, startedAt, finishedAt, failure);
        }
    }
}
