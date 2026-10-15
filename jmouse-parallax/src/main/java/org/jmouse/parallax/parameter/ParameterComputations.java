package org.jmouse.parallax.parameter;

import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.numerics.OdeIntegrator;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.DifferentialSystem;
import org.jmouse.parallax.object.ObjectDefinition.DiscreteMap;
import org.jmouse.parallax.object.ObjectDefinition.ExplicitCurve;
import org.jmouse.parallax.object.ObjectDefinition.ExplicitSurface;
import org.jmouse.parallax.object.ParameterDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parameters as coordinates: a sweep that maps behaviour over them, and descent paths over a landscape. */
public final class ParameterComputations {

    private ParameterComputations() {
    }

    public enum Behaviour { MAXIMUM, MINIMUM, MEAN, VALUE_AT_POINT, LYAPUNOV, PERIOD, ATTRACTOR_SIZE }

    /**
     * Runs the object once per point of a parameter grid (one or two parameters) and records one number per run:
     * a map of where the behaviour changes — the stability zones of a system, the Lyapunov exponent of the
     * logistic map, the period of its cycles.
     */
    public static class ParameterSweep implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.PARAMETER_SWEEP;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ExplicitCurve || definition instanceof ExplicitSurface
                    || definition instanceof DiscreteMap || definition instanceof DifferentialSystem;
        }

        /** A sweep varies a parameter: an object without one has nothing to sweep. */
        @Override
        public boolean accepts(ObjectDefinition definition, List<ParameterDefinition> parameters) {
            return accepts(definition) && !parameters.isEmpty();
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.text("first", "", 32, "First swept parameter; the object's first when empty"),
                    SettingSpecification.text("second", "", 32, "Second swept parameter; a one-dimensional sweep when empty"),
                    SettingSpecification.integer("steps", 120, 2, 1000, "Values per swept parameter"),
                    SettingSpecification.choice("behaviour", Behaviour.MAXIMUM, "What is measured per run"),
                    SettingSpecification.vector("point", List.of(), 0, -1e9, 1e9, "Where VALUE_AT_POINT reads, or the initial state"),
                    SettingSpecification.integer("iterations", 400, 10, 100_000, "Iterations or samples per run"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            long runs = (long) settings.integer("steps") * (settings.text("second").isBlank() ? 1 : settings.integer("steps"));
            long perRun = settings.integer("iterations") * (object.definition() instanceof DifferentialSystem ? 30L : 1L);
            return runs * perRun;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            if (object.parameters().isEmpty()) {
                throw new IllegalArgumentException("A sweep needs a parameter, and this object has none.");
            }
            ParameterDefinition first = parameter(object, settings.text("first"), object.parameters().getFirst().name());
            ParameterDefinition second = settings.text("second").isBlank() ? null : parameter(object, settings.text("second"), null);
            int steps = settings.integer("steps");
            int rows = second == null ? 1 : steps;
            Behaviour behaviour = settings.choice("behaviour", Behaviour.class);
            float[] values = new float[steps * rows];
            for (int row = 0; row < rows; row++) {
                progress.checkpoint((double) row / rows, "sweeping");
                for (int column = 0; column < steps; column++) {
                    Map<String, Double> point = new LinkedHashMap<>();
                    point.put(first.name(), first.minimum() + (first.maximum() - first.minimum()) * column / (steps - 1));
                    if (second != null) {
                        point.put(second.name(), second.minimum() + (second.maximum() - second.minimum()) * row / (rows - 1));
                    }
                    values[row * steps + column] = (float) measure(object.withParameters(point), behaviour, settings);
                }
            }
            double minimum = Double.POSITIVE_INFINITY;
            double maximum = Double.NEGATIVE_INFINITY;
            for (float value : values) {
                if (Float.isFinite(value)) {
                    minimum = Math.min(minimum, value);
                    maximum = Math.max(maximum, value);
                }
            }
            Interval secondRange = second == null ? Interval.of(0, 1) : Interval.of(second.minimum(), second.maximum());
            return new Dataset.ScalarGrid(steps, rows, Interval.of(first.minimum(), first.maximum()), secondRange, values,
                    Double.isFinite(minimum) ? minimum : 0, Double.isFinite(maximum) ? maximum : 0,
                    behaviour.name().toLowerCase() + " over " + first.name() + (second == null ? "" : ", " + second.name()));
        }

        private static ParameterDefinition parameter(CompiledObject object, String name, String fallback) {
            String wanted = name.isBlank() ? fallback : name;
            return object.parameters().stream().filter(parameter -> parameter.name().equals(wanted)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("'" + wanted + "' is not a parameter of this object."));
        }

        private static double measure(CompiledObject object, Behaviour behaviour, ComputationSettings settings) {
            int iterations = settings.integer("iterations");
            double[] point = settings.vectorOr("point", new double[0]);
            return switch (object.definition()) {
                case ExplicitCurve curve -> scalarBehaviour(behaviour, object.function(curve.expression(), curve.variables()), point,
                        iterations, List.of(curve.x()));
                case ExplicitSurface surface -> scalarBehaviour(behaviour, object.function(surface.expression(), surface.variables()),
                        point, (int) Math.sqrt(iterations), List.of(surface.x(), surface.y()));
                case DiscreteMap map -> mapBehaviour(object, map, behaviour, point, iterations);
                case DifferentialSystem system -> flowBehaviour(object, system, behaviour, point, iterations);
                default -> throw new IllegalArgumentException("not sweepable");
            };
        }

        private static double scalarBehaviour(Behaviour behaviour, BoundFunction function, double[] point, int samples,
                                              List<Interval> domain) {
            if (behaviour == Behaviour.VALUE_AT_POINT) {
                if (point.length != domain.size()) {
                    throw new IllegalArgumentException("VALUE_AT_POINT reads at a point with " + domain.size() + " coordinates.");
                }
                return function.at(point);
            }
            double minimum = Double.POSITIVE_INFINITY;
            double maximum = Double.NEGATIVE_INFINITY;
            double sum = 0;
            int count = 0;
            int perAxis = Math.max(2, samples);
            int total = domain.size() == 1 ? perAxis : perAxis * perAxis;
            double[] argument = new double[domain.size()];
            for (int index = 0; index < total; index++) {
                argument[0] = domain.get(0).sample(index % perAxis, perAxis);
                if (domain.size() == 2) {
                    argument[1] = domain.get(1).sample(index / perAxis, perAxis);
                }
                double value = function.at(argument);
                if (Double.isFinite(value)) {
                    minimum = Math.min(minimum, value);
                    maximum = Math.max(maximum, value);
                    sum += value;
                    count++;
                }
            }
            return switch (behaviour) {
                case MINIMUM -> minimum;
                case MEAN -> count == 0 ? Double.NaN : sum / count;
                default -> maximum;
            };
        }

        /** For a map: the Lyapunov exponent (one variable), the period of the settled cycle, or the attractor's size. */
        private static double mapBehaviour(CompiledObject object, DiscreteMap map, Behaviour behaviour, double[] point,
                                           int iterations) {
            List<String> arguments = new ArrayList<>(map.variables());
            arguments.add("n");
            List<BoundFunction> updates = object.functions(map.updates(), arguments);
            int dimension = map.variables().size();
            double[] state = new double[dimension + 1];
            for (int index = 0; index < dimension; index++) {
                state[index] = point.length == dimension ? point[index] : 0.5;
            }
            BoundFunction derivative = dimension == 1 && behaviour == Behaviour.LYAPUNOV
                    ? object.function(object.derivativeExpression(map.updates().getFirst(), map.variables().getFirst()), arguments)
                    : null;
            int transientIterations = iterations / 2;
            double lyapunov = 0;
            List<double[]> settled = new ArrayList<>();
            double[] next = new double[dimension];
            for (int step = 0; step < iterations; step++) {
                state[dimension] = step;
                if (derivative != null && step >= transientIterations) {
                    lyapunov += Math.log(Math.abs(derivative.at(state)) + 1e-300);
                }
                for (int variable = 0; variable < dimension; variable++) {
                    next[variable] = updates.get(variable).at(state);
                }
                System.arraycopy(next, 0, state, 0, dimension);
                if (!Double.isFinite(state[0])) {
                    return Double.NaN;
                }
                if (step >= transientIterations) {
                    settled.add(java.util.Arrays.copyOf(state, dimension));
                }
            }
            return switch (behaviour) {
                case LYAPUNOV -> derivative == null ? Double.NaN : lyapunov / (iterations - transientIterations);
                case PERIOD -> period(settled);
                default -> spread(settled);
            };
        }

        private static double flowBehaviour(CompiledObject object, DifferentialSystem system, Behaviour behaviour,
                                            double[] point, int samples) {
            List<String> arguments = new ArrayList<>(system.variables());
            arguments.add("t");
            List<BoundFunction> equations = object.functions(system.equations(), arguments);
            int dimension = system.variables().size();
            double[] buffer = new double[dimension + 1];
            double[] start = new double[dimension];
            for (int index = 0; index < dimension; index++) {
                start[index] = point.length == dimension ? point[index] : 0.1;
            }
            OdeIntegrator.Solution solution = OdeIntegrator.integrate((time, state, derivative) -> {
                System.arraycopy(state, 0, buffer, 0, dimension);
                buffer[dimension] = time;
                for (int index = 0; index < dimension; index++) {
                    derivative[index] = equations.get(index).at(buffer);
                }
            }, start, system.time().minimum(), system.time().maximum(), samples, 1e-7, 500_000, 1e9, ComputationProgress.NONE);
            if (solution.escaped()) {
                return Double.NaN;
            }
            List<double[]> settled = new ArrayList<>();
            for (int index = solution.states().length / 2; index < solution.states().length; index++) {
                settled.add(solution.states()[index]);
            }
            return switch (behaviour) {
                case MAXIMUM -> settled.stream().mapToDouble(state -> state[0]).max().orElse(Double.NaN);
                case MINIMUM -> settled.stream().mapToDouble(state -> state[0]).min().orElse(Double.NaN);
                case MEAN -> settled.stream().mapToDouble(state -> state[0]).average().orElse(Double.NaN);
                default -> spread(settled);
            };
        }

        /** The smallest p ≤ 64 with x(n + p) = x(n) over the settled tail; 0 when there is none (chaos). */
        private static double period(List<double[]> settled) {
            int size = settled.size();
            for (int period = 1; period <= Math.min(64, size / 2); period++) {
                boolean repeats = true;
                for (int index = size - period * 2; index < size - period && repeats; index++) {
                    for (int axis = 0; axis < settled.get(index).length; axis++) {
                        if (Math.abs(settled.get(index)[axis] - settled.get(index + period)[axis]) > 1e-6) {
                            repeats = false;
                            break;
                        }
                    }
                }
                if (repeats) {
                    return period;
                }
            }
            return 0;
        }

        private static double spread(List<double[]> settled) {
            double minimum = Double.POSITIVE_INFINITY;
            double maximum = Double.NEGATIVE_INFINITY;
            for (double[] state : settled) {
                minimum = Math.min(minimum, state[0]);
                maximum = Math.max(maximum, state[0]);
            }
            return maximum - minimum;
        }
    }

    public enum DescentMethod { GRADIENT, MOMENTUM, NEWTON }

    /**
     * A point that walks downhill: the optimization path over the surface, with the value, the gradient's size and
     * the step at every iteration beside it. Newton's method uses the exact Hessian.
     */
    public static class GradientDescent implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.GRADIENT_DESCENT;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ExplicitSurface || definition instanceof ExplicitCurve;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.vector("start", List.of(), 0, -1e9, 1e9, "Where the descent starts"),
                    SettingSpecification.choice("method", DescentMethod.GRADIENT, "How each step is chosen"),
                    SettingSpecification.number("rate", 0.1, 1e-9, 100, "Step size (learning rate)"),
                    SettingSpecification.number("momentum", 0.9, 0, 0.999, "Momentum coefficient"),
                    SettingSpecification.integer("iterations", 200, 1, 100_000, "Iterations at most"),
                    SettingSpecification.number("tolerance", 1e-8, 0, 1, "Stop when the gradient is smaller"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return settings.integer("iterations") * 6L;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            List<String> variables = DefinitionCompiler.variablesOf(object.definition());
            String source = object.definition() instanceof ExplicitSurface surface ? surface.expression()
                    : ((ExplicitCurve) object.definition()).expression();
            List<Interval> domain = object.definition() instanceof ExplicitSurface surface ? List.of(surface.x(), surface.y())
                    : List.of(((ExplicitCurve) object.definition()).x());
            int dimension = variables.size();
            BoundFunction value = object.function(source, variables);
            List<BoundFunction> gradient = new ArrayList<>();
            List<List<BoundFunction>> hessian = new ArrayList<>();
            for (String variable : variables) {
                org.jmouse.formula.Formula partial = object.derivativeExpression(source, variable);
                gradient.add(object.function(partial, variables));
                List<BoundFunction> row = new ArrayList<>();
                for (String other : variables) {
                    row.add(object.function(object.derivativeExpression(partial, other), variables));
                }
                hessian.add(row);
            }
            double[] position = settings.vectorOr("start", domain.stream().mapToDouble(interval -> interval.at(0.8)).toArray());
            if (position.length != dimension) {
                throw new IllegalArgumentException("The start has " + dimension + " coordinates.");
            }
            DescentMethod method = settings.choice("method", DescentMethod.class);
            double rate = settings.number("rate");
            double momentum = settings.number("momentum");
            double tolerance = settings.number("tolerance");
            int iterations = settings.integer("iterations");
            double[] velocity = new double[dimension];
            List<double[]> path = new ArrayList<>();
            List<Float> values = new ArrayList<>();
            List<Float> gradientNorms = new ArrayList<>();
            List<Float> steps = new ArrayList<>();
            boolean converged = false;
            for (int iteration = 0; iteration < iterations; iteration++) {
                double current = value.at(position);
                double[] slope = new double[dimension];
                double norm = 0;
                for (int axis = 0; axis < dimension; axis++) {
                    slope[axis] = gradient.get(axis).at(position);
                    norm += slope[axis] * slope[axis];
                }
                norm = Math.sqrt(norm);
                path.add(append(position, current));
                values.add((float) current);
                gradientNorms.add((float) norm);
                if (!Double.isFinite(current) || norm < tolerance) {
                    converged = norm < tolerance;
                    steps.add(0f);
                    break;
                }
                double[] step = switch (method) {
                    case GRADIENT -> scale(slope, -rate);
                    case MOMENTUM -> {
                        for (int axis = 0; axis < dimension; axis++) {
                            velocity[axis] = momentum * velocity[axis] - rate * slope[axis];
                        }
                        yield velocity.clone();
                    }
                    case NEWTON -> {
                        double[][] matrix = new double[dimension][dimension];
                        for (int row = 0; row < dimension; row++) {
                            for (int column = 0; column < dimension; column++) {
                                matrix[row][column] = hessian.get(row).get(column).at(position);
                            }
                        }
                        double[] solved = org.jmouse.parallax.numerics.LinearAlgebra.solve(matrix, scale(slope, -1));
                        yield solved == null ? scale(slope, -rate) : solved;
                    }
                };
                double stepLength = 0;
                for (int axis = 0; axis < dimension; axis++) {
                    position[axis] += step[axis];
                    stepLength += step[axis] * step[axis];
                }
                steps.add((float) Math.sqrt(stepLength));
            }
            int shown = dimension + 1;
            float[] flat = new float[path.size() * shown];
            for (int index = 0; index < path.size(); index++) {
                for (int axis = 0; axis < shown; axis++) {
                    flat[index * shown + axis] = (float) path.get(index)[axis];
                }
            }
            float[] axis = new float[values.size()];
            for (int index = 0; index < axis.length; index++) {
                axis[index] = index;
            }
            Map<String, Dataset> parts = new LinkedHashMap<>();
            parts.put("path", new Dataset.Polylines(shown, List.of(new Dataset.Polyline(flat, null, method.name(), false)),
                    new double[shown], new double[shown]));
            parts.put("series", new Dataset.TimeSeries("iteration", axis, List.of("value", "gradient", "step"),
                    List.of(floats(values), floats(gradientNorms), floats(steps))));
            parts.put("result", new Dataset.Measurements(Map.of("iterations", (double) values.size(),
                    "converged", converged ? 1.0 : 0.0, "value", (double) values.getLast()), Map.of()));
            return new Dataset.Composite(parts);
        }

        private static double[] append(double[] position, double value) {
            double[] point = java.util.Arrays.copyOf(position, position.length + 1);
            point[position.length] = value;
            return point;
        }

        private static double[] scale(double[] vector, double factor) {
            double[] scaled = new double[vector.length];
            for (int index = 0; index < vector.length; index++) {
                scaled[index] = vector[index] * factor;
            }
            return scaled;
        }

        private static float[] floats(List<Float> values) {
            float[] array = new float[values.size()];
            for (int index = 0; index < array.length; index++) {
                array[index] = values.get(index);
            }
            return array;
        }
    }
}
