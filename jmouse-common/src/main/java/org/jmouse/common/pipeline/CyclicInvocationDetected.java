package org.jmouse.common.pipeline;

public class CyclicInvocationDetected extends RuntimeException  {

    public CyclicInvocationDetected() {
        super();
    }

    public CyclicInvocationDetected(String message) {
        super(message);
    }

    public CyclicInvocationDetected(String message, Throwable cause) {
        super(message, cause);
    }

    public CyclicInvocationDetected(Throwable cause) {
        super(cause);
    }

}
