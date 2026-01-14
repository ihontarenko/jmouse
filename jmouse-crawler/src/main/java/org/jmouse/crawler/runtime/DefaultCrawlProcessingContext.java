package org.jmouse.crawler.runtime;

import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.runtime.CrawlProcessingContext;
import org.jmouse.crawler.runtime.CrawlRunContext;
import org.jmouse.crawler.runtime.DecisionLog;
import org.jmouse.crawler.spi.FetchResult;
import org.jmouse.crawler.spi.ParsedDocument;

public final class DefaultCrawlProcessingContext implements CrawlProcessingContext {

    private final CrawlTask task;
    private final CrawlRunContext run;
    private final DecisionLog decisions;

    private FetchResult fetchResult;
    private ParsedDocument document;

    private String routeId;

    public DefaultCrawlProcessingContext(CrawlTask task, CrawlRunContext run) {
        this.task = task;
        this.run = run;
        this.decisions = new InMemoryDecisionLog();
    }

    @Override public CrawlTask task() { return task; }
    @Override public CrawlRunContext run() { return run; }

    @Override public FetchResult fetchResult() { return fetchResult; }
    @Override public void setFetchResult(FetchResult result) { this.fetchResult = result; }

    @Override public ParsedDocument document() { return document; }
    @Override public void setDocument(ParsedDocument document) { this.document = document; }

    @Override public DecisionLog decisions() { return decisions; }

    @Override public String routeId() { return routeId; }
    @Override public void setRouteId(String routeId) { this.routeId = routeId; }
}
