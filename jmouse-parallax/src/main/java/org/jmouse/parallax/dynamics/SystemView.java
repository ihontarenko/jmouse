package org.jmouse.parallax.dynamics;

import org.jmouse.parallax.numerics.OdeIntegrator;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition.DifferentialSystem;
import org.jmouse.parallax.object.ObjectDefinition.DiscreteMap;

import java.util.ArrayList;
import java.util.List;

/**
 * A dynamical system ready to step: dX/dt = F(X, t) for a differential system, X ↦ G(X, n) for a map. It also
 * gives the exact Jacobian, from which equilibria are classified.
 */
final class SystemView {

    private final List<String> variables;
    private final List<String> sources;
    private final List<String> arguments;
    private final List<BoundFunction> functions;
    private final List<Interval> bounds;
    private final boolean continuous;
    private final CompiledObject object;
    private final double[] buffer;
    private final double[] declaredStart;

    private SystemView(CompiledObject object, List<String> variables, List<String> sources, List<Interval> bounds,
                       List<String> starts, boolean continuous) {
        this.object = object;
        this.declaredStart = starts.stream().mapToDouble(object::constant).toArray();
        this.variables = variables;
        this.sources = sources;
        this.bounds = bounds;
        this.continuous = continuous;
        this.arguments = new ArrayList<>(variables);
        arguments.add(continuous ? "t" : "n");
        this.functions = object.functions(sources, arguments);
        this.buffer = new double[arguments.size()];
    }

    static SystemView of(CompiledObject object) {
        return switch (object.definition()) {
            case DifferentialSystem system -> new SystemView(object, system.variables(), system.equations(), system.bounds(),
                    system.starts(), true);
            case DiscreteMap map -> new SystemView(object, map.variables(), withStepValues(map), map.bounds(), map.starts(),
                    false);
            default -> throw new IllegalArgumentException("This object is not a dynamical system.");
        };
    }

    /**
     * The updates with the step's values in front of each, as jMF local definitions: a deterministic step value is
     * the same number wherever it is read, so it can be written into every update — and differentiated there.
     */
    private static List<String> withStepValues(DiscreteMap map) {
        if (map.stepValues().isEmpty()) {
            return map.updates();
        }
        String values = String.join("; ", map.stepValues().stream()
                .map(value -> value.name() + " = " + value.expression()).toList());
        return map.updates().stream().map(update -> values + "; " + update).toList();
    }

    int dimension() {
        return variables.size();
    }

    List<String> variables() {
        return variables;
    }

    List<Interval> bounds() {
        return bounds;
    }

    boolean continuous() {
        return continuous;
    }

    /** F(X, t) for a flow, G(X, n) for a map. */
    void apply(double time, double[] state, double[] target) {
        System.arraycopy(state, 0, buffer, 0, state.length);
        buffer[state.length] = time;
        for (int index = 0; index < functions.size(); index++) {
            target[index] = functions.get(index).at(buffer);
        }
    }

    /** Where a path starts: the state a representation was asked to start from, else the one the document declares. */
    double[] initialOr(double[] given) {
        if (given.length == dimension()) {
            return given;
        }
        if (given.length != 0) {
            throw new IllegalArgumentException("The initial state has " + dimension() + " values, one per variable "
                    + variables + ".");
        }
        return declaredStart.clone();
    }

    OdeIntegrator.Solution integrate(double[] initial, double from, double to, int samples,
                                     org.jmouse.parallax.ComputationProgress progress) {
        return OdeIntegrator.integrate(this::apply, initial, from, to, samples, 1e-8, 2_000_000, 1e9, progress);
    }

    /** Jacobian entries ∂(F or G)ᵢ/∂xⱼ, symbolic. */
    List<List<BoundFunction>> jacobian() {
        List<List<BoundFunction>> rows = new ArrayList<>();
        for (String source : sources) {
            List<BoundFunction> row = new ArrayList<>();
            for (String variable : variables) {
                row.add(object.function(object.derivativeExpression(source, variable), arguments));
            }
            rows.add(row);
        }
        return rows;
    }

    double[][] jacobianAt(List<List<BoundFunction>> jacobian, double[] state, double time) {
        double[] point = new double[arguments.size()];
        System.arraycopy(state, 0, point, 0, state.length);
        point[state.length] = time;
        double[][] matrix = new double[dimension()][dimension()];
        for (int row = 0; row < dimension(); row++) {
            for (int column = 0; column < dimension(); column++) {
                matrix[row][column] = jacobian.get(row).get(column).at(point);
            }
        }
        return matrix;
    }
}
