package org.jmouse.el.extension.calculator.operation.handler;

import org.jmouse.el.extension.calculator.operation.IllegalOperationException;
import org.jmouse.el.extension.calculator.operation.OperationHandler;
import org.jmouse.el.extension.calculator.operation.OperationType;

import java.util.List;

import static org.jmouse.el.extension.calculator.operation.OperationType.MULTIPLY;
import static org.jmouse.el.extension.calculator.operation.OperationType.PLUS;

public class StringNumberOperationHandler implements OperationHandler<String, Number> {

    @Override
    public boolean supports(OperationType type, Class<?> x, Class<?> y) {
        boolean supported = x != null && String.class.isAssignableFrom(x);

        if (supported && y != null && !Number.class.isAssignableFrom(y)) {
            supported = false;
        }

        return supported;
    }

    @Override
    public Object execute(OperationType type, String text, Number number) {
        return switch (type) {
            case PLUS -> text.concat(number.toString());
            case MULTIPLY -> text.repeat(number.intValue());
            default -> throw new IllegalOperationException(
                    "Operator '%s' can't be applied to string. Only %s can be applied."
                            .formatted(type, List.of(MULTIPLY, PLUS)));
        };
    }
}
