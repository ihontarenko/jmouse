package org.jmouse.common.pipeline.runtime;

public final class PipelineCompilationException extends RuntimeException {
    public PipelineCompilationException(String message) { super(message); }
    public PipelineCompilationException(String message, Throwable cause) { super(message, cause); }
}