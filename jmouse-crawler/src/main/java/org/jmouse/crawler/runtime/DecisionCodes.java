package org.jmouse.crawler.runtime;

public class DecisionCodes {
    public static final String PIPELINE_STEP_START  = "pipeline.step.start";
    public static final String PIPELINE_STEP_OK     = "pipeline.step.ok";
    public static final String PIPELINE_STEP_ERROR  = "pipeline.step.error";
    public static final String PIPELINE_STOP        = "pipeline.stop";
    public static final String DUPLICATE_DISCOVERED = "crawler.duplicate.discovered";
    public static final String DUPLICATE_PROCESSED  = "crawler.duplicate.processed";
    public static final String SCOPE_DENY           = "crawler.scope.denied";
    public static final String ENQUEUE_ACCEPT       = "enqueued";
    public static final String INVALID_URL          = "invalid.url";

    private DecisionCodes() {
    }
}
