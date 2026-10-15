package org.jmouse.parallax.object;

import org.jmouse.formula.Formula;
import org.jmouse.formula.RealFunction;

/**
 * A compiled expression with its parameter values already in place, evaluated at a point of its variables.
 *
 * <p>It owns one argument buffer and is therefore NOT thread-safe: a computation that fans out across threads
 * takes a {@link #copy()} per thread. That trade is deliberate — a mesh of a million vertices would otherwise
 * allocate a million arrays.
 */
public final class BoundFunction {

    private final Formula expression;
    private final RealFunction function;
    private final int variableCount;
    private final double[] buffer;

    BoundFunction(Formula expression, RealFunction function, int variableCount, double[] parameterValues) {
        this.expression = expression;
        this.function = function;
        this.variableCount = variableCount;
        this.buffer = new double[variableCount + parameterValues.length];
        System.arraycopy(parameterValues, 0, buffer, variableCount, parameterValues.length);
    }

    public BoundFunction copy() {
        double[] parameters = new double[buffer.length - variableCount];
        System.arraycopy(buffer, variableCount, parameters, 0, parameters.length);
        return new BoundFunction(expression, function, variableCount, parameters);
    }

    public Formula expression() {
        return expression;
    }

    public double at() {
        return function.evaluate(buffer);
    }

    public double at(double first) {
        buffer[0] = first;
        return function.evaluate(buffer);
    }

    public double at(double first, double second) {
        buffer[0] = first;
        buffer[1] = second;
        return function.evaluate(buffer);
    }

    public double at(double first, double second, double third) {
        buffer[0] = first;
        buffer[1] = second;
        buffer[2] = third;
        return function.evaluate(buffer);
    }

    public double at(double first, double second, double third, double fourth) {
        buffer[0] = first;
        buffer[1] = second;
        buffer[2] = third;
        buffer[3] = fourth;
        return function.evaluate(buffer);
    }

    /** Evaluates at a point of all variables; the point's length must equal the variable count. */
    public double at(double[] point) {
        System.arraycopy(point, 0, buffer, 0, variableCount);
        return function.evaluate(buffer);
    }

    public int variableCount() {
        return variableCount;
    }
}
