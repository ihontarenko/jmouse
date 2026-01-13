package org.jmouse.common.pipeline;

public class InvalidProcessorReturnException extends PipelineRuntimeException {

    public InvalidProcessorReturnException() {
        super();
    }

    public InvalidProcessorReturnException(String message) {
        super(message);
    }

    public InvalidProcessorReturnException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidProcessorReturnException(Throwable cause) {
        super(cause);
    }
}
