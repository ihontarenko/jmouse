package org.jmouse.crawler.route;

import org.jmouse.crawler.api.RunContext;
import org.jmouse.crawler.api.ProcessingTask;

public interface ProcessingRouteResolver {
    ProcessingRoute resolve(ProcessingTask task, RunContext run);
}