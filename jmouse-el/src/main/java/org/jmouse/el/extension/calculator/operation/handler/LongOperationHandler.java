package org.jmouse.el.extension.calculator.operation.handler;

import org.jmouse.el.extension.calculator.operation.IllegalOperationException;
import org.jmouse.el.extension.calculator.operation.OperationHandler;
import org.jmouse.el.extension.calculator.operation.OperationType;

import static org.jmouse.el.extension.calculator.operation.OperationType.DIVIDE;
import static org.jmouse.el.extension.calculator.operation.OperationType.MODULUS;

/**
 * {@link OperationHandler} that performs long‐based arithmetic.
 * <p>
 * Supports binary operations (PLUS, MINUS, MULTIPLY, DIVIDE, MODULUS, EXPONENTIAL)
 * where the left operand is an {@link Long} and the right operand is any {@link Number},
 * as well as unary operations (INCREMENT, DECREMENT).
 * </p>
 */
public class LongOperationHandler implements OperationHandler<Long, Number> {

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
        boolean supported = x != null && Long.class.isAssignableFrom(x);

        if (supported && y != null && !Number.class.isAssignableFrom(y)) {
            supported = false;
        }

        return supported;
    }

    /**
     * Executes the specified integer operation.
     *
     * @param type      the operation type
     * @param x         the left operand (non‐null {@link Long})
     * @param operand   the right operand (may be {@link Number} or {@code null} for unary ops)
     * @return the result as {@link Long} for integer ops or {@link Double} for exponentiation
     * @throws IllegalOperationException if division or modulus by zero is attempted
     */
    @Override
    public Object execute(OperationType type, Long x, Number operand) {
        long y = (operand == null ? 0L : operand.longValue());

        // Check division/modulus by zero
        if ((type == DIVIDE || type == MODULUS) && y == 0) {
            throw new IllegalOperationException("%s by zero".formatted(type));
        }

        return switch (type) {
            case PLUS        -> x + y;
            case MINUS       -> x - y;
            case MULTIPLY    -> x * y;
            case DIVIDE      -> x / y;
            case MODULUS     -> x % y;
            case EXPONENTIAL -> Math.pow(x, y);
            case INCREMENT   -> x + 1;
            case DECREMENT   -> x - 1;
        };
    }

}
