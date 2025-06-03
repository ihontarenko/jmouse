package org.jmouse.el.extension.calculator.operation;

import org.jmouse.core.convert.ClassPair;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade for registering and executing operation handlers.
 * <p>
 * Maintains a list of {@link OperationHandler}s and dispatches
 * unary or binary operations to the first handler that declares support.
 * </p>
 */
public class Calculation {

    private final List<OperationHandler<?, ?>> handlers;

    /**
     * Creates an empty Calculation instance with no registered handlers.
     */
    public Calculation() {
        handlers = new ArrayList<>();
    }

    /**
     * Registers a new operation handler.
     *
     * @param handler the handler to register
     */
    public void register(OperationHandler<?, ?> handler) {
        handlers.add(handler);
    }

    /**
     * Executes a binary operation.
     *
     * @param type the operation type (e.g. PLUS, MINUS)
     * @param x    the left operand (must not be null)
     * @param y    the right operand (must not be null)
     * @return the result of the operation
     * @throws CalculationOperationException if either operand is null
     *                                       or if no handler supports the given operands
     */
    public Object binary(OperationType type, Object x, Object y) {
        if (x == null) {
            throw new CalculationOperationException("Operand 'x' is null for binary operation");
        }

        if (y == null) {
            throw new CalculationOperationException("Operand 'y' is null for binary operation");
        }

        return getOperationHandler(type, x.getClass(), y.getClass()).execute(type, x, y);
    }

    /**
     * Executes a unary operation.
     *
     * @param type the operation type (INCREMENT or DECREMENT)
     * @param x    the operand (must not be null)
     * @return the result of the operation
     * @throws CalculationOperationException if the operand is null
     *                                       or if no handler supports the given operand
     */
    public Object unary(OperationType type, Object x) {
        if (x == null) {
            throw new CalculationOperationException("Operand 'x' is null for unary operation");
        }

        return getOperationHandler(type, x.getClass(), null).execute(type, x, null);
    }

    /**
     * Finds the first registered handler that supports the given operation
     * type and operand classes.
     *
     * @param type  the operation type
     * @param xType the class of the left operand
     * @param yType the class of the right operand (or null for unary)
     * @return the matching {@link OperationHandler}
     * @throws CalculationOperationException if no suitable handler is found
     */
    @SuppressWarnings("unchecked")
    public OperationHandler<Object, Object> getOperationHandler(OperationType type, Class<?> xType, Class<?> yType) {
        OperationHandler<Object, Object> handler = null;

        for (OperationHandler<?, ?> candidate : handlers) {
            if (candidate.supports(type, xType, yType)) {
                handler = (OperationHandler<Object, Object>) candidate;
                break;
            }
        }

        if (handler == null) {
            throw new CalculationOperationException(
                    "No operator-handler registered for: %s(%s)"
                            .formatted(type, new ClassPair(xType, yType)));
        }

        return handler;
    }

}
