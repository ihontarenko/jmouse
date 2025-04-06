package org.jmouse.el.core.node.expression;

import org.jmouse.el.core.evaluation.EvaluationException;

public class FunctionNotFoundException extends EvaluationException {

    public FunctionNotFoundException(String message) {
        super(message);
    }

    public FunctionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
