package org.jmouse.crawler.dsl;

import org.jmouse.crawler.runtime.ExecutorRunner;
import org.jmouse.crawler.runtime.SingleThreadRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Runners {

    private Runners() {}

    public static CrawlRunnerFactory singleThread() {
        return (context, scheduler) -> new SingleThreadRunner(scheduler, context.clock());
    }

    public static CrawlRunnerFactory executor(int threads, int maxInFlight) {
        return (context, scheduler) -> {
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            return new ExecutorRunner(scheduler, context.clock(), executor, maxInFlight);
        };
    }

    public static CrawlRunnerFactory executor(ExecutorService executor, int maxInFlight) {
        return (context, scheduler) -> new ExecutorRunner(scheduler, context.clock(), executor, maxInFlight);
    }
}
