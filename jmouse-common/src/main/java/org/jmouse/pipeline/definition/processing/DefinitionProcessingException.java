package org.jmouse.pipeline.definition.processing;

public final class DefinitionProcessingException extends RuntimeException {
    public DefinitionProcessingException(String message) { super(message); }
    public DefinitionProcessingException(String message, Throwable cause) { super(message, cause); }
}