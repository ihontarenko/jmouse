package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.RoutingHint;
import org.jmouse.crawler.runtime.ProcessingTask;
import org.jmouse.crawler.runtime.RunContext;

public interface ProcessingRoute {

    String id();

    CrawlPipeline pipeline();

    boolean matches(ProcessingTask task, RunContext run);

    default boolean supportsHint(RoutingHint hint) {
        return false;
    }

}