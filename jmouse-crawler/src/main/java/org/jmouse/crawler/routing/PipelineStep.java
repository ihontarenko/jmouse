package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.ProcessingContext;

@FunctionalInterface
public interface PipelineStep {
    PipelineResult execute(ProcessingContext context) throws Exception;
}
