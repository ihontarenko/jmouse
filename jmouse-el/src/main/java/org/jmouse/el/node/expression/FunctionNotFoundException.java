package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationException;

public class FunctionNotFoundException extends EvaluationException {

    public FunctionNotFoundException(String message) {
        super(message);
    }

    public FunctionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
