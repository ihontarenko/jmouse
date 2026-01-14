package org.jmouse.crawler.routing;

import org.jmouse.crawler.runtime.CrawlProcessingContext;

@FunctionalInterface
public interface CrawlStep {
    PipelineResult execute(CrawlProcessingContext context) throws Exception;
}
