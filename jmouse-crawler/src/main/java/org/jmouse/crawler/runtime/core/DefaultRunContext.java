package org.jmouse.crawler.runtime.core;

import org.jmouse.crawler.api.*;
import org.jmouse.crawler.route.ProcessingRouteResolver;
import org.jmouse.crawler.runtime.dlq.DeadLetterQueue;
import org.jmouse.crawler.api.Frontier;
import org.jmouse.crawler.api.RetryBuffer;
import org.jmouse.crawler.api.DynamicAttributes;
import org.jmouse.crawler.api.SeenStore;
import org.jmouse.crawler.api.PolitenessPolicy;
import org.jmouse.crawler.api.RetryPolicy;
import org.jmouse.crawler.api.ScopePolicy;

import java.time.Clock;

public final class DefaultRunContext implements RunContext {

    private final Frontier        frontier;
    private final RetryBuffer     retryBuffer;
    private final DeadLetterQueue dlq;
    private final DecisionLog     decisionLog;
    private final DynamicAttributes       attributes;
    private final ProcessingRouteResolver routes;
    private final Fetcher                 fetcher;
    private final ParserRegistry parsers;
    private final SeenStore      seen;
    private final ScopePolicy    scope;
    private final RetryPolicy             retry;
    private final UtilityRegistry         utilities;
    private final PolitenessPolicy        politeness;

    private final Clock clock;

    public DefaultRunContext(
            Frontier frontier,
            RetryBuffer retryBuffer,
            DeadLetterQueue dlq,
            DecisionLog decisionLog,
            DynamicAttributes attributes,
            ProcessingRouteResolver routes,
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
        this.attributes = attributes;
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
    public DynamicAttributes attributes() {
        return attributes;
    }

    @Override
    public ProcessingRouteResolver routes() {
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
