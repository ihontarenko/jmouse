package org.jmouse.crawler.runtime;

import org.jmouse.crawler.routing.CrawlRouteResolver;
import org.jmouse.crawler.spi.*;

import java.time.Clock;

public final class DefaultCrawlRunContext implements CrawlRunContext {

    private final Frontier           frontier;
    private final RetryBuffer        retryBuffer;
    private final DeadLetterQueue    dlq;
    private final DecisionLog        decisionLog;
    private final CrawlRouteResolver routes;
    private final Fetcher            fetcher;
    private final ParserRegistry     parsers;
    private final SeenStore          seen;
    private final ScopePolicy        scope;
    private final RetryPolicy        retry;
    private final UtilityRegistry    utilities;
    private final PolitenessPolicy   politeness;

    private final Clock clock;

    public DefaultCrawlRunContext(
            Frontier frontier,
            RetryBuffer retryBuffer,
            DeadLetterQueue dlq,
            DecisionLog decisionLog,
            CrawlRouteResolver routes,
            Fetcher fetcher,
            ParserRegistry parsers,
            SeenStore seen,
            ScopePolicy scope,
            RetryPolicy retry,
            Clock clock,
            UtilityRegistry utilities,
            PolitenessPolicy politeness
    ) {
        this.frontier = frontier;
        this.retryBuffer = retryBuffer;
        this.dlq = dlq;
        this.decisionLog = decisionLog;
        this.routes = routes;
        this.fetcher = fetcher;
        this.parsers = parsers;
        this.seen = seen;
        this.scope = scope;
        this.retry = retry;
        this.clock = clock;
        this.utilities = utilities;
        this.politeness = politeness;
    }

    @Override
    public Frontier frontier() {
        return frontier;
    }

    @Override
    public RetryBuffer retryBuffer() {
        return retryBuffer;
    }

    @Override
    public DeadLetterQueue deadLetterQueue() {
        return dlq;
    }

    @Override
    public DecisionLog decisionLog() {
        return decisionLog;
    }

    @Override
    public CrawlRouteResolver routes() {
        return routes;
    }

    @Override
    public Fetcher fetcher() {
        return fetcher;
    }

    @Override
    public ParserRegistry parsers() {
        return parsers;
    }

    @Override
    public SeenStore seen() {
        return seen;
    }

    @Override
    public ScopePolicy scope() {
        return scope;
    }

    @Override
    public RetryPolicy retry() {
        return retry;
    }

    @Override
    public Clock clock() {
        return clock;
    }

    @Override
    public UtilityRegistry utilities() {
        return utilities;
    }

    @Override
    public PolitenessPolicy politeness() {
        return politeness;
    }
}
