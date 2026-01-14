package org.jmouse.crawler.dsl;

import org.jmouse.crawler.runtime.*;

import java.util.concurrent.Executors;

public final class Runners {

    private Runners() {}

    public static CrawlRunner singleThread() {
        return new SingleThreadRunner();
    }

    public static CrawlRunner executor(int numberOfThreads, int maxInFlight) {
        return new ExecutorRunner(Executors.newFixedThreadPool(numberOfThreads), maxInFlight);
    }
}
