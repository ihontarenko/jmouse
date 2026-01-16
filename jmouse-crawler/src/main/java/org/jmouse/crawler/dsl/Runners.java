package org.jmouse.crawler.dsl;

import org.jmouse.crawler.runtime.ExecutorRunner;
import org.jmouse.crawler.runtime.SingleThreadRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Runners {

    private Runners() {}

    public static CrawlRunnerFactory singleThread() {
        return (runContext, scheduler) -> new SingleThreadRunner(scheduler);
    }

    public static CrawlRunnerFactory executor(int threads, int maxInFlight) {
        return (runContext, scheduler) -> {
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            return new ExecutorRunner(scheduler, executor, maxInFlight);
        };
    }

    public static CrawlRunnerFactory executor(ExecutorService executor, int maxInFlight) {
        return (runContext, scheduler) -> new ExecutorRunner(scheduler, executor, maxInFlight);
    }
}
