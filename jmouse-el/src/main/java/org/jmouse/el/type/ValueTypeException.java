package org.jmouse.el.type;

import org.jmouse.el.evaluation.EvaluationException;

/** A value bound to a typed declaration that is not of its type and does not convert to it. */
public class ValueTypeException extends EvaluationException {

    public ValueTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    static ValueTypeException refused(ValueType type, Object value, Throwable cause) {
        return new ValueTypeException("'%s' (%s) is not %s and does not convert to it.".formatted(value,
                value.getClass().getSimpleName(), type.name()), cause);
    }
}
