package org.jmouse.parallax.jpa.computation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jmouse.parallax.jpa.NativeValues;
import org.jmouse.parallax.jpa.StoredPage;
import org.jmouse.parallax.owner.ParallaxOwner;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * ⏳ The job queue: the jobs as people list them, and the statements a worker claims, renews and finishes them by.
 *
 * <h3>⚠️ The worker's statements are native, and deliberately</h3>
 *
 * <p>A job is claimed with {@code SELECT … FOR UPDATE SKIP LOCKED} — MySQL 8 and PostgreSQL both have it — so several
 * processes share one queue without taking the same job twice. The worker never loads a job as an entity: it updates
 * the row in place, bumping {@code version} itself so that a person's change to the same job made through the entity
 * still meets its optimistic lock.</p>
 *
 * <p>⚠️ Every query reads a list, never a stream: outside a transaction the shared proxy closes its
 * {@code EntityManager} as the call returns, and a stream read after that finds its result set closed.</p>
 *
 * <p>Every write here must run inside the caller's transaction, and the claim inside the same one as its update.</p>
 */
public class JobQueue {

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public JobQueue(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // ── As people see it ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * 🔎 One job.
     *
     * @param jobId the job
     * @return the job, or empty
     */
    public Optional<ComputationJob> find(String jobId) {
        return Optional.ofNullable(entityManager.find(ComputationJob.class, jobId));
    }

    /**
     * ➕ Queue a new job.
     *
     * @param job the job
     * @return the same job, now managed
     */
    public ComputationJob add(ComputationJob job) {
        entityManager.persist(job);

        return job;
    }

    /**
     * 📂 A realm's jobs, newest first.
     *
     * @param realm the realm (a workspace)
     * @param page  the page number, from zero
     * @param size  the page size
     * @return the page
     */
    public StoredPage<ComputationJob> inRealm(ParallaxOwner realm, int page, int size) {
        String conditions = " where job.realmType = :type and job.realmId = :id";
        TypedQuery<ComputationJob> rows = entityManager.createQuery(
                "select job from ComputationJob job" + conditions + " order by job.queuedAt desc", ComputationJob.class);
        TypedQuery<Long> count = entityManager.createQuery(
                "select count(job) from ComputationJob job" + conditions, Long.class);

        for (TypedQuery<?> query : List.of(rows, count)) {
            query.setParameter("type", realm.type()).setParameter("id", realm.id());
        }

        return StoredPage.of(rows, count, page, size);
    }

    /**
     * 📚 Every job, newest first.
     *
     * @param page the page number, from zero
     * @param size the page size
     * @return the page
     */
    public StoredPage<ComputationJob> all(int page, int size) {
        return StoredPage.of(
                entityManager.createQuery("select job from ComputationJob job order by job.queuedAt desc",
                                          ComputationJob.class),
                entityManager.createQuery("select count(job) from ComputationJob job", Long.class),
                page, size);
    }

    /**
     * 📚 The jobs in some statuses, newest first.
     *
     * @param statuses the statuses
     * @param page     the page number, from zero
     * @param size     the page size
     * @return the page
     */
    public StoredPage<ComputationJob> withStatus(Collection<ComputationJob.Status> statuses, int page, int size) {
        String conditions = " where job.status in :statuses";
        TypedQuery<ComputationJob> rows = entityManager.createQuery(
                "select job from ComputationJob job" + conditions + " order by job.queuedAt desc", ComputationJob.class);
        TypedQuery<Long> count = entityManager.createQuery(
                "select count(job) from ComputationJob job" + conditions, Long.class);

        for (TypedQuery<?> query : List.of(rows, count)) {
            query.setParameter("statuses", statuses);
        }

        return StoredPage.of(rows, count, page, size);
    }

    /**
     * 🔎 A job computing a fingerprint in one of some statuses, so the same work is not queued twice.
     *
     * @param fingerprint the fingerprint
     * @param statuses    the statuses that count
     * @return such a job, or empty
     */
    public Optional<ComputationJob> findByFingerprint(String fingerprint, Collection<ComputationJob.Status> statuses) {
        return entityManager.createQuery(
                        "select job from ComputationJob job where job.fingerprint = :fingerprint and job.status in :statuses",
                        ComputationJob.class)
                .setParameter("fingerprint", fingerprint)
                .setParameter("statuses", statuses)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

    /**
     * 🔢 How many jobs are in one status.
     *
     * @param status the status
     * @return the count
     */
    public long countByStatus(ComputationJob.Status status) {
        return entityManager.createQuery("select count(job) from ComputationJob job where job.status = :status",
                                         Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    /**
     * 🔢 How many jobs failed since a moment.
     *
     * @param since the moment
     * @return the count
     */
    public long countFailedSince(Instant since) {
        return entityManager.createQuery("""
                        select count(job) from ComputationJob job
                        where job.status = :status and job.finishedAt > :since
                        """, Long.class)
                .setParameter("status", ComputationJob.Status.FAILED)
                .setParameter("since", since)
                .getSingleResult();
    }

    /**
     * 🔢 How many jobs of the catalogue's public queue — the ones that belong to nobody — are in some statuses.
     *
     * @param statuses the statuses
     * @return the count
     */
    public long countCatalogueQueue(Collection<ComputationJob.Status> statuses) {
        return entityManager.createQuery(
                        "select count(job) from ComputationJob job where job.ownerType is null and job.status in :statuses",
                        Long.class)
                .setParameter("statuses", statuses)
                .getSingleResult();
    }

    // ── As a worker runs it ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * 🙋 Claim the most urgent queued job for a worker, under a lease.
     *
     * <p>⚠️ Must run inside a transaction: the row stays locked against other workers until it commits.</p>
     *
     * @param workerIdentifier the worker
     * @param now              when
     * @param leaseExpiresAt   when the lease runs out unless renewed
     * @return the claimed job, or empty when nothing is queued
     */
    public Optional<String> claimNext(String workerIdentifier, Instant now, Instant leaseExpiresAt) {
        List<?> next = entityManager.createNativeQuery("""
                        SELECT id FROM computation_jobs WHERE status = 'QUEUED'
                        ORDER BY priority DESC, queued_at LIMIT 1 FOR UPDATE SKIP LOCKED
                        """)
                .getResultList();

        Optional<String> claimed = next.stream().findFirst().map(NativeValues::stringOf);

        claimed.ifPresent(jobId -> entityManager.createNativeQuery("""
                        UPDATE computation_jobs SET status = 'RUNNING', worker_identifier = :worker,
                               lease_expires_at = :leaseExpiresAt, attempts = attempts + 1, started_at = :now,
                               progress = 0, version = version + 1
                        WHERE id = :jobId
                        """)
                .setParameter("worker", workerIdentifier)
                .setParameter("leaseExpiresAt", leaseExpiresAt)
                .setParameter("now", now)
                .setParameter("jobId", jobId)
                .executeUpdate());

        return claimed;
    }

    /**
     * 📄 The stored request of a job, as the JSON it was queued with.
     *
     * @param jobId the job
     * @return the request
     */
    public String requestOf(String jobId) {
        return NativeValues.stringOf(entityManager.createNativeQuery(
                        "SELECT request FROM computation_jobs WHERE id = :jobId")
                .setParameter("jobId", jobId)
                .getSingleResult());
    }

    /**
     * 📈 Record a running job's progress and renew its lease — only while this worker still holds it.
     *
     * @param jobId            the job
     * @param workerIdentifier the worker
     * @param fraction         how far it is, 0…1
     * @param message          what it is doing, or {@code null}; cut to the column's length
     * @param leaseExpiresAt   the renewed lease
     */
    public void reportProgress(String jobId, String workerIdentifier, double fraction, String message,
                               Instant leaseExpiresAt) {
        entityManager.createNativeQuery("""
                        UPDATE computation_jobs SET progress = :fraction, progress_message = :message,
                               lease_expires_at = :leaseExpiresAt
                        WHERE id = :jobId AND worker_identifier = :worker
                        """)
                .setParameter("fraction", fraction)
                .setParameter("message", NativeValues.truncated(message, ComputationJob.PROGRESS_MESSAGE_LENGTH))
                .setParameter("leaseExpiresAt", leaseExpiresAt)
                .setParameter("jobId", jobId)
                .setParameter("worker", workerIdentifier)
                .executeUpdate();
    }

    /**
     * 🛑 Whether somebody asked a job to stop. A job that is gone counts as asked.
     *
     * @param jobId the job
     * @return whether it should stop
     */
    public boolean isCancelRequested(String jobId) {
        List<?> rows = entityManager.createNativeQuery("SELECT cancel_requested FROM computation_jobs WHERE id = :jobId")
                .setParameter("jobId", jobId)
                .getResultList();

        return rows.stream().findFirst().map(NativeValues::booleanOf).orElse(true);
    }

    /**
     * 🏁 Finish a job — only while this worker still holds it.
     *
     * @param jobId            the job
     * @param workerIdentifier the worker
     * @param status           how it ended
     * @param reason           why it failed, or {@code null}
     * @param detail           the failure's words, or {@code null}; cut to the column's length
     * @param datasetId        the dataset it produced, or {@code null}
     * @param now              when
     */
    public void finish(String jobId, String workerIdentifier, ComputationJob.Status status,
                       ComputationJob.FailureReason reason, String detail, String datasetId, Instant now) {
        entityManager.createNativeQuery("""
                        UPDATE computation_jobs SET status = :status, failure_reason = :reason, failure_detail = :detail,
                               dataset_id = :datasetId,
                               progress = CASE WHEN :status = 'SUCCEEDED' THEN 1 ELSE progress END,
                               finished_at = :now, lease_expires_at = NULL, version = version + 1
                        WHERE id = :jobId AND worker_identifier = :worker
                        """)
                .setParameter("status", status.name())
                .setParameter("reason", reason == null ? null : reason.name())
                .setParameter("detail", NativeValues.truncated(detail, ComputationJob.FAILURE_DETAIL_LENGTH))
                .setParameter("datasetId", datasetId)
                .setParameter("now", now)
                .setParameter("jobId", jobId)
                .setParameter("worker", workerIdentifier)
                .executeUpdate();
    }

    /**
     * 🔁 Put back in the queue every running job whose lease ran out and that has attempts left.
     *
     * @param now when
     * @return how many were requeued
     */
    public int requeueExpiredLeases(Instant now) {
        return entityManager.createNativeQuery("""
                        UPDATE computation_jobs SET status = 'QUEUED', worker_identifier = NULL, lease_expires_at = NULL,
                               version = version + 1
                        WHERE status = 'RUNNING' AND lease_expires_at < :now AND attempts < maximum_attempts
                        """)
                .setParameter("now", now)
                .executeUpdate();
    }

    /**
     * ❌ Fail every running job whose lease ran out and that has no attempts left.
     *
     * @param now when
     * @return how many were failed
     */
    public int failExpiredLeases(Instant now) {
        return entityManager.createNativeQuery("""
                        UPDATE computation_jobs SET status = 'FAILED', failure_reason = 'WORKER_LOST',
                               failure_detail = 'The worker running it stopped answering.', finished_at = :now,
                               lease_expires_at = NULL, version = version + 1
                        WHERE status = 'RUNNING' AND lease_expires_at < :now AND attempts >= maximum_attempts
                        """)
                .setParameter("now", now)
                .executeUpdate();
    }

    /**
     * 💓 Record that a worker is alive and how many jobs it is running.
     *
     * @param workerIdentifier the worker
     * @param startedAt        when the worker started
     * @param now              when
     * @param runningJobs      how many jobs it runs now
     */
    public void recordHeartbeat(String workerIdentifier, Instant startedAt, Instant now, int runningJobs) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE worker_heartbeats SET last_seen_at = :now, running_jobs = :running
                        WHERE worker_identifier = :worker
                        """)
                .setParameter("now", now)
                .setParameter("running", runningJobs)
                .setParameter("worker", workerIdentifier)
                .executeUpdate();

        if (updated == 0) {
            entityManager.createNativeQuery("""
                            INSERT INTO worker_heartbeats (worker_identifier, started_at, last_seen_at, running_jobs)
                            VALUES (:worker, :startedAt, :now, :running)
                            """)
                    .setParameter("worker", workerIdentifier)
                    .setParameter("startedAt", startedAt)
                    .setParameter("now", now)
                    .setParameter("running", runningJobs)
                    .executeUpdate();
        }
    }

    /**
     * 💓 Every worker that ever reported, the most recently seen first.
     *
     * @return the heartbeats
     */
    public List<WorkerHeartbeat> heartbeats() {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT worker_identifier, started_at, last_seen_at, running_jobs
                        FROM worker_heartbeats ORDER BY last_seen_at DESC
                        """)
                .getResultList();

        return rows.stream()
                .map(row -> (Object[]) row)
                .map(columns -> new WorkerHeartbeat(NativeValues.stringOf(columns[0]),
                        NativeValues.instantOf(columns[1]), NativeValues.instantOf(columns[2]),
                        NativeValues.intOf(columns[3])))
                .toList();
    }

    /**
     * 💓 What a worker last said about itself.
     *
     * @param workerIdentifier the worker
     * @param startedAt        when it started
     * @param lastSeenAt       when it last reported
     * @param runningJobs      how many jobs it was running then
     */
    public record WorkerHeartbeat(String workerIdentifier, Instant startedAt, Instant lastSeenAt, int runningJobs) {
    }
}
