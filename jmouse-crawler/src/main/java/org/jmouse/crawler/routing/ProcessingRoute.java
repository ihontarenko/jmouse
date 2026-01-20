package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.ProcessingTask;
import org.jmouse.crawler.runtime.RunContext;

public interface ProcessingRoute {

    String id();

    ProcessingPipeline pipeline();

    boolean matches(ProcessingTask task, RunContext run);

}