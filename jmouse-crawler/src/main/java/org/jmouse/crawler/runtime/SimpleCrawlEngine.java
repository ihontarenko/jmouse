package org.jmouse.crawler.runtime.impl;

import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.core.TaskDisposition;
import org.jmouse.crawler.routing.CrawlRoute;
import org.jmouse.crawler.routing.PipelineResult;
import org.jmouse.crawler.runtime.*;
import org.jmouse.crawler.spi.FetchRequest;
import org.jmouse.crawler.spi.RetryDecision;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class SimpleCrawlEngine implements CrawlEngine {

    private final CrawlRunContext run;

    public SimpleCrawlEngine(CrawlRunContext run) {
        this.run = run;
    }

    @Override
    public void submit(CrawlTask task) {
        run.frontier().offer(task);
    }

    @Override
    public void runOnce() {
        Instant now = run.clock().instant();

        // 1) Move ready retries back to frontier
        List<CrawlTask> ready = run.retryBuffer().drainReady(now, 128);
        for (CrawlTask t : ready) {
            run.frontier().offer(t);
        }

        // 2) Take next task
        CrawlTask task = run.frontier().poll();
        if (task == null) {
            return;
        }

        // 3) Basic scope + seen gate (ETAP 1 keeps it minimal)
        if (!run.scope().isAllowed(task)) {
            String reason = run.scope().denyReason(task);
            // discard (not a failure)
            return;
        }
        if (!run.seen().firstTime(task.url())) {
            // duplicate
            return;
        }

        // 4) Execute route pipeline
        TaskDisposition disposition = executeTask(task, now);

        // 5) Apply disposition
        applyDisposition(task, disposition, now);
    }

    @Override
    public void runUntilDrained() {
        // Minimal draining strategy:
        // keep running while there is something in frontier or retry buffer ready soon.
        // For ETAP 1 we stop when frontier is empty AND retryBuffer is empty.
        while (run.frontier().size() > 0 || run.retryBuffer().size() > 0) {
            runOnce();
            // Note: no sleeping/backoff here in ETAP 1.
            // In ETAP 2/4 we will add gating and time-based loop.
            if (run.frontier().size() == 0 && run.retryBuffer().size() > 0) {
                // If only delayed retries exist, ETAP 1 ends (no waiting).
                break;
            }
        }
    }

    private TaskDisposition executeTask(CrawlTask task, Instant now) {
        DefaultCrawlProcessingContext context = new DefaultCrawlProcessingContext(task, run);

        CrawlRoute route = run.routes().resolve(task, run);

        if (route == null) {
            return TaskDisposition.deadLetter("No route resolved", null);
        }

        context.setRouteId(route.id());

        try {
            PipelineResult result = route.pipeline().execute(context);
            return TaskDisposition.completed();
        } catch (Throwable error) {
            RetryDecision decision = run.retry().onFailure(task, error, now);

            if (decision instanceof RetryDecision.Retry(Instant notBefore, String reason)) {
                return TaskDisposition.retryLater(notBefore, reason, error);
            }
            if (decision instanceof RetryDecision.Discard(String reason)) {
                return TaskDisposition.discarded(reason);
            }
            if (decision instanceof RetryDecision.DeadLetter(String reason)) {
                return TaskDisposition.deadLetter(reason, error);
            }
            return TaskDisposition.deadLetter("Unknown retry decision", error);
        }
    }

    private void applyDisposition(CrawlTask task, TaskDisposition disposition, Instant now) {
        if (disposition instanceof TaskDisposition.Completed) {
            return;
        }

        if (disposition instanceof TaskDisposition.Discarded) {
            return;
        }

        if (disposition instanceof TaskDisposition.RetryLater(Instant notBefore, String reason, Throwable error)) {
            // we increment attempt on retry
            CrawlTask next = task.nextAttempt(now);
            run.retryBuffer().schedule(next, notBefore, reason, error);
            return;
        }

        if (disposition instanceof TaskDisposition.DeadLetter(String reason, Throwable error)) {
            DeadLetterItem item = new DeadLetterItem(
                    now,
                    reason,
                    "pipeline",
                    "route:" + safe(run, task),
                    task.attempt(),
                    error
            );
            run.deadLetterQueue().put(task, item);
        }
    }

    private String safe(CrawlRunContext run, CrawlTask task) {
        try {
            CrawlRoute route = run.routes().resolve(task, run);
            return route != null ? route.id() : "unknown";
        } catch (Throwable ignored) {
            return "unknown";
        }
    }
}
