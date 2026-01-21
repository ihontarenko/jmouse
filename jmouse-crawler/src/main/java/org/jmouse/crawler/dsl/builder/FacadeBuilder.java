package org.jmouse.crawler.dsl.builder;

import org.jmouse.core.Verify;
import org.jmouse.crawler.api.*;
import org.jmouse.crawler.dsl.factory.Runners;
import org.jmouse.crawler.dsl.factory.Schedulers;
import org.jmouse.crawler.route.ProcessingRouteResolver;
import org.jmouse.crawler.runtime.core.SimpleProcessingEngine;
import org.jmouse.crawler.api.UtilityRegistry;
import org.jmouse.crawler.runtime.core.TaskFactory;
import org.jmouse.crawler.runtime.runner.JobRunner;
import org.jmouse.crawler.runtime.schedule.JobScheduler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class FacadeBuilder {

    private final List<SeedSpecification> seeds          = new ArrayList<>();
    private final RunContextBuilder       runtimeBuilder = new RunContextBuilder();
    private final RoutesBuilder     routesBuilder    = new RoutesBuilder();
    private final UtilitiesBuilder  utilitiesBuilder = new UtilitiesBuilder();

    private CrawlRunnerFactory runnerFactory    = Runners.singleThread();
    private SchedulerFactory   schedulerFactory = Schedulers.defaultScheduler();

    private FacadeBuilder() {}

    public static FacadeBuilder create() {
        return new FacadeBuilder();
    }

    private record SeedSpecification(URI url, RoutingHint hint) {}

    public FacadeBuilder seed(String url, RoutingHint hint) {
        return seed(URI.create(Verify.nonNull(url, "url")), hint);
    }

    public FacadeBuilder seed(URI url, RoutingHint hint) {
        Verify.nonNull(url, "url");
        seeds.add(new SeedSpecification(url, hint));
        return this;
    }

    public FacadeBuilder runtime(Consumer<RunContextBuilder> customizer) {
        Verify.nonNull(customizer, "customizer").accept(runtimeBuilder);
        return this;
    }

    public FacadeBuilder routes(Consumer<RoutesBuilder> customizer) {
        Verify.nonNull(customizer, "customizer").accept(routesBuilder);
        return this;
    }

    public FacadeBuilder utilities(Consumer<UtilitiesBuilder> customizer) {
        Verify.nonNull(customizer, "customizer").accept(utilitiesBuilder);
        return this;
    }

    public FacadeBuilder runner(CrawlRunnerFactory runnerFactory) {
        this.runnerFactory = Verify.nonNull(runnerFactory, "runnerFactory");
        return this;
    }

    public FacadeBuilder scheduler(SchedulerFactory schedulerFactory) {
        this.schedulerFactory = Verify.nonNull(schedulerFactory, "schedulerFactory");
        return this;
    }

    public Crawler build() {
        runtimeBuilder.ensureDefaults();

        ProcessingRouteResolver routeResolver = routesBuilder.build();
        UtilityRegistry         utilities     = utilitiesBuilder.build();
        RunContext              runContext    = runtimeBuilder.build(routeResolver, utilities);

        JobScheduler     scheduler = schedulerFactory.create(runContext);
        JobRunner        runner    = runnerFactory.create(runContext, scheduler);
        ProcessingEngine engine    = new SimpleProcessingEngine(runContext);

        Frontier    frontier = runContext.frontier();
        TaskFactory tasks    = runContext.tasks();

        for (SeedSpecification seed : seeds) {
            frontier.offer(
                    tasks.seed(seed.url(), seed.hint(), TaskOrigin.seed("seed"))
            );
        }

        return new Crawler(engine, runner);
    }
}
