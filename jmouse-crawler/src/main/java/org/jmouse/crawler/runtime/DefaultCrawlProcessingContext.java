package org.jmouse.crawler.runtime;

import org.jmouse.crawler.spi.FetchResult;
import org.jmouse.crawler.spi.ParsedDocument;
import org.jmouse.crawler.spi.ScopePolicy;
import org.jmouse.crawler.spi.SeenStore;

import java.net.URI;

public final class DefaultCrawlProcessingContext implements CrawlProcessingContext {

    private final CrawlTask       task;
    private final CrawlRunContext runContext;
    private final DecisionLog     decisions;

    private FetchResult    fetchResult;
    private ParsedDocument document;
    private String         routeId;

    public DefaultCrawlProcessingContext(CrawlTask task, CrawlRunContext runContext) {
        this.task = task;
        this.runContext = runContext;
        this.decisions = runContext.decisionLog();
    }

    @Override
    public CrawlTask task() {
        return task;
    }

    @Override
    public CrawlRunContext run() {
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
    public void enqueue(URI url, CrawlHint hint) {
        CrawlTask   parent      = this.task;
        ScopePolicy scopePolicy = runContext.scope();
        SeenStore   seenStore   = runContext.seen();
        Frontier    frontier    = runContext.frontier();

        CrawlTask next = new CrawlTask(
                url,
                parent.depth() + 1,
                parent.url(),
                "from:" + parent.url(),
                parent.priority(),
                runContext.clock().instant(),
                0,
                hint
        );

        if (!scopePolicy.isAllowed(next)) {
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
