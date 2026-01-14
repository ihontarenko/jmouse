package org.jmouse.crawler.runtime;

public class Crawler {

    private final CrawlEngine engine;
    private final CrawlRunner runner;

    public Crawler(CrawlEngine engine, CrawlRunner runner) {
        this.engine = engine;
        this.runner = runner;
    }

    public void runUntilDrained() {
        runner.runUntilDrained(engine);
    }

}
