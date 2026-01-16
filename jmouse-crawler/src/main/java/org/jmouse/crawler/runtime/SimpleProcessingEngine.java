package org.jmouse.crawler.runtime;

import org.jmouse.core.Verify;
import org.jmouse.crawler.routing.ProcessingRoute;
import org.jmouse.crawler.routing.ProcessingRouteRegistry;
import org.jmouse.crawler.routing.PipelineResult;
import org.jmouse.crawler.spi.*;

import java.time.Instant;

public final class SimpleProcessingEngine implements ProcessingEngine {

    private static final String STAGE_PIPELINE = "pipeline";
    private static final int    MAX_ROUTE_HOPS = 8;

    private final RunContext runContext;

    public SimpleProcessingEngine(RunContext runContext) {
        this.runContext = Verify.nonNull(runContext, "runContext");
    }

    @Override
    public void apply(ProcessingTask task, TaskDisposition disposition, Instant now) {
        Verify.nonNull(task, "task");
        Verify.nonNull(disposition, "disposition");
        Verify.nonNull(now, "now");
        applyDisposition(task, disposition, now);
    }

    @Override
    public TaskDisposition execute(ProcessingTask task) {
        Verify.nonNull(task, "task");

        Instant          now              = runContext.clock().instant();
        ScopePolicy      scopePolicy      = runContext.scope();
        SeenStore        seenStore        = runContext.seen();

        if (scopePolicy.isDisallowed(task)) {
            return TaskDisposition.discarded(scopePolicy.denyReason(task));
        }

        if (seenStore.isProcessed(task.url())) {
            return TaskDisposition.discarded("already processed");
        }

        return doExecutePipeline(task, now);
    }

    private TaskDisposition doExecutePipeline(ProcessingTask task, Instant now) {
        SeenStore                seenStore         = runContext.seen();
        DefaultProcessingContext processingContext = new DefaultProcessingContext(task, runContext);
        ProcessingRoute          route             = runContext.routes().resolve(task, runContext);

        if (route == null) {
            return TaskDisposition.deadLetter("No route resolved", null, STAGE_PIPELINE, "route:unknown");
        }

        processingContext.setRouteId(route.id());

        try {
            PipelineResult ignore = route.pipeline().execute(processingContext);
            ignore = followRouteHops(processingContext, ignore);
            seenStore.markProcessed(task.url());
            return TaskDisposition.completed();
        } catch (Throwable error) {
            RetryDecision retryDecision = runContext.retry().onFailure(task, error, now);
            return toTaskDisposition(retryDecision, error, processingContext.routeId(), STAGE_PIPELINE);
        }
    }

    private PipelineResult followRouteHops(
            DefaultProcessingContext processingContext,
            PipelineResult initialResult
    ) throws Exception {

        PipelineResult currentResult = initialResult;

        for (int hop = 1; hop <= MAX_ROUTE_HOPS; hop++) {
            if (!(currentResult instanceof PipelineResult.Route routeInstruction)) {
                return currentResult;
            }

            String nextRouteId = routeInstruction.routeId();

            ProcessingRouteRegistry registry = Verify.instanceOf(
                    runContext.routes(), ProcessingRouteRegistry.class, "runContext.routes"
            );

            ProcessingRoute nextRoute = registry.byId(nextRouteId);
            Verify.state(nextRoute != null, "next route not found: " + nextRouteId);
            processingContext.setRouteId(nextRoute.id());
            currentResult = nextRoute.pipeline().execute(processingContext);
        }

        throw new IllegalStateException("Route hop limit exceeded (" + MAX_ROUTE_HOPS + ")");
    }

    private TaskDisposition toTaskDisposition(
            RetryDecision retryDecision,
            Throwable error,
            String routeId,
            String stageId
    ) {
        return switch (retryDecision) {
            case RetryDecision.Retry(Instant notBefore, String reason) ->
                    TaskDisposition.retryLater(notBefore, reason, error, stageId, routeId);
            case RetryDecision.Discard(String reason) ->
                    TaskDisposition.discarded(reason);
            case RetryDecision.DeadLetter(String reason) ->
                    TaskDisposition.deadLetter(reason, error, stageId, routeId);
        };
    }

    private void applyDisposition(ProcessingTask task, TaskDisposition disposition, Instant now) {
        if (disposition instanceof TaskDisposition.Completed || disposition instanceof TaskDisposition.Discarded) {
            return;
        }

        if (disposition instanceof TaskDisposition.RetryLater retryLater) {
            ProcessingTask newTask = task.attempt(now);
            runContext.retryBuffer().schedule(newTask, retryLater.notBefore(), retryLater.reason(), retryLater.error());
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
            runContext.deadLetterQueue().put(task, item);
        }
    }
}
