package org.jmouse.pipeline;

public class ProcessorInstantiationException extends RuntimeException {

    public ProcessorInstantiationException() {
        super();
    }

    public ProcessorInstantiationException(String message) {
        super(message);
    }

    public ProcessorInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessorInstantiationException(Throwable cause) {
        super(cause);
    }

}
