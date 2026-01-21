package org.jmouse.crawler.runtime.core;

import org.jmouse.core.Verify;
import org.jmouse.crawler.api.*;
import org.jmouse.crawler.route.ProcessingRoute;
import org.jmouse.crawler.route.ProcessingRouteRegistry;
import org.jmouse.crawler.pipeline.PipelineResult;
import org.jmouse.crawler.api.DeadLetterItem;
import org.jmouse.crawler.api.SeenStore;
import org.jmouse.crawler.api.RetryPolicy;
import org.jmouse.crawler.api.ScopePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Simple, synchronous {@link ProcessingEngine} that executes a routing pipeline and
 * translates outcomes into {@link TaskDisposition}. ⚙️
 *
 * <p>Execution model:</p>
 * <ul>
 *   <li>{@link #execute(ProcessingTask)} performs task work (fetch/parse/pipeline) and produces a disposition.</li>
 *   <li>{@link #apply(ProcessingTask, TaskDisposition, Instant)} applies side effects (retry scheduling, DLQ) based on the disposition.</li>
 * </ul>
 *
 * <p>Key behaviors:</p>
 * <ul>
 *   <li>Scope and duplicate checks are performed before pipeline execution.</li>
 *   <li>Routes are resolved through {@link RunContext#routes()}.</li>
 *   <li>Pipeline may request routing hops via {@link PipelineResult.Route}, bounded by {@link #MAX_ROUTE_HOPS}.</li>
 *   <li>Failures are translated via {@link RetryPolicy} into retry/discard/dead-letter dispositions.</li>
 * </ul>
 *
 * <p>Thread-safety: the engine is safe to share if {@link RunContext} dependencies are thread-safe.
 * Individual {@link DefaultProcessingContext} instances are per-task and thread-confined.</p>
 */
public final class SimpleProcessingEngine implements ProcessingEngine {

    /**
     * Logical stage identifier used in dispositions produced by this engine.
     */
    private static final String STAGE_PIPELINE = "pipeline";

    /**
     * Safety bound against infinite route loops.
     */
    private static final int MAX_ROUTE_HOPS = 8;

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleProcessingEngine.class);

    private final RunContext run;

    /**
     * @param runContext run-level context (must be non-null)
     */
    public SimpleProcessingEngine(RunContext runContext) {
        this.run = Verify.nonNull(runContext, "runContext");
    }

    /**
     * Apply disposition side effects (retry scheduling and DLQ).
     *
     * <p>Completed and discarded dispositions have no further side effects.</p>
     */
    @Override
    public void apply(ProcessingTask task, TaskDisposition disposition, Instant now) {
        Verify.nonNull(task, "task");
        Verify.nonNull(disposition, "disposition");
        Verify.nonNull(now, "now");
        applyDisposition(task, disposition, now);
    }

    /**
     * Execute the task and compute the outcome disposition.
     *
     * <p>This method performs preflight validation (scope and duplicate checks)
     * and then runs the routing pipeline.</p>
     */
    @Override
    public TaskDisposition execute(ProcessingTask task) {
        Verify.nonNull(task, "task");

        Instant     now   = run.clock().instant();
        ScopePolicy scope = run.scope();
        SeenStore   seen  = run.seen();

        if (scope.isDisallowed(task)) {
            LOGGER.info("Disallowed execution of task {}", task);
            return TaskDisposition.discarded(scope.denyReason(task));
        }

        if (seen.isProcessed(task.url())) {
            LOGGER.info("Skipping execution of task {}. Already processed", task);
            return TaskDisposition.discarded("already processed");
        }

        return executePipeline(task, now);
    }

    /**
     * Resolve route and execute its pipeline, including bounded route hops.
     */
    private TaskDisposition executePipeline(ProcessingTask task, Instant now) {
        SeenStore seen = run.seen();

        DefaultProcessingContext processingContext = new DefaultProcessingContext(task, run);
        ProcessingRoute          route             = run.routes().resolve(task, run);

        if (route == null) {
            LOGGER.error("No route found for task {}", task);
            return TaskDisposition.deadLetter("No route resolved", null, STAGE_PIPELINE, "route:unknown");
        }

        processingContext.setRouteId(route.id());

        try {
            PipelineResult result = route.pipeline().execute(processingContext);
            result = followRouteHops(processingContext, result);

            // Mark as processed only after a successful pipeline completion.
            seen.markProcessed(task.url());

            return TaskDisposition.completed(result);
        } catch (Throwable error) {
            RetryDecision decision = run.retry().onFailure(task, error, now);
            return toDisposition(decision, error, processingContext.routeId(), STAGE_PIPELINE);
        }
    }

    /**
     * Follow route hop instructions emitted by pipelines.
     *
     * <p>When a pipeline returns {@link PipelineResult.Route}, the engine resolves
     * the target route and executes its pipeline with the same context.
     * A hop limit is enforced to prevent infinite routing loops.</p>
     *
     * @throws Exception if a downstream pipeline throws
     */
    private PipelineResult followRouteHops(DefaultProcessingContext processingContext, PipelineResult initialResult) throws Exception {
        PipelineResult result = initialResult;

        for (int hop = 1; hop <= MAX_ROUTE_HOPS; hop++) {
            if (!(result instanceof PipelineResult.Route routeInstruction)) {
                return result;
            }

            String nextRouteId = routeInstruction.routeId();

            ProcessingRouteRegistry registry = Verify.instanceOf(
                    run.routes(), ProcessingRouteRegistry.class, "run.routes"
            );

            ProcessingRoute nextRoute = registry.byId(nextRouteId);
            Verify.state(nextRoute != null, "next route not found: " + nextRouteId);

            processingContext.setRouteId(nextRoute.id());

            // Hop to the next route.
            result = nextRoute.pipeline().execute(processingContext);
        }

        throw new IllegalStateException("Route hop limit exceeded (" + MAX_ROUTE_HOPS + ")");
    }

    /**
     * Translate {@link RetryDecision} into a {@link TaskDisposition}.
     */
    private static TaskDisposition toDisposition(
            RetryDecision decision,
            Throwable error,
            String routeId,
            String stageId
    ) {
        return switch (decision) {
            case RetryDecision.Retry(Instant eligibleAt, String reason) ->
                    TaskDisposition.retryLater(eligibleAt, reason, error, stageId, routeId);
            case RetryDecision.Discard(String reason) ->
                    TaskDisposition.discarded(reason);
            case RetryDecision.DeadLetter(String reason) ->
                    TaskDisposition.deadLetter(reason, error, stageId, routeId);
        };
    }

    /**
     * Apply side effects implied by the disposition.
     *
     * <p>Important: this method is called by the runner, potentially from a different
     * thread than {@link #execute(ProcessingTask)}. Keep shared state mutation here.</p>
     */
    private void applyDisposition(ProcessingTask task, TaskDisposition disposition, Instant now) {
        if (disposition instanceof TaskDisposition.Completed || disposition instanceof TaskDisposition.Discarded) {
            return;
        }

        if (disposition instanceof TaskDisposition.RetryLater retryLater) {
            ProcessingTask retryTask = task.attempt(now);
            run.retryBuffer().schedule(
                    retryTask,
                    retryLater.eligibleAt(),
                    retryLater.reason(),
                    retryLater.error()
            );
            return;
        }

        if (disposition instanceof TaskDisposition.DeadLetter(
                String reason, Throwable error, String stageId, String routeId
        )) {
            DeadLetterItem item = new DeadLetterItem(
                    now,
                    reason,
                    stageId,
                    routeId,
                    task.attempt(),
                    error
            );
            run.deadLetterQueue().put(task, item);
        }
    }
}
