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

        Object left = promote(x, y);
        Object right = promote(y, x);

        return getOperationHandler(type, left.getClass(), right.getClass()).execute(type, left, right);
    }

    /**
     * Widens a whole number when the other operand is fractional.
     *
     * <h2>⚠️ Why arithmetic was ORDER-DEPENDENT and silently wrong without this</h2>
     *
     * <p>A handler is chosen by the <em>left</em> operand's type, and every narrow handler accepts any
     * {@link Number} on the right by calling {@code intValue()} on it. So the fractional part of the
     * right operand was thrown away whenever the left one was whole:</p>
     *
     * <pre>
     *   500 * 0.8  →  0        ← 0.8 truncated to 0
     *   0.8 * 500  →  400.0    ← the same question, the other way round
     *   500 + 0.8  →  500
     * </pre>
     *
     * <p>⚠️ Nothing raised. {@code price * quantity} and {@code quantity * price} answered differently,
     * and a query language multiplying a decimal price by a whole count is the ordinary case rather than
     * an exotic one.</p>
     *
     * <p>Widening here rather than in each handler keeps the rule in one place: a handler still sees a
     * pair it understands, and the promotion is visible where somebody looking for it would look.</p>
     *
     * @param value the operand to widen
     * @param other the operand it is being combined with
     * @return the operand, widened where the other one is fractional
     */
    private Object promote(Object value, Object other) {
        boolean whole = value instanceof Integer || value instanceof Long
                        || value instanceof Short || value instanceof Byte;

        if (!whole) {
            return value;
        }

        if (other instanceof java.math.BigDecimal) {
            return java.math.BigDecimal.valueOf(((Number) value).longValue());
        }

        if (other instanceof Double || other instanceof Float) {
            return ((Number) value).doubleValue();
        }

        return value;
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
