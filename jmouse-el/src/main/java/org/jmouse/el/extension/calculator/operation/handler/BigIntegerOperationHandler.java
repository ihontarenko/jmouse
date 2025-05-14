package org.jmouse.el.extension.calculator.operation.handler;

import org.jmouse.el.extension.calculator.operation.IllegalOperationException;
import org.jmouse.el.extension.calculator.operation.OperationHandler;
import org.jmouse.el.extension.calculator.operation.OperationType;

import java.math.BigInteger;

import static org.jmouse.el.extension.calculator.operation.OperationType.*;

public class BigIntegerOperationHandler implements OperationHandler<BigInteger, Number> {

    @Override
    public boolean supports(OperationType type, Class<?> x, Class<?> y) {
        boolean supported = x != null && BigInteger.class.isAssignableFrom(x);

        if (supported && y != null && !Number.class.isAssignableFrom(y)) {
            supported = false;
        }

        return supported;
    }

    @Override
    public Object execute(OperationType type, BigInteger x, Number right) {
        BigInteger y = (right == null ? BigInteger.ZERO : BigInteger.valueOf(right.longValue()));

        if ((type == DIVIDE || type == MODULUS) && BigInteger.ZERO.equals(y)) {
            throw new IllegalOperationException("%s by zero".formatted(type));
        } else if (type == EXPONENTIAL && y.intValue() < 0) {
            throw new IllegalOperationException(
                    "Exponential operation with negative value %d (raw: %s)".formatted(y.intValue(), y));
        }

        return switch (type) {
            // binary operations
            case PLUS        -> x.add(y);
            case MINUS       -> x.subtract(y);
            case MULTIPLY    -> x.multiply(y);
            case DIVIDE      -> x.divide(y);
            case MODULUS     -> x.remainder(y);
            case EXPONENTIAL -> x.pow(y.intValue());
            // unary operations
            case INCREMENT   -> x.add(BigInteger.ONE);
            case DECREMENT   -> x.subtract(BigInteger.ONE);
        };
    }

}
