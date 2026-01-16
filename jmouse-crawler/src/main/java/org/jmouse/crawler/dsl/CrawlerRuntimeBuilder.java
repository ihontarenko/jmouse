package org.jmouse.crawler.dsl;

import org.jmouse.core.Verify;
import org.jmouse.crawler.routing.ProcessingRouteResolver;
import org.jmouse.crawler.runtime.*;
import org.jmouse.crawler.runtime.InMemoryRetryBuffer;
import org.jmouse.crawler.spi.*;

import java.time.Clock;

public final class CrawlerRuntimeBuilder {

    // core runtime
    private Frontier        frontier;
    private RetryBuffer     retryBuffer;
    private DeadLetterQueue deadLetterQueue;

    // io
    private Fetcher        fetcher;
    private ParserRegistry parserRegistry;
    private DecisionLog    decisionLog;

    // policies
    private SeenStore        seenStore;
    private ScopePolicy      scopePolicy;
    private RetryPolicy      retryPolicy;
    private PolitenessPolicy politenessPolicy;

    // infra
    private Clock clock;

    private CrawlHintAdapter hintAdapter = CrawlHintAdapter.defaultAdapter();

    /* ===================== setters ===================== */

    public CrawlerRuntimeBuilder frontier(Frontier value) {
        this.frontier = Verify.nonNull(value, "frontier");
        return this;
    }

    public CrawlerRuntimeBuilder retryBuffer(RetryBuffer value) {
        this.retryBuffer = Verify.nonNull(value, "retryBuffer");
        return this;
    }

    public CrawlerRuntimeBuilder deadLetterQueue(DeadLetterQueue value) {
        this.deadLetterQueue = Verify.nonNull(value, "deadLetterQueue");
        return this;
    }

    public CrawlerRuntimeBuilder decisionLog(DecisionLog value) {
        this.decisionLog = Verify.nonNull(value, "decisionLog");
        return this;
    }

    public CrawlerRuntimeBuilder fetcher(Fetcher value) {
        this.fetcher = Verify.nonNull(value, "fetcher");
        return this;
    }

    public CrawlerRuntimeBuilder parsers(ParserRegistry value) {
        this.parserRegistry = Verify.nonNull(value, "parserRegistry");
        return this;
    }

    public CrawlerRuntimeBuilder seen(SeenStore value) {
        this.seenStore = Verify.nonNull(value, "seenStore");
        return this;
    }

    public CrawlerRuntimeBuilder scope(ScopePolicy value) {
        this.scopePolicy = Verify.nonNull(value, "scopePolicy");
        return this;
    }

    public CrawlerRuntimeBuilder retry(RetryPolicy value) {
        this.retryPolicy = Verify.nonNull(value, "retryPolicy");
        return this;
    }

    public CrawlerRuntimeBuilder politeness(PolitenessPolicy value) {
        this.politenessPolicy = Verify.nonNull(value, "politenessPolicy");
        return this;
    }

    public CrawlerRuntimeBuilder clock(Clock value) {
        this.clock = Verify.nonNull(value, "clock");
        return this;
    }

    public CrawlerRuntimeBuilder hints(CrawlHintAdapter value) {
        this.hintAdapter = Verify.nonNull(value, "hintAdapter");
        return this;
    }

    /* ===================== defaults ===================== */

    void ensureDefaults() {
        if (clock == null) {
            clock = Clock.systemUTC();
        }

        if (frontier == null) {
            frontier = new FifoFrontier();
        }

        if (retryBuffer == null) {
            retryBuffer = new InMemoryRetryBuffer();
        }

        if (deadLetterQueue == null) {
            deadLetterQueue = new InMemoryDeadLetterQueue();
        }

        if (seenStore == null) {
            seenStore = new ConcurrentSeenStore();
        }

        if (scopePolicy == null) {
            scopePolicy = ScopePolicies.allowAll();
        }

        if (retryPolicy == null) {
            retryPolicy = RetryPolicies.simple();
        }

        if (politenessPolicy == null) {
            politenessPolicy = NoopPolitenessPolicy.INSTANCE;
        }

        if (parserRegistry == null) {
            parserRegistry = ParserRegistries.noop();
        }

        if (decisionLog == null) {
            decisionLog = new InMemoryDecisionLog();
        }

        Verify.state(fetcher != null,
                "Fetcher must be configured via runtime.fetcher(...)");
    }

    /* ===================== accessors ===================== */

    public CrawlHintAdapter hints() {
        return hintAdapter;
    }

    public Clock clock() {
        if (clock == null) {
            clock = Clock.systemUTC();
        }
        return clock;
    }

    /* ===================== build ===================== */

    RunContext build(ProcessingRouteResolver routes, UtilityRegistry utilities) {
        Verify.nonNull(routes, "routes");

        if (utilities == null) {
            utilities = UtilityRegistry.empty();
        }

        return new DefaultRunContext(
                frontier,
                retryBuffer,
                deadLetterQueue,
                decisionLog,
                routes,
                fetcher,
                parserRegistry,
                seenStore,
                scopePolicy,
                retryPolicy,
                clock,
                utilities,
                politenessPolicy
        );
    }
}
