package org.jmouse.crawler.routing;

public interface PipelineControlPolicy {
    boolean shouldStop(PipelineResult result, String stepId);
}