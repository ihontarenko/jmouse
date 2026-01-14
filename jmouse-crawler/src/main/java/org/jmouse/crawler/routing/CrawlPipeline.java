package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.CrawlProcessingContext;

public interface CrawlPipeline {
    String id();
    PipelineResult execute(CrawlProcessingContext ctx) throws Exception;
}