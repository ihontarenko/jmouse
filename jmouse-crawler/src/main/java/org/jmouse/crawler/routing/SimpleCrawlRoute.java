package org.jmouse.crawler.routing;

import org.jmouse.crawler.core.CrawlHint;
import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.runtime.CrawlRunContext;

import java.util.Set;

public final class SimpleCrawlRoute implements CrawlRoute {

    private final CrawlPipeline                   pipeline;
    private final String                          id;
    private final UrlMatch                        match;
    private final Set<Class<? extends CrawlHint>> hints;

    public SimpleCrawlRoute(String id, UrlMatch match, CrawlPipeline pipeline, Set<Class<? extends CrawlHint>> hints) {
        this.id = id;
        this.match = match;
        this.pipeline = pipeline;
        this.hints = hints;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public CrawlPipeline pipeline() {
        return pipeline;
    }

    @Override
    public boolean matches(CrawlTask task, CrawlRunContext run) {
        return match.test(task, run);
    }
}
