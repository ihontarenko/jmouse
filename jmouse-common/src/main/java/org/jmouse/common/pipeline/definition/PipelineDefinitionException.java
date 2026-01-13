package org.jmouse.common.pipeline.definition;

public class PipelineDefinitionException extends RuntimeException {

    public PipelineDefinitionException(String message) {
        super(message);
    }

    public PipelineDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PipelineDefinitionException(Throwable cause) {
        super(cause);
    }
}
