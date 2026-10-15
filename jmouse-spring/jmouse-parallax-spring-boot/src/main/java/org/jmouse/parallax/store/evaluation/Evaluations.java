package org.jmouse.parallax.store.evaluation;

import org.jmouse.parallax.jpa.evaluation.EvaluationLedger;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.verification.ConsistencyCheck;
import org.jmouse.parallax.verification.ConsistencyCheck.CheckResult;
import org.jmouse.parallax.verification.StandardChecks;
import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.event.EvaluationCompleted;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.jmouse.parallax.support.ParallaxIdentifiers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Model consistency: runs every applicable check over the published catalogue (or one object) and keeps the report.
 * A check that throws is a failure with the exception's words, so one broken object never hides the rest.
 *
 * <p>Each object is compiled with the components it may use, exactly as the catalogue and the bench compile it — a
 * circuit built of saved components (a signal generator, a model) is evaluated, not refused for a missing part. Every
 * write to the ledger commits on its own, so a run that fails half way still shows what it found.</p>
 */
public class Evaluations {

    public record RunView(String id, String status, String objectId, String startedBy, Instant startedAt,
                          Instant finishedAt, int totalChecks, int passed, int warned, int failed, int skipped) {
    }

    public record ResultView(String objectId, String objectSlug, String objectTitle, String checkCode, String outcome,
                             Double measuredValue, Double tolerance, String detail, long durationMillis) {
    }

    public record RunReport(RunView run, List<ResultView> results) {
    }

    public record CheckView(String code, String description) {
    }

    private final List<ConsistencyCheck> checks = StandardChecks.all();
    private final Catalogue catalogue;
    private final ParallaxAuthority authority;
    private final ApplicationEventPublisher events;
    private final EvaluationLedger ledger;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public Evaluations(Catalogue catalogue, ParallaxAuthority authority,
                       ApplicationEventPublisher events, EvaluationLedger ledger, TransactionTemplate transactions,
                       Clock clock) {
        this.catalogue = catalogue;
        this.authority = authority;
        this.events = events;
        this.ledger = ledger;
        this.transactions = transactions;
        this.clock = clock;
    }

    public List<CheckView> checks() {
        authority.requireGlobally(ParallaxCapability.EVALUATION_READ);
        return checks.stream().map(check -> new CheckView(check.code(), check.description())).toList();
    }

    /** Runs the checks now and answers with the report; the catalogue is small enough for that to take seconds. */
    public RunReport run(String objectId) {
        authority.requireGlobally(ParallaxCapability.EVALUATION_RUN);
        String actor = authority.requireActor();
        List<Catalogue.Evaluable> targets = catalogue.evaluables(objectId);
        String runId = ParallaxIdentifiers.newIdentifier();
        String evaluatedObjectId = objectId == null ? null : targets.getFirst().objectId();
        transactions.executeWithoutResult(status -> ledger.startRun(runId, evaluatedObjectId, actor, clock.instant()));
        Map<ConsistencyCheck.Outcome, Integer> counts;
        try {
            counts = evaluate(runId, targets);
        } catch (RuntimeException stopped) {
            transactions.executeWithoutResult(status -> ledger.failRun(runId, clock.instant()));
            throw stopped;
        }
        int passed = counts.getOrDefault(ConsistencyCheck.Outcome.PASSED, 0);
        int warned = counts.getOrDefault(ConsistencyCheck.Outcome.WARNED, 0);
        int failed = counts.getOrDefault(ConsistencyCheck.Outcome.FAILED, 0);
        int skipped = counts.getOrDefault(ConsistencyCheck.Outcome.SKIPPED, 0);
        transactions.executeWithoutResult(status -> ledger.completeRun(runId, clock.instant(), passed, warned, failed,
                skipped));
        events.publishEvent(new EvaluationCompleted(runId, actor, passed, warned, failed));
        return report(runId);
    }

    /** Runs every applicable check over every target, recording each result as it is found; answers the counts. */
    private Map<ConsistencyCheck.Outcome, Integer> evaluate(String runId, List<Catalogue.Evaluable> targets) {
        Map<ConsistencyCheck.Outcome, Integer> counts = new EnumMap<>(ConsistencyCheck.Outcome.class);
        for (Catalogue.Evaluable object : targets) {
            CompiledObject compiled = null;
            RuntimeException compilation = null;
            try {
                compiled = Catalogue.compileChecked(object.definition(), object.parameters(), Map.of(),
                        object.components());
            } catch (RuntimeException failure) {
                compilation = failure;
            }
            for (ConsistencyCheck check : checks) {
                if (!check.appliesTo(object.definition())) {
                    continue;
                }
                long started = System.nanoTime();
                CheckResult result;
                try {
                    if (compilation != null) {
                        throw compilation;
                    }
                    result = check.check(compiled);
                } catch (RuntimeException failure) {
                    result = new CheckResult(ConsistencyCheck.Outcome.FAILED, null, null,
                            failure.getClass().getSimpleName() + ": " + failure.getMessage());
                }
                long millis = (System.nanoTime() - started) / 1_000_000;
                counts.merge(result.outcome(), 1, Integer::sum);
                CheckResult found = result;
                transactions.executeWithoutResult(status -> ledger.recordResult(ParallaxIdentifiers.newIdentifier(), runId,
                        object.objectId(), check.code(), found.outcome().name(), finiteOrNull(found.measured()),
                        finiteOrNull(found.tolerance()), found.detail(), millis));
            }
        }
        return counts;
    }

    public List<RunView> runs(int size) {
        authority.requireGlobally(ParallaxCapability.EVALUATION_READ);
        return ledger.runsNewestFirst(Math.max(1, Math.min(size, 200))).stream().map(Evaluations::runView).toList();
    }

    public RunReport report(String runId) {
        authority.requireGlobally(ParallaxCapability.EVALUATION_READ);
        RunView run = ledger.findRun(runId).map(Evaluations::runView)
                .orElseThrow(() -> ParallaxProblem.notFound("The evaluation run"));
        List<ResultView> results = ledger.results(runId).stream()
                .map(result -> new ResultView(result.objectId(), result.objectSlug(), result.objectTitle(),
                        result.checkCode(), result.outcome(), result.measuredValue(), result.tolerance(), result.detail(),
                        result.durationMillis()))
                .toList();
        return new RunReport(run, results);
    }

    private static RunView runView(EvaluationLedger.Run run) {
        return new RunView(run.id(), run.status(), run.objectId(), run.startedBy(), run.startedAt(), run.finishedAt(),
                run.totalChecks(), run.passed(), run.warned(), run.failed(), run.skipped());
    }

    private static Double finiteOrNull(Double value) {
        return value == null || !Double.isFinite(value) ? null : value;
    }
}
