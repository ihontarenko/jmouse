package org.jmouse.crawler.dsl.builder;

import org.jmouse.core.Customizer;
import org.jmouse.core.Verify;
import org.jmouse.crawler.api.*;
import org.jmouse.crawler.dsl.factory.ParserRegistries;
import org.jmouse.crawler.route.ProcessingRouteResolver;
import org.jmouse.crawler.runtime.RetryPolicies;
import org.jmouse.crawler.runtime.ScopePolicies;
import org.jmouse.crawler.runtime.core.DefaultRunContext;
import org.jmouse.crawler.runtime.core.DefaultTaskFactory;
import org.jmouse.crawler.runtime.core.TaskFactory;
import org.jmouse.crawler.runtime.politeness.defaults.NoopPolitenessPolicy;
import org.jmouse.crawler.runtime.queue.FifoFrontier;
import org.jmouse.crawler.runtime.queue.InMemoryRetryBuffer;
import org.jmouse.crawler.runtime.dlq.InMemoryDeadLetterQueue;
import org.jmouse.crawler.runtime.state.*;

import org.jmouse.crawler.runtime.state.persistence.*;

import org.jmouse.crawler.runtime.state.persistence.FileSnapshotRepository;
import org.jmouse.crawler.runtime.state.persistence.events.InFlightEvent;
import org.jmouse.crawler.runtime.state.persistence.file.FileWalRepository;
import org.jmouse.crawler.runtime.state.persistence.snapshot.InFlightSnapshot;
import org.jmouse.crawler.runtime.state.persistence.wrap.PersistentInFlightBuffer;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

public final class RunContextBuilder {

    private Frontier        frontier;
    private RetryBuffer     retryBuffer;
    private DeadLetterQueue deadLetterQueue;

    private Fetcher        fetcher;
    private ParserRegistry parserRegistry;
    private DecisionLog    decisionLog;
    private DynamicAttributes attributes;

    private SeenStore        seenStore;
    private ScopePolicy      scopePolicy;
    private RetryPolicy      retryPolicy;
    private PolitenessPolicy politenessPolicy;

    private TaskFactory    tasks;
    private InFlightBuffer inFlight;

    private Clock clock;

    // persistence
    private PersistenceConfig  persistence;
    private StateBootstrapper  bootstrapper;

    /* ===================== setters ===================== */

    public RunContextBuilder frontier(Frontier value) {
        this.frontier = Verify.nonNull(value, "frontier");
        return this;
    }

    public RunContextBuilder retryBuffer(RetryBuffer value) {
        this.retryBuffer = Verify.nonNull(value, "retryBuffer");
        return this;
    }

    public RunContextBuilder deadLetterQueue(DeadLetterQueue value) {
        this.deadLetterQueue = Verify.nonNull(value, "deadLetterQueue");
        return this;
    }

    public RunContextBuilder decisionLog(DecisionLog value) {
        this.decisionLog = Verify.nonNull(value, "decisionLog");
        return this;
    }

    public RunContextBuilder dynamicAttributes(DynamicAttributes attributes) {
        this.attributes = Verify.nonNull(attributes, "attributes");
        return this;
    }

    public RunContextBuilder fetcher(Fetcher value) {
        this.fetcher = Verify.nonNull(value, "fetcher");
        return this;
    }

    public RunContextBuilder parsers(ParserRegistry value) {
        this.parserRegistry = Verify.nonNull(value, "parserRegistry");
        return this;
    }

    public RunContextBuilder seen(SeenStore value) {
        this.seenStore = Verify.nonNull(value, "seenStore");
        return this;
    }

    public RunContextBuilder scope(ScopePolicy value) {
        this.scopePolicy = Verify.nonNull(value, "scopePolicy");
        return this;
    }

    public RunContextBuilder retry(RetryPolicy value) {
        this.retryPolicy = Verify.nonNull(value, "retryPolicy");
        return this;
    }

    public RunContextBuilder politeness(PolitenessPolicy value) {
        this.politenessPolicy = Verify.nonNull(value, "politenessPolicy");
        return this;
    }

    public RunContextBuilder politeness(Customizer<LanePolitenessBuilder> customizer) {
        LanePolitenessBuilder builder = new LanePolitenessBuilder();
        builder.fallback(Duration.ZERO);
        Verify.nonNull(customizer, "customizer").customize(builder);
        this.politenessPolicy = builder.build();
        return this;
    }

    public RunContextBuilder clock(Clock value) {
        this.clock = Verify.nonNull(value, "clock");
        return this;
    }

    public RunContextBuilder tasks(TaskFactory tasks) {
        this.tasks = Verify.nonNull(tasks, "tasks");
        return this;
    }

    public RunContextBuilder inFlight(InFlightBuffer inFlight) {
        this.inFlight = Verify.nonNull(inFlight, "inFlight");
        return this;
    }

    /**
     * Enable persistence via client-side configuration.
     *
     * <p>When enabled, builder will wrap selected runtime stores with persistent decorators
     * and expose {@link #bootstrapper()} for restore/checkpoint hooks.</p>
     */
    public RunContextBuilder persistence(Customizer<PersistenceBuilder> customizer) {
        PersistenceBuilder builder = new PersistenceBuilder();
        Verify.nonNull(customizer, "customizer").customize(builder);
        this.persistence = builder.build();
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

        if (attributes == null) {
            attributes = new InMemoryDynamicAttributes();
        }

        if (tasks == null) {
            // keep your current generator style
            tasks = new DefaultTaskFactory(clock, () -> TaskId.random().value());
        }

        if (inFlight == null) {
            inFlight = new InMemoryInFlightBuffer();
        }

        Verify.state(fetcher != null, "Fetcher must be configured via runtime.fetcher(...)");

        if (persistence != null) {
            applyPersistence(persistence);
        }
    }

    public Clock clock() {
        return clock == null ? Clock.systemUTC() : clock;
    }

    /**
     * Expose bootstrapper to be called by FacadeBuilder:
     * <pre>
     *   runtimeBuilder.bootstrapper().restore();
     * </pre>
     */
    public StateBootstrapper bootstrapper() {
        return bootstrapper;
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
                attributes,
                routes,
                fetcher,
                parserRegistry,
                seenStore,
                scopePolicy,
                retryPolicy,
                clock,
                utilities,
                politenessPolicy,
                tasks,
                inFlight
        );
    }

    /* ===================== persistence wiring ===================== */

    private void applyPersistence(PersistenceConfig persistenceConfig) {
        Path directory = persistenceConfig.directory();

        WalRepository<InFlightEvent> wal = new FileWalRepository<>(
                directory.resolve("inflight.wal"),
                persistenceConfig.codec(),
                InFlightEvent.class,
                persistenceConfig.durability()
        );

        SnapshotRepository<InFlightSnapshot> snapshots = new FileSnapshotRepository<>(
                directory.resolve("inflight.snapshot"),
                persistenceConfig.codec(),
                InFlightSnapshot.class,
                InFlightSnapshot.empty()
        );

        this.inFlight = new PersistentInFlightBuffer(
                this.inFlight,
                wal,
                snapshots,
                persistenceConfig.snapshotPolicy(),
                this.clock
        );

        this.bootstrapper = new DefaultStateBootstrapper(this.frontier, this.inFlight);
    }
}
