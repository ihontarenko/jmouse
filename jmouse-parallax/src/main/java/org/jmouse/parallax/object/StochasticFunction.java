package org.jmouse.parallax.object;

import org.jmouse.formula.RealFunction;

import java.util.List;
import java.util.SplittableRandom;

/**
 * A compiled expression that calls random functions, with its parameter values in place: before every evaluation each
 * written call draws a fresh number from the source it is given — see {@link Randomness}.
 *
 * <p>Like {@link BoundFunction} it owns one argument buffer and is NOT thread-safe.</p>
 */
public final class StochasticFunction {

    private final RealFunction                   function;
    private final List<Randomness.BoundDraw>     draws;
    private final int                            variableCount;
    private final double[]                       buffer;

    StochasticFunction(RealFunction function, List<Randomness.BoundDraw> draws, int variableCount, double[] parameterValues) {
        this.function = function;
        this.draws = List.copyOf(draws);
        this.variableCount = variableCount;
        this.buffer = new double[draws.size() + variableCount + parameterValues.length];
        System.arraycopy(parameterValues, 0, buffer, draws.size() + variableCount, parameterValues.length);
    }

    /**
     * Evaluates at a point of the variables, drawing every random call first, in the order the calls are written.
     *
     * @param step the integrator's step, which {@code white_noise()} is drawn for
     */
    public double at(double[] point, SplittableRandom random, double step) {
        for (int index = 0; index < draws.size(); index++) {
            Randomness.BoundDraw draw = draws.get(index);
            buffer[index] = draw.draw().draw(random, draw.arguments(), step);
        }
        System.arraycopy(point, 0, buffer, draws.size(), variableCount);
        return function.evaluate(buffer);
    }

    public int variableCount() {
        return variableCount;
    }
}
