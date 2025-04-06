package org.jmouse.el.core.evaluation;

public class EvaluationException extends RuntimeException {

    public EvaluationException(String message) {
        super(message);
    }

    public EvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

}
