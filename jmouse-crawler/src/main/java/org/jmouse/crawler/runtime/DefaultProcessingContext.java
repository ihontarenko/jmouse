package org.jmouse.crawler.runtime;

import org.jmouse.crawler.spi.FetchResult;
import org.jmouse.crawler.spi.ParsedDocument;
import org.jmouse.crawler.spi.ScopePolicy;
import org.jmouse.crawler.spi.SeenStore;

import java.net.URI;

public final class DefaultProcessingContext implements ProcessingContext {

    private final ProcessingTask task;
    private final RunContext     runContext;
    private final DecisionLog    decisions;

    private FetchResult    fetchResult;
    private ParsedDocument document;
    private String         routeId;

    public DefaultProcessingContext(ProcessingTask task, RunContext runContext) {
        this.task = task;
        this.runContext = runContext;
        this.decisions = runContext.decisionLog();
    }

    @Override
    public ProcessingTask task() {
        return task;
    }

    @Override
    public RunContext run() {
        return runContext;
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
    public DecisionLog decisions() {
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

    @Override
    public void enqueue(URI url, RoutingHint hint) {
        ProcessingTask parent      = this.task;
        ScopePolicy    scopePolicy = runContext.scope();
        SeenStore      seenStore   = runContext.seen();
        Frontier       frontier    = runContext.frontier();

        ProcessingTask next = new ProcessingTask(
                url,
                parent.depth() + 1,
                parent.url(),
                parent.url().toString(),
                parent.priority(),
                runContext.clock().instant(),
                0,
                hint
        );

        if (scopePolicy.isDisallowed(next)) {
            decisions.reject(DecisionCodes.SCOPE_DENY, "out of scope");
            return;
        }

        if (!seenStore.markDiscovered(next.url())) {
            decisions.reject(DecisionCodes.DUPLICATE_DISCOVERED, "duplicate discovered");
            return;
        }

        frontier.offer(next);
    }

}
