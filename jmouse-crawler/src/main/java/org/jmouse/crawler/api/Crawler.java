package org.jmouse.crawler.api;

import org.jmouse.crawler.runtime.runner.JobRunner;

public class Crawler {

    private final ProcessingEngine engine;
    private final JobRunner        runner;

    public Crawler(ProcessingEngine engine, JobRunner runner) {
        this.engine = engine;
        this.runner = runner;
    }

    public void runUntilDrained() {
        runner.runUntilDrained(engine);
    }

}
