package org.jmouse.crawler.runtime;

public class Crawler {

    private final ProcessingEngine engine;
    private final CrawlRunner      runner;

    public Crawler(ProcessingEngine engine, CrawlRunner runner) {
        this.engine = engine;
        this.runner = runner;
    }

    public void runUntilDrained() {
        runner.runUntilDrained(engine);
    }

}
