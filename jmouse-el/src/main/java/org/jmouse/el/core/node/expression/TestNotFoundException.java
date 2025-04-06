package org.jmouse.el.core.node.expression;

import org.jmouse.el.core.evaluation.EvaluationException;

public class TestNotFoundException extends EvaluationException {

    public TestNotFoundException(String message) {
        super(message);
    }

    public TestNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
