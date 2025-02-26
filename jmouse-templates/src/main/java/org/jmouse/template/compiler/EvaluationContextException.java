package org.jmouse.template.compiler;

public class EvaluationContextException extends RuntimeException {

    public EvaluationContextException() {
        super();
    }

    public EvaluationContextException(String message) {
        super(message);
    }

    public EvaluationContextException(String message, Throwable cause) {
        super(message, cause);
    }

    public EvaluationContextException(Throwable cause) {
        super(cause);
    }

}
