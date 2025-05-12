package org.jmouse.el.extension.calculator.operation.handler;

import org.jmouse.el.extension.calculator.operation.IllegalOperationException;
import org.jmouse.el.extension.calculator.operation.OperationHandler;
import org.jmouse.el.extension.calculator.operation.OperationType;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.jmouse.el.extension.calculator.operation.OperationType.DIVIDE;
import static org.jmouse.el.extension.calculator.operation.OperationType.MODULUS;

public class BigDecimalOperationHandler implements OperationHandler<BigDecimal, Number> {

    @Override
    public boolean supports(OperationType type, Class<?> x, Class<?> y) {
        boolean supported = x != null && BigDecimal.class.isAssignableFrom(x);

        if (supported && y != null && !Number.class.isAssignableFrom(y)) {
            supported = false;
        }

        return supported;
    }

    @Override
    public Object execute(OperationType type, BigDecimal x, Number right) {
        BigDecimal y = (right == null ? BigDecimal.ZERO : BigDecimal.valueOf(right.doubleValue()));

        if ((type == DIVIDE || type == MODULUS) && BigDecimal.ZERO.equals(y)) {
            throw new IllegalOperationException("%s by zero".formatted(type));
        }

        return switch (type) {
            // binary operations
            case PLUS        -> x.add(y);
            case MINUS       -> x.subtract(y);
            case MULTIPLY    -> x.multiply(y);
            case DIVIDE      -> x.divide(y, 16, RoundingMode.HALF_EVEN);
            case MODULUS     -> x.remainder(y);
            case EXPONENTIAL -> x.pow(y.intValue());
            // unary operations
            case INCREMENT   -> x.add(BigDecimal.ONE);
            case DECREMENT   -> x.subtract(BigDecimal.ONE);
        };
    }

}
