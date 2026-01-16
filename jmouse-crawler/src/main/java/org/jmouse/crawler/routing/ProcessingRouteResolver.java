package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.RunContext;
import org.jmouse.crawler.runtime.ProcessingTask;

public interface ProcessingRouteResolver {
    ProcessingRoute resolve(ProcessingTask task, RunContext run);
}