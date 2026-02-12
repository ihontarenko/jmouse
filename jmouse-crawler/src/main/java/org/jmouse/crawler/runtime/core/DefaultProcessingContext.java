package org.jmouse.crawler.runtime.core;

import org.jmouse.core.Verify;
import org.jmouse.crawler.api.*;
import org.jmouse.crawler.api.FetchResult;
import org.jmouse.crawler.api.ParsedDocument;
import org.jmouse.crawler.api.Frontier;
import org.jmouse.crawler.api.SeenStore;
import org.jmouse.crawler.api.ScopePolicy;
import org.jmouse.crawler.runtime.state.DecisionCodes;

import java.net.URI;

/**
 * Default {@link ProcessingContext} implementation used during execution of a single {@link ProcessingTask}. 🧠
 *
 * <p>This context is mutable and is typically populated in stages:</p>
 * <ul>
 *   <li>fetch stage sets {@link #setFetchResult(FetchResult)}</li>
 *   <li>parse stage sets {@link #setDocument(ParsedDocument)}</li>
 *   <li>routing selects and sets {@link #setRouteId(String)}</li>
 *   <li>extraction enqueues new URLs via {@link #enqueue(URI)} / {@link #enqueue(URI, RoutingHint)}</li>
 * </ul>
 *
 * <p>Decision logging is delegated to the run-level {@link DecisionLog} so that
 * decisions can be aggregated and observed at the crawl-run scope.</p>
 *
 * <p>Thread-safety: instances are intended to be confined to a single task execution thread.</p>
 */
public final class DefaultProcessingContext implements ProcessingContext {

    private final ProcessingTask task;
    private final RunContext     run;
    private final DecisionLog    decisions;

    private FetchResult    fetchResult;
    private ParsedDocument document;
    private String         routeId;

    /**
     * @param task       task being processed (must be non-null)
     * @param runContext run-level context (must be non-null)
     */
    public DefaultProcessingContext(ProcessingTask task, RunContext runContext) {
        this.task = Verify.nonNull(task, "task");
        this.run = Verify.nonNull(runContext, "runContext");
        this.decisions = Verify.nonNull(runContext.decisionLog(), "runContext.decisionLog");
    }

    @Override
    public ProcessingTask processingTask() {
        return task;
    }

    @Override
    public RunContext run() {
        return run;
    }

    @Override
    public FetchResult fetchResult() {
        return fetchResult;
    }

    @Override
    public void setFetchResult(FetchResult result) {
        this.fetchResult = result;
    }

    @Override
    public ParsedDocument document() {
        return document;
    }

    @Override
    public void setDocument(ParsedDocument document) {
        this.document = document;
    }

    @Override
    public DecisionLog decisionLog() {
        return decisions;
    }

    @Override
    public String routeId() {
        return routeId;
    }

    @Override
    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    @Override
    public void enqueue(URI url) {
        enqueue(url, null);
    }

    /**
     * Enqueue a discovered URL as a new {@link ProcessingTask}.
     *
     * <p>This method applies:</p>
     * <ul>
     *   <li>{@link ScopePolicy} validation</li>
     *   <li>{@link SeenStore} duplicate suppression</li>
     * </ul>
     *
     * <p>If accepted, the new task is offered to the run's {@link Frontier}.</p>
     *
     * @param url  discovered URL
     * @param hint optional routing hint for the next task
     */
    @Override
    public void enqueue(URI url, RoutingHint hint) {
        if (url == null) {
            decisions.reject(DecisionCodes.INVALID_URL, "null url");
            return;
        }

        ScopePolicy scope    = run.scopePolicy();
        SeenStore   seen     = run.seenStore();
        Frontier    frontier = run.frontier();

        ProcessingTask parent = this.task;

        if (url.equals(parent.url())) {
            decisions.reject(DecisionCodes.DUPLICATE_SELF, "self link");
            return;
        }

        TaskOrigin origin = TaskOrigin.discovered(
                "pipeline",
                routeId(),
                parent.id()
        );

        ProcessingTask next = run.taskFactory().childOf(parent, url, hint, origin);

        if (scope.isDisallowed(next)) {
            decisions.reject(DecisionCodes.SCOPE_DENY, "out of scope");
            return;
        }

        if (!seen.markDiscovered(next.url())) {
            decisions.reject(DecisionCodes.DUPLICATE_DISCOVERED, "duplicate discovered");
            return;
        }

        frontier.offer(next);

        decisions.accept(DecisionCodes.ENQUEUE_ACCEPT,
                         "enqueued childId=%s parentId=%s route=%s".formatted(next.id(), parent.id(), routeId()));
    }

}
