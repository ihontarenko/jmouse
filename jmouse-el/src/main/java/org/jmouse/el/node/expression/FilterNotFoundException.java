package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationException;

public class FilterNotFoundException extends EvaluationException {

    public FilterNotFoundException(String message) {
        super(message);
    }

    public FilterNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
