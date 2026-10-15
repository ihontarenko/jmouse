package org.jmouse.parallax.dynamics;

import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.DifferentialSystem;
import org.jmouse.parallax.object.ObjectDefinition.FlowImage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * A picture in which every pixel is one run of a differential system — its {@code image} block: the run starts from
 * the pixel, is stepped (fourth-order Runge–Kutta, a fixed step) until a stop holds or t passes the system's span, and
 * the first place its crossing expression changes sign where the hit condition holds gives the pixel its value. That is
 * how a camera sees a black hole (a ray traced back from each pixel to where it meets the disk), and how a basin of
 * attraction is drawn (where each start ends up). Rows are run in parallel, each worker with its own bound functions.
 */
public final class FlowImageComputation implements Computation {

    public RepresentationKind kind() {
        return RepresentationKind.FLOW_IMAGE;
    }

    public boolean accepts(ObjectDefinition definition) {
        return definition instanceof DifferentialSystem system && system.image() != null && DynamicsComputations.deterministic(definition);
    }

    public List<SettingSpecification> settings() {
        return List.of(SettingSpecification.integer("columns", 360, 16, 1600, "Pixels across"),
                SettingSpecification.number("step", 0.01, 1e-6, 10, "The step in t each run is taken in"));
    }

    public long estimatedCost(CompiledObject object, ComputationSettings settings) {
        DifferentialSystem system = (DifferentialSystem) object.definition();
        int columns = settings.integer("columns");
        long steps = (long) Math.min(100_000, Math.ceil(system.time().length() / settings.number("step")));
        return (long) columns * rows(system.image(), columns) * steps * system.variables().size();
    }

    private static int rows(FlowImage image, int columns) {
        double aspect = image.verticalRange().length() / image.horizontalRange().length();
        return Math.max(1, (int) Math.round(columns * aspect));
    }

    public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
        DifferentialSystem system = (DifferentialSystem) object.definition();
        FlowImage image = system.image();
        int columns = settings.integer("columns");
        int rows = rows(image, columns);
        double step = settings.number("step");
        List<String> arguments = new ArrayList<>(system.variables());
        arguments.add("t");
        arguments.add(image.horizontal());
        arguments.add(image.vertical());
        Run template = new Run(object.functions(system.equations(), arguments), object.functions(image.starts(), arguments),
                object.functions(image.stops(), arguments), object.function(image.crossing(), arguments),
                image.where() == null ? null : object.function(image.where(), arguments), object.function(image.value(), arguments),
                system.variables().size(), system.time(), step);
        float[] values = new float[columns * rows];
        AtomicInteger done = new AtomicInteger();
        int workers = Math.max(1, Runtime.getRuntime().availableProcessors());
        IntStream.range(0, workers).parallel().forEach(worker -> {
            Run run = template.copy();
            for (int row = worker; row < rows; row += workers) {
                double y = image.verticalRange().at((row + 0.5) / rows);
                for (int column = 0; column < columns; column++) {
                    double x = image.horizontalRange().at((column + 0.5) / columns);
                    values[row * columns + column] = (float) run.pixel(x, y);
                }
                int finished = done.incrementAndGet();
                if (worker == 0) {
                    progress.checkpoint((double) finished / rows, "tracing");
                }
            }
        });
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        for (float value : values) {
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        return new Dataset.ScalarGrid(columns, rows, image.horizontalRange(), image.verticalRange(), values, minimum,
                maximum == minimum ? minimum + 1 : maximum, "value");
    }

    /** One worker's bound functions and buffers: a run of the system from one pixel. */
    private record Run(List<BoundFunction> equations, List<BoundFunction> starts, List<BoundFunction> stops,
                       BoundFunction crossing, BoundFunction where, BoundFunction value, int dimension, Interval time,
                       double step, double[] arguments, double[][] stages, double[] state, double[] next) {

        Run(List<BoundFunction> equations, List<BoundFunction> starts, List<BoundFunction> stops, BoundFunction crossing,
            BoundFunction where, BoundFunction value, int dimension, Interval time, double step) {
            this(equations, starts, stops, crossing, where, value, dimension, time, step, new double[dimension + 3],
                    new double[5][dimension], new double[dimension], new double[dimension]);
        }

        Run copy() {
            return new Run(equations.stream().map(BoundFunction::copy).toList(), starts.stream().map(BoundFunction::copy).toList(),
                    stops.stream().map(BoundFunction::copy).toList(), crossing.copy(), where == null ? null : where.copy(),
                    value.copy(), dimension, time, step);
        }

        double pixel(double x, double y) {
            arguments[dimension + 1] = x;
            arguments[dimension + 2] = y;
            java.util.Arrays.fill(arguments, 0, dimension, 0);
            arguments[dimension] = time.minimum();
            for (int variable = 0; variable < dimension; variable++) {
                state[variable] = starts.get(variable).at(arguments);
            }
            double t = time.minimum();
            double before = at(crossing, state, t);
            while (t < time.maximum()) {
                if (stopped(state, t)) {
                    return 0;
                }
                stepFrom(state, t, next);
                double after = at(crossing, next, t + step);
                if (Double.isNaN(after)) {
                    return 0;
                }
                if ((before < 0 && after >= 0) || (before > 0 && after <= 0)) {
                    double fraction = before / (before - after);
                    double[] hit = stages[4];
                    for (int variable = 0; variable < dimension; variable++) {
                        hit[variable] = state[variable] + fraction * (next[variable] - state[variable]);
                    }
                    double when = t + fraction * step;
                    if (where == null || at(where, hit, when) != 0) {
                        double shade = at(value, hit, when);
                        return Double.isFinite(shade) ? shade : 0;
                    }
                }
                System.arraycopy(next, 0, state, 0, dimension);
                t += step;
                before = after;
            }
            return 0;
        }

        private boolean stopped(double[] at, double t) {
            for (BoundFunction stop : stops) {
                double condition = at(stop, at, t);
                if (condition != 0 || Double.isNaN(condition)) {
                    return true;
                }
            }
            return false;
        }

        private double at(BoundFunction function, double[] at, double t) {
            System.arraycopy(at, 0, arguments, 0, dimension);
            arguments[dimension] = t;
            return function.at(arguments);
        }

        /** One fourth-order Runge–Kutta step of size {@code step} from {@code from} at t. */
        private void stepFrom(double[] from, double t, double[] into) {
            double[] k1 = stages[0];
            double[] k2 = stages[1];
            double[] k3 = stages[2];
            double[] k4 = stages[3];
            double[] probe = into;
            derivative(from, t, k1);
            for (int variable = 0; variable < dimension; variable++) {
                probe[variable] = from[variable] + 0.5 * step * k1[variable];
            }
            derivative(probe, t + 0.5 * step, k2);
            for (int variable = 0; variable < dimension; variable++) {
                probe[variable] = from[variable] + 0.5 * step * k2[variable];
            }
            derivative(probe, t + 0.5 * step, k3);
            for (int variable = 0; variable < dimension; variable++) {
                probe[variable] = from[variable] + step * k3[variable];
            }
            derivative(probe, t + step, k4);
            for (int variable = 0; variable < dimension; variable++) {
                into[variable] = from[variable] + step / 6 * (k1[variable] + 2 * k2[variable] + 2 * k3[variable] + k4[variable]);
            }
        }

        private void derivative(double[] at, double t, double[] into) {
            System.arraycopy(at, 0, arguments, 0, dimension);
            arguments[dimension] = t;
            for (int variable = 0; variable < dimension; variable++) {
                into[variable] = equations.get(variable).at(arguments);
            }
        }
    }
}
