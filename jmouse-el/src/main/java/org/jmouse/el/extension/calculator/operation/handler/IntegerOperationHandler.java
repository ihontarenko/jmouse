package org.jmouse.el.extension.calculator.operation.handler;

import org.jmouse.el.extension.calculator.operation.IllegalOperationException;
import org.jmouse.el.extension.calculator.operation.OperationHandler;
import org.jmouse.el.extension.calculator.operation.OperationType;

import static org.jmouse.el.extension.calculator.operation.OperationType.DIVIDE;
import static org.jmouse.el.extension.calculator.operation.OperationType.MODULUS;

/**
 * {@link OperationHandler} that performs integer‐based arithmetic.
 * <p>
 * Supports binary operations (PLUS, MINUS, MULTIPLY, DIVIDE, MODULUS, EXPONENTIAL)
 * where the left operand is an {@link Integer} and the right operand is any {@link Number},
 * as well as unary operations (INCREMENT, DECREMENT).
 * </p>
 */
public class IntegerOperationHandler implements OperationHandler<Integer, Number> {

    /**
     * Determines if this handler supports the given operation type and operand classes.
     *
     * @param type the operation type to perform
     * @param x    the class of the left operand
     * @param y    the class of the right operand (may be {@code null} for unary ops)
     * @return {@code true} if this handler can process the given combination
     */
    @Override
    public boolean supports(OperationType type, Class<?> x, Class<?> y) {
        boolean supported = x != null && Integer.class.isAssignableFrom(x);

        if (supported && y != null && !Number.class.isAssignableFrom(y)) {
            supported = false;
        }

        return supported;
    }

    /**
     * Executes the specified integer operation.
     *
     * @param type the operation type
     * @param x    the left operand (non‐null {@link Integer})
     * @param y    the right operand (may be {@link Number} or {@code null} for unary ops)
     * @return the result as {@link Integer} for integer ops or {@link Double} for exponentiation
     * @throws IllegalOperationException if division or modulus by zero is attempted
     */
    @Override
    public Object execute(OperationType type, Integer x, Number y) {
        // Check division/modulus by zero
        if ((type == DIVIDE || type == MODULUS) && y.intValue() == 0) {
            throw new IllegalOperationException("%s by zero".formatted(type));
        }

        return switch (type) {
            case PLUS        -> x + y.intValue();
            case MINUS       -> x - y.intValue();
            case MULTIPLY    -> x * y.intValue();
            case DIVIDE      -> x / y.intValue();
            case MODULUS     -> x % y.intValue();
            case EXPONENTIAL -> Math.pow(x, y.doubleValue());
            case INCREMENT   -> x + 1;
            case DECREMENT   -> x - 1;
        };
    }

}
