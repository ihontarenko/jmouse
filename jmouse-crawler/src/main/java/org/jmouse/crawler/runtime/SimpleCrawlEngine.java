package org.jmouse.crawler.runtime;

import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.core.TaskDisposition;
import org.jmouse.crawler.core.TaskDisposition.Completed;
import org.jmouse.crawler.core.TaskDisposition.Discarded;
import org.jmouse.crawler.core.TaskDisposition.RetryLater;
import org.jmouse.crawler.routing.CrawlRoute;
import org.jmouse.crawler.routing.PipelineResult;
import org.jmouse.crawler.spi.RetryDecision;

import java.time.Instant;
import java.util.List;

public final class SimpleCrawlEngine implements ParallelCrawlEngine {

    private final CrawlRunContext run;

    public SimpleCrawlEngine(CrawlRunContext run) {
        this.run = run;
    }

    @Override
    public void submit(CrawlTask task) {
        run.frontier().offer(task);
    }

    @Override
    public boolean tick() {
        Instant now = run.clock().instant();

        boolean didWork = false;

        // 1) Move ready retries back to frontier
        List<CrawlTask> ready = run.retryBuffer().drainReady(now, 128);
        if (!ready.isEmpty()) {
            didWork = true;
            for (CrawlTask t : ready) run.frontier().offer(t);
        }

        // 2) Take next task
        CrawlTask task = run.frontier().poll();
        if (task == null) {
            return didWork; // false if nothing moved and nothing polled
        }

        // 3) Scope + seen gate
        if (!run.scope().isAllowed(task)) return true;
        if (!run.seen().firstTime(task.url())) return true;

        // 4) Execute
        TaskDisposition taskDisposition = executeTask(task, now);

        // 5) Apply disposition
        applyDisposition(task, taskDisposition, now);

        return true;
    }

    @Override
    public int moveReadyRetries(int max) {
        Instant now = now();
        List<CrawlTask> ready = run.retryBuffer().drainReady(now, max);
        for (CrawlTask t : ready) {
            run.frontier().offer(t);
        }
        return ready.size();
    }

    @Override
    public CrawlTask poll() {
        return run.frontier().poll();
    }

    @Override
    public TaskDisposition execute(CrawlTask task, Instant now) {
        // scope+seen gate must be done before pipeline execution
        if (!run.scope().isAllowed(task)) {
            return TaskDisposition.discarded(run.scope().denyReason(task));
        }
        if (!run.seen().firstTime(task.url())) {
            return TaskDisposition.discarded("duplicate");
        }
        return executeTask(task, now);
    }

    @Override
    public void apply(CrawlTask task, TaskDisposition disposition, Instant now) {
        applyDisposition(task, disposition, now);
    }

    @Override
    public int frontierSize() {
        return run.frontier().size();
    }

    @Override
    public int retrySize() {
        return run.retryBuffer().size();
    }

    @Override
    public Instant now() {
        return run.clock().instant();
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
        if (disposition instanceof Completed || disposition instanceof Discarded) {
            return;
        }

        if (disposition instanceof RetryLater(Instant notBefore, String reason, Throwable error)) {
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
