package org.jmouse.crawler.runtime;

import org.jmouse.core.Verify;
import org.jmouse.crawler.routing.CrawlRoute;
import org.jmouse.crawler.routing.CrawlRouteRegistry;
import org.jmouse.crawler.routing.PipelineResult;
import org.jmouse.crawler.spi.*;

import java.time.Instant;
import java.util.List;

public final class SimpleCrawlEngine implements ParallelCrawlEngine {

    private static final String STAGE_PIPELINE          = "pipeline";
    private static final String RETRY_REASON_POLITENESS = "politeness";
    private static final int    MAX_ROUTE_HOPS          = 8;

    private final CrawlRunContext runContext;

    public SimpleCrawlEngine(CrawlRunContext runContext) {
        this.runContext = Verify.nonNull(runContext, "runContext");
    }

    @Override
    public void submit(CrawlTask task) {
        Verify.nonNull(task, "task");
        runContext.frontier().offer(task);
    }

    @Override
    public int moveReadyRetries(int max) {
        Instant         now        = now();
        List<CrawlTask> readyTasks = runContext.retryBuffer().drainReady(now, max);

        for (CrawlTask readyTask : readyTasks) {
            runContext.frontier().offer(readyTask);
        }

        return readyTasks.size();
    }

    @Override
    public CrawlTask poll() {
        return runContext.frontier().poll();
    }

    @Override
    public void apply(CrawlTask task, TaskDisposition disposition, Instant now) {
        Verify.nonNull(task, "task");
        Verify.nonNull(disposition, "disposition");
        Verify.nonNull(now, "now");
        applyDisposition(task, disposition, now);
    }

    @Override
    public int frontierSize() {
        return runContext.frontier().size();
    }

    @Override
    public int retrySize() {
        return runContext.retryBuffer().size();
    }

    @Override
    public Instant now() {
        return runContext.clock().instant();
    }

    @Override
    public TaskDisposition execute(CrawlTask task, Instant now) {
        Verify.nonNull(task, "task");
        Verify.nonNull(now, "now");

        ScopePolicy      scopePolicy      = runContext.scope();
        SeenStore        seenStore        = runContext.seen();
        PolitenessPolicy politenessPolicy = runContext.politeness();
        Instant          notBefore        = politenessPolicy.notBefore(task.url(), now);

        if (scopePolicy.isDisallowed(task)) {
            return TaskDisposition.discarded(scopePolicy.denyReason(task));
        }

        if (notBefore.isAfter(now)) {
            return TaskDisposition.retryLater(notBefore, RETRY_REASON_POLITENESS, null, STAGE_PIPELINE, "route:unknown");
        }

        if (seenStore.isProcessed(task.url())) {
            return TaskDisposition.discarded("already processed");
        }

        return doExecutePipeline(task, now);
    }

    private TaskDisposition doExecutePipeline(CrawlTask task, Instant now) {
        SeenStore                     seenStore         = runContext.seen();
        DefaultCrawlProcessingContext processingContext = new DefaultCrawlProcessingContext(task, runContext);
        CrawlRoute                    route             = runContext.routes().resolve(task, runContext);

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
            return mapRetryDecision(retryDecision, error, processingContext.routeId(), STAGE_PIPELINE);
        }
    }

    private PipelineResult followRouteHops(
            DefaultCrawlProcessingContext processingContext,
            PipelineResult initialResult
    ) throws Exception {

        PipelineResult currentResult = initialResult;

        for (int hop = 1; hop <= MAX_ROUTE_HOPS; hop++) {
            if (!(currentResult instanceof PipelineResult.Route routeInstruction)) {
                return currentResult;
            }

            String nextRouteId = routeInstruction.routeId();

            CrawlRouteRegistry registry = Verify.instanceOf(
                    runContext.routes(), CrawlRouteRegistry.class, "runContext.routes"
            );

            CrawlRoute nextRoute = registry.byId(nextRouteId);
            Verify.state(nextRoute != null, "next route not found: " + nextRouteId);
            processingContext.setRouteId(nextRoute.id());
            currentResult = nextRoute.pipeline().execute(processingContext);
        }

        throw new IllegalStateException("Route hop limit exceeded (" + MAX_ROUTE_HOPS + ")");
    }

    private TaskDisposition mapRetryDecision(
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

            default ->
                    TaskDisposition.deadLetter("Unknown retry decision: " + retryDecision, error, stageId, routeId);
        };
    }

    private void applyDisposition(CrawlTask task, TaskDisposition disposition, Instant now) {
        if (disposition instanceof TaskDisposition.Completed || disposition instanceof TaskDisposition.Discarded) {
            return;
        }

        System.out.println("DLQ: "
                                   + runContext.deadLetterQueue().size()
                                   + " FRONTIER: "
                                   + runContext.frontier().size()
                                   + " RETRY_BUFFER: "
                                   + runContext.retryBuffer().size());

        if (disposition instanceof TaskDisposition.RetryLater retryLater) {
            CrawlTask nextAttempt = task.nextAttempt(now);
            runContext.retryBuffer().schedule(nextAttempt, retryLater.notBefore(), retryLater.reason(), retryLater.error());
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
