package org.jmouse.crawler.runtime;

public final class SingleThreadRunner implements CrawlRunner {
    public void runUntilDrained(CrawlEngine engine) {
        while (engine.tick()) {}
    }
}
