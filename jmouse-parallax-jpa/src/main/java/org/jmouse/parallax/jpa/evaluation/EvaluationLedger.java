package org.jmouse.parallax.jpa.evaluation;

import jakarta.persistence.EntityManager;
import org.jmouse.parallax.jpa.NativeValues;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 📒 The evaluation runs and what each check found, kept so a report can be read again.
 *
 * <p>Rows without entities: a run is written once, completed once, and read as a report. Every write must run inside
 * the caller's transaction; a run that writes as it goes commits each write on its own, so one broken object never
 * takes the rest of the report with it.</p>
 *
 * <p>It does not authorize anything.</p>
 */
public class EvaluationLedger {

    private static final String RUN_COLUMNS = """
            id, status, object_id, started_by, started_at, finished_at, total_checks, passed, warned, failed, skipped
            """;

    /** The length of {@code evaluation_results.detail}. */
    private static final int DETAIL_LENGTH = 2000;

    private final EntityManager entityManager;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     */
    public EvaluationLedger(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 📋 One run.
     *
     * @param id          the run
     * @param status      {@code RUNNING}, {@code COMPLETED} or {@code FAILED}
     * @param objectId    the one object it evaluated, or {@code null} for the catalogue
     * @param startedBy   who started it
     * @param startedAt   when
     * @param finishedAt  when it finished, or {@code null}
     * @param totalChecks how many checks ran
     * @param passed      how many passed
     * @param warned      how many warned
     * @param failed      how many failed
     * @param skipped     how many were skipped
     */
    public record Run(String id, String status, String objectId, String startedBy, Instant startedAt,
                      Instant finishedAt, int totalChecks, int passed, int warned, int failed, int skipped) {
    }

    /**
     * 🔬 What one check found on one object, with the object's slug and title for the report.
     *
     * @param objectId       the object
     * @param objectSlug     its slug
     * @param objectTitle    its title
     * @param checkCode      the check
     * @param outcome        {@code PASSED}, {@code WARNED}, {@code FAILED} or {@code SKIPPED}
     * @param measuredValue  what it measured, or {@code null}
     * @param tolerance      what it allowed, or {@code null}
     * @param detail         its words, or {@code null}
     * @param durationMillis how long it took
     */
    public record Result(String objectId, String objectSlug, String objectTitle, String checkCode, String outcome,
                         Double measuredValue, Double tolerance, String detail, long durationMillis) {
    }

    /**
     * ▶️ Record that a run started.
     *
     * @param runId     the run
     * @param objectId  the one object it evaluates, or {@code null} for the catalogue
     * @param startedBy who started it
     * @param startedAt when
     */
    public void startRun(String runId, String objectId, String startedBy, Instant startedAt) {
        entityManager.createNativeQuery("""
                        INSERT INTO evaluation_runs (id, status, object_id, started_by, started_at)
                        VALUES (:id, 'RUNNING', :objectId, :startedBy, :startedAt)
                        """)
                .setParameter("id", runId)
                .setParameter("objectId", objectId)
                .setParameter("startedBy", startedBy)
                .setParameter("startedAt", startedAt)
                .executeUpdate();
    }

    /**
     * 🔬 Record what one check found.
     *
     * @param resultId       the result's identifier
     * @param runId          the run
     * @param objectId       the object
     * @param checkCode      the check
     * @param outcome        the outcome's name
     * @param measuredValue  what it measured, or {@code null}
     * @param tolerance      what it allowed, or {@code null}
     * @param detail         its words, or {@code null}; cut to 2000 characters
     * @param durationMillis how long it took
     */
    public void recordResult(String resultId, String runId, String objectId, String checkCode, String outcome,
                             Double measuredValue, Double tolerance, String detail, long durationMillis) {
        entityManager.createNativeQuery("""
                        INSERT INTO evaluation_results (id, run_id, object_id, check_code, outcome, measured_value,
                                                        tolerance, detail, duration_millis)
                        VALUES (:id, :runId, :objectId, :checkCode, :outcome, :measuredValue, :tolerance, :detail,
                                :durationMillis)
                        """)
                .setParameter("id", resultId)
                .setParameter("runId", runId)
                .setParameter("objectId", objectId)
                .setParameter("checkCode", checkCode)
                .setParameter("outcome", outcome)
                .setParameter("measuredValue", measuredValue)
                .setParameter("tolerance", tolerance)
                .setParameter("detail", NativeValues.truncated(detail, DETAIL_LENGTH))
                .setParameter("durationMillis", durationMillis)
                .executeUpdate();
    }

    /**
     * 🏁 Record that a run completed, and its counts.
     *
     * @param runId      the run
     * @param finishedAt when
     * @param passed     how many passed
     * @param warned     how many warned
     * @param failed     how many failed
     * @param skipped    how many were skipped
     */
    public void completeRun(String runId, Instant finishedAt, int passed, int warned, int failed, int skipped) {
        entityManager.createNativeQuery("""
                        UPDATE evaluation_runs SET status = 'COMPLETED', finished_at = :finishedAt, total_checks = :total,
                               passed = :passed, warned = :warned, failed = :failed, skipped = :skipped
                        WHERE id = :id
                        """)
                .setParameter("finishedAt", finishedAt)
                .setParameter("total", passed + warned + failed + skipped)
                .setParameter("passed", passed)
                .setParameter("warned", warned)
                .setParameter("failed", failed)
                .setParameter("skipped", skipped)
                .setParameter("id", runId)
                .executeUpdate();
    }

    /**
     * 💥 Record that a run stopped before it completed; the results it recorded stay.
     *
     * @param runId      the run
     * @param finishedAt when
     */
    public void failRun(String runId, Instant finishedAt) {
        entityManager.createNativeQuery("""
                        UPDATE evaluation_runs SET status = 'FAILED', finished_at = :finishedAt
                        WHERE id = :id AND status = 'RUNNING'
                        """)
                .setParameter("finishedAt", finishedAt)
                .setParameter("id", runId)
                .executeUpdate();
    }

    /**
     * 📋 The newest runs.
     *
     * @param limit how many at most
     * @return the runs, newest first
     */
    public List<Run> runsNewestFirst(int limit) {
        List<?> rows = entityManager.createNativeQuery(
                        "SELECT " + RUN_COLUMNS + " FROM evaluation_runs ORDER BY started_at DESC")
                .setMaxResults(limit)
                .getResultList();

        return rows.stream().map(Object[].class::cast).map(EvaluationLedger::runOf).toList();
    }

    /**
     * 🔎 One run.
     *
     * @param runId the run
     * @return the run, or empty
     */
    public Optional<Run> findRun(String runId) {
        List<?> rows = entityManager.createNativeQuery("SELECT " + RUN_COLUMNS + " FROM evaluation_runs WHERE id = :id")
                .setParameter("id", runId)
                .getResultList();

        return rows.stream().map(Object[].class::cast).map(EvaluationLedger::runOf).findFirst();
    }

    /**
     * 🔬 What a run found, by outcome, then object, then check.
     *
     * @param runId the run
     * @return the results
     */
    public List<Result> results(String runId) {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT result.object_id, object.slug, object.title, result.check_code, result.outcome,
                               result.measured_value, result.tolerance, result.detail, result.duration_millis
                        FROM evaluation_results result
                        JOIN objects object ON object.id = result.object_id
                        WHERE result.run_id = :runId ORDER BY result.outcome, object.slug, result.check_code
                        """)
                .setParameter("runId", runId)
                .getResultList();

        return rows.stream()
                .map(Object[].class::cast)
                .map(columns -> new Result(NativeValues.stringOf(columns[0]),
                                           NativeValues.stringOf(columns[1]),
                                           NativeValues.stringOf(columns[2]),
                                           NativeValues.stringOf(columns[3]),
                                           NativeValues.stringOf(columns[4]),
                                           NativeValues.doubleOf(columns[5]),
                                           NativeValues.doubleOf(columns[6]),
                                           NativeValues.stringOf(columns[7]),
                                           NativeValues.longOf(columns[8])))
                .toList();
    }

    private static Run runOf(Object[] columns) {
        return new Run(NativeValues.stringOf(columns[0]), NativeValues.stringOf(columns[1]),
                       NativeValues.stringOf(columns[2]), NativeValues.stringOf(columns[3]),
                       NativeValues.instantOf(columns[4]), NativeValues.instantOf(columns[5]),
                       NativeValues.intOf(columns[6]), NativeValues.intOf(columns[7]), NativeValues.intOf(columns[8]),
                       NativeValues.intOf(columns[9]), NativeValues.intOf(columns[10]));
    }
}
