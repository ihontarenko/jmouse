package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.ProcessingContext;

public interface CrawlPipeline {
    String id();
    PipelineResult execute(ProcessingContext ctx) throws Exception;
}