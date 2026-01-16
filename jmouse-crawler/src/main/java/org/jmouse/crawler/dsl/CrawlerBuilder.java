package org.jmouse.crawler.dsl;

import org.jmouse.core.Verify;
import org.jmouse.crawler.routing.CrawlRouteResolver;
import org.jmouse.crawler.runtime.CrawlTask;
import org.jmouse.crawler.runtime.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class CrawlerBuilder {

    private final List<CrawlTask> seeds = new ArrayList<>();

    private final CrawlerRuntimeBuilder runtimeBuilder   = new CrawlerRuntimeBuilder();
    private final RoutesBuilder         routesBuilder    = new RoutesBuilder();
    private final UtilitiesBuilder      utilitiesBuilder = new UtilitiesBuilder();

    private CrawlRunnerFactory runnerFactory    = Runners.singleThread();
    private SchedulerFactory   schedulerFactory = Schedulers.defaultScheduler();

    private CrawlerBuilder() {}

    public static CrawlerBuilder create() {
        return new CrawlerBuilder();
    }

    public CrawlerBuilder seed(String url, Object hint) {
        return seed(URI.create(Verify.nonNull(url, "url")), hint);
    }

    public CrawlerBuilder seed(URI url, Object hint) {
        Verify.nonNull(url, "url");

        seeds.add(new CrawlTask(
                url,
                0,
                null,
                "seed",
                0,
                runtimeBuilder.clock().instant(),
                0,
                runtimeBuilder.hints().adapt(hint)
        ));

        return this;
    }

    public CrawlerBuilder runtime(Consumer<CrawlerRuntimeBuilder> customizer) {
        Verify.nonNull(customizer, "customizer").accept(runtimeBuilder);
        return this;
    }

    public CrawlerBuilder routes(Consumer<RoutesBuilder> customizer) {
        Verify.nonNull(customizer, "customizer").accept(routesBuilder);
        return this;
    }

    public CrawlerBuilder utilities(Consumer<UtilitiesBuilder> customizer) {
        Verify.nonNull(customizer, "customizer").accept(utilitiesBuilder);
        return this;
    }

    public CrawlerBuilder runner(CrawlRunnerFactory runnerFactory) {
        this.runnerFactory = Verify.nonNull(runnerFactory, "runnerFactory");
        return this;
    }

    public CrawlerBuilder scheduler(SchedulerFactory schedulerFactory) {
        this.schedulerFactory = Verify.nonNull(schedulerFactory, "schedulerFactory");
        return this;
    }

    public Crawler build() {
        runtimeBuilder.ensureDefaults();

        CrawlRouteResolver routeResolver = routesBuilder.build();
        UtilityRegistry    utilities     = utilitiesBuilder.build();
        CrawlRunContext    runContext    = runtimeBuilder.build(routeResolver, utilities);
        CrawlScheduler     scheduler     = schedulerFactory.create(runContext);
        CrawlRunner        runner        = runnerFactory.create(runContext, scheduler);
        CrawlEngine        engine        = new SimpleCrawlEngine(runContext);

        for (CrawlTask seed : seeds) {
            engine.submit(seed);
        }

        return new Crawler(engine, runner);
    }
}

