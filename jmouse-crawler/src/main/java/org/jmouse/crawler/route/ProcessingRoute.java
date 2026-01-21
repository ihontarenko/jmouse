package org.jmouse.crawler.route;

import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.RunContext;
import org.jmouse.crawler.pipeline.ProcessingPipeline;

public interface ProcessingRoute {

    String id();

    ProcessingPipeline pipeline();

    boolean matches(ProcessingTask task, RunContext run);

}