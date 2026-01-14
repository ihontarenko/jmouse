package org.jmouse.crawler.routing;

public record PipelineResult(String code, String stageId) {
    public static PipelineResult ok(String stageId) {
        return new PipelineResult("OK", stageId);
    }
}