package org.jmouse.parallax.store.computation;

import jakarta.annotation.PreDestroy;
import org.jmouse.parallax.jpa.computation.ComputationJob;
import org.jmouse.parallax.jpa.computation.JobQueue;
import org.jmouse.parallax.event.JobFailed;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Takes queued jobs and runs them. A job is claimed under a lease with FOR UPDATE SKIP LOCKED — MySQL 8 and
 * PostgreSQL both have it — so several processes share one queue without taking the same job twice. A running job
 * renews its lease while it reports progress; a job whose lease ran out belongs to a worker that vanished and is
 * tried again, up to its attempts, or failed.
 *
 * <p>{@link ComputationSchedule} calls {@link #poll()} every poll interval and {@link #maintain()} every thirty
 * seconds; with no worker threads a poll takes nothing, so the process answers the API and never computes in the
 * background.</p>
 */
public class ComputationWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComputationWorker.class);
    private static final long PROGRESS_INTERVAL_MILLIS = 1000;

    private final Computing computing;
    private final JobQueue jobs;
    private final TransactionTemplate transactions;
    private final ComputationProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final ExecutorService executor;
    private final Semaphore slots;
    private final AtomicInteger running = new AtomicInteger();
    private final String identifier;
    private final Instant startedAt;

    public ComputationWorker(Computing computing, JobQueue jobs, TransactionTemplate transactions,
                             ComputationProperties properties, ApplicationEventPublisher events, Clock clock) {
        this.computing = computing;
        this.jobs = jobs;
        this.transactions = transactions;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
        this.identifier = ManagementFactory.getRuntimeMXBean().getName();
        this.startedAt = clock.instant();
        int threads = Math.max(1, properties.workerThreads());
        this.executor = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "parallax-computation");
            thread.setDaemon(true);
            return thread;
        });
        this.slots = new Semaphore(properties.workerThreads());
    }

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }

    public void poll() {
        while (properties.workerThreads() > 0 && slots.tryAcquire()) {
            Optional<String> claimed = claim();
            if (claimed.isEmpty()) {
                slots.release();
                return;
            }
            running.incrementAndGet();
            executor.submit(() -> {
                try {
                    execute(claimed.get());
                } finally {
                    running.decrementAndGet();
                    slots.release();
                }
            });
        }
    }

    private Optional<String> claim() {
        return transactions.execute(status -> {
            Instant now = clock.instant();
            return jobs.claimNext(identifier, now, now.plus(properties.lease()));
        });
    }

    private void execute(String jobId) {
        try {
            ComputationRequest request = computing.requestOf(jobs.requestOf(jobId));
            Computing.Prepared prepared = transactions.execute(status -> computing.prepare(request, false));
            ComputationViews.Ready ready = computing.run(prepared, new JobProgress(jobId));
            finish(jobId, ComputationJob.Status.SUCCEEDED, null, null, ready.datasetId());
        } catch (ComputationProgress.ComputationCancelled cancelled) {
            finish(jobId, ComputationJob.Status.CANCELLED, ComputationJob.FailureReason.CANCELLED, "Cancelled while running.", null);
        } catch (ParallaxProblem.LimitExceeded limit) {
            finish(jobId, ComputationJob.Status.FAILED, ComputationJob.FailureReason.LIMIT_EXCEEDED, limit.getMessage(), null);
        } catch (ParallaxProblem | IllegalArgumentException invalid) {
            finish(jobId, ComputationJob.Status.FAILED, ComputationJob.FailureReason.INVALID_REQUEST, invalid.getMessage(), null);
        } catch (RuntimeException failure) {
            LOGGER.warn("Computation job {} failed", jobId, failure);
            finish(jobId, ComputationJob.Status.FAILED, ComputationJob.FailureReason.COMPUTATION_ERROR,
                    failure.getClass().getSimpleName() + ": " + failure.getMessage(), null);
            events.publishEvent(new JobFailed(jobId, failure.getClass().getSimpleName()));
        }
    }

    private void finish(String jobId, ComputationJob.Status status, ComputationJob.FailureReason reason, String detail,
                        String datasetId) {
        transactions.executeWithoutResult(transaction -> jobs.finish(jobId, identifier, status, reason, detail, datasetId,
                clock.instant()));
    }

    /** Recovers jobs whose worker stopped renewing its lease, and records that this worker is alive. */
    public void maintain() {
        Instant now = clock.instant();
        transactions.executeWithoutResult(status -> {
            int requeued = jobs.requeueExpiredLeases(now);
            int failed = jobs.failExpiredLeases(now);
            if (requeued + failed > 0) {
                LOGGER.info("Recovered {} job(s) of a lost worker, failed {}", requeued, failed);
            }
            jobs.recordHeartbeat(identifier, startedAt, now, running.get());
        });
    }

    public String identifier() {
        return identifier;
    }

    public int running() {
        return running.get();
    }

    /** Writes progress at most once a second, renews the lease with it, and turns a cancel request into a stop. */
    private final class JobProgress implements ComputationProgress {

        private final String jobId;
        private long lastWrite;
        private boolean cancelled;

        JobProgress(String jobId) {
            this.jobId = jobId;
        }

        @Override
        public void report(double fraction, String message) {
            long now = System.currentTimeMillis();
            if (now - lastWrite < PROGRESS_INTERVAL_MILLIS) {
                return;
            }
            lastWrite = now;
            Instant instant = clock.instant();
            transactions.executeWithoutResult(status -> jobs.reportProgress(jobId, identifier, fraction, message,
                    instant.plus(properties.lease())));
            cancelled = jobs.isCancelRequested(jobId);
        }

        @Override
        public boolean cancelled() {
            return cancelled;
        }
    }
}
