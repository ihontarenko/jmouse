package org.jmouse.parallax.fractal;

import org.jmouse.formula.Complex;
import org.jmouse.formula.Formula;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.PlaneWindow;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.Sampling;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.EscapeTimeFractal;
import org.jmouse.parallax.object.ObjectDefinition.IteratedFunctionSystem;
import org.jmouse.parallax.object.ObjectDefinition.NewtonFractal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;

/**
 * Escape time, basins of settling and the chaos game. (The orbit of one point is the {@code ORBIT} of the iteration.)
 *
 * <p>The first two are engines an {@code iteration} reaches by the function its output calls —
 * {@code escape_time(z, bailout, iterations)}, {@code newton_basin(z, iterations, tolerance)} — which each engine
 * declares as its {@code FUNCTION}. They step whatever {@code z_next} the document writes; no picture is built in.</p>
 */
public final class FractalComputations {

    private FractalComputations() {
    }

    /**
     * The iteration limit at the asked depth: the object's own limit over its whole region, and 64 more for every
     * doubling of the zoom, since a boundary seen closer needs more steps to tell inside from outside.
     */
    static int iterationsAtDepth(int limit, ComputationSettings settings, Interval real) {
        double doublings = Math.log(PlaneWindow.magnification(settings, real)) / Math.log(2);
        return (int) Math.min(100_000, limit + Math.round(64 * doublings));
    }

    public static class EscapeTime implements Computation {

        /** What an output calls to be computed here: {@code escape : image = escape_time(z, bailout, iterations)}. */
        public static final String FUNCTION = "escape_time";

        public RepresentationKind kind() {
            return RepresentationKind.ESCAPE_TIME;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof EscapeTimeFractal;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("resolution", 400, 16, 4096, "Samples per axis"), PlaneWindow.specification(),
                    SettingSpecification.integer("iterations", 0, 0, 100_000, "Overrides the object's limit when above 0"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            EscapeTimeFractal fractal = (EscapeTimeFractal) object.definition();
            int iterations = settings.integer("iterations") > 0 ? settings.integer("iterations")
                    : iterationsAtDepth(fractal.maximumIterations(), settings, fractal.real());
            return Sampling.square(settings.integer("resolution")) * iterations / 4;
        }

        /**
         * A step of the shape z² + k, k free of z, runs on doubles directly — it is the inner loop of every famous
         * picture, Mandelbrot's (k = c, the point) and Julia's (k a parameter) alike; any other step is compiled from
         * its formula. The count is smoothed with the normalized iteration count.
         */
        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            EscapeTimeFractal fractal = (EscapeTimeFractal) object.definition();
            int resolution = settings.integer("resolution");
            int maximum = settings.integer("iterations") > 0 ? settings.integer("iterations")
                    : iterationsAtDepth(fractal.maximumIterations(), settings, fractal.real());
            Interval[] region = PlaneWindow.region(settings, fractal.real(), fractal.imaginary());
            List<String> pointOnly = List.of(fractal.input());
            CompiledObject.ComplexBinding start = object.complexFunction(fractal.start(), pointOnly);
            Optional<CompiledObject.ComplexBinding> addend = quadraticAddend(fractal)
                    .map(constant -> object.complexFunction(constant, pointOnly));
            CompiledObject.ComplexBinding iteration = addend.isPresent() ? null
                    : object.complexFunction(fractal.iteration(), DefinitionCompiler.variablesOf(fractal));
            boolean pointIsState = fractal.state().equals(fractal.input());
            double bailoutSquared = fractal.bailout() * fractal.bailout();
            float[] values = new float[resolution * resolution];
            for (int row = 0; row < resolution; row++) {
                if (row % 16 == 0) {
                    progress.checkpoint((double) row / resolution, "iterating");
                }
                double imaginary = region[1].sample(row, resolution);
                for (int column = 0; column < resolution; column++) {
                    Complex point = new Complex(region[0].sample(column, resolution), imaginary);
                    Complex first = start.at(point);
                    double zr = first.real();
                    double zi = first.imaginary();
                    double kr = 0;
                    double ki = 0;
                    if (addend.isPresent()) {
                        Complex constant = addend.get().at(point);
                        kr = constant.real();
                        ki = constant.imaginary();
                    }
                    int count = 0;
                    double modulusSquared = zr * zr + zi * zi;
                    while (count < maximum && modulusSquared <= bailoutSquared) {
                        if (addend.isPresent()) {
                            double nextReal = zr * zr - zi * zi + kr;
                            zi = 2 * zr * zi + ki;
                            zr = nextReal;
                        } else {
                            Complex state = new Complex(zr, zi);
                            Complex next = pointIsState ? iteration.at(state) : iteration.at(state, point);
                            zr = next.real();
                            zi = next.imaginary();
                        }
                        modulusSquared = zr * zr + zi * zi;
                        count++;
                    }
                    values[row * resolution + column] = count >= maximum ? Float.NaN
                            : (float) (count + 1 - Math.log(Math.log(Math.sqrt(modulusSquared))) / Math.log(2));
                }
            }
            return new Dataset.IterationGrid(resolution, resolution, region[0], region[1], values, null, maximum, List.of());
        }

        /** k of a step written z^2 + k with k free of the state; empty for a step of any other shape. */
        private static Optional<Formula> quadraticAddend(EscapeTimeFractal fractal) {
            Formula step = FormulaLanguage.standard().parse(fractal.iteration());
            if (step instanceof Formula.Binary(Formula.Operator sum, Formula.Binary square, Formula constant)
                    && sum == Formula.Operator.ADD && square.operator() == Formula.Operator.POWER
                    && square.left() instanceof Formula.Variable(String name) && name.equals(fractal.state())
                    && square.right() instanceof Formula.Constant(double exponent) && exponent == 2
                    && !constant.dependsOn(fractal.state())) {
                return Optional.of(constant);
            }
            return Optional.empty();
        }
    }

    /**
     * The step from every point until it settles: each start coloured by where it settles, and how fast. With the step
     * {@code z - p(z) / diff(p(z), z)} these are the basins of Newton's method; the engine only steps and watches.
     */
    public static class NewtonBasins implements Computation {

        /** What an output calls to be computed here: {@code basin : image = newton_basin(z, iterations, tolerance)}. */
        public static final String FUNCTION = "newton_basin";

        public RepresentationKind kind() {
            return RepresentationKind.NEWTON_BASINS;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof NewtonFractal;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("resolution", 400, 16, 4096, "Samples per axis"), PlaneWindow.specification());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return Sampling.square(settings.integer("resolution")) * ((NewtonFractal) object.definition()).maximumIterations() / 4;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            NewtonFractal fractal = (NewtonFractal) object.definition();
            int resolution = settings.integer("resolution");
            Interval[] region = PlaneWindow.region(settings, fractal.real(), fractal.imaginary());
            CompiledObject.ComplexBinding iteration = object.complexFunction(fractal.iteration(), List.of(fractal.state()));
            List<double[]> roots = new ArrayList<>();
            float[] values = new float[resolution * resolution];
            int[] basins = new int[resolution * resolution];
            double tolerance = fractal.tolerance();
            for (int row = 0; row < resolution; row++) {
                if (row % 16 == 0) {
                    progress.checkpoint((double) row / resolution, "iterating");
                }
                double imaginary = region[1].sample(row, resolution);
                for (int column = 0; column < resolution; column++) {
                    Complex z = new Complex(region[0].sample(column, resolution), imaginary);
                    int count = 0;
                    boolean converged = false;
                    while (count < fractal.maximumIterations()) {
                        Complex next = iteration.at(z);
                        if (!next.isFinite()) {
                            break;
                        }
                        Complex step = next.subtract(z);
                        z = next;
                        count++;
                        if (step.modulus() < tolerance) {
                            converged = true;
                            break;
                        }
                    }
                    int index = row * resolution + column;
                    values[index] = count;
                    basins[index] = converged ? rootIndex(roots, z, Math.sqrt(tolerance) * 100) : -1;
                }
            }
            return new Dataset.IterationGrid(resolution, resolution, region[0], region[1], values, basins,
                    fractal.maximumIterations(), roots);
        }

        private static int rootIndex(List<double[]> roots, Complex z, double separation) {
            for (int index = 0; index < roots.size(); index++) {
                if (Math.hypot(roots.get(index)[0] - z.real(), roots.get(index)[1] - z.imaginary()) < Math.max(separation, 1e-6)) {
                    return index;
                }
            }
            roots.add(new double[]{z.real(), z.imaginary()});
            return roots.size() - 1;
        }
    }

    /** The chaos game: apply a randomly chosen map of the system, again and again; the points fill the attractor. */
    public static class ChaosGame implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.CHAOS_GAME;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof IteratedFunctionSystem;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("points", 100_000, 100, 5_000_000, "Points plotted"),
                    SettingSpecification.integer("seed", 11, 0, Integer.MAX_VALUE, "Random seed"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return settings.integer("points") * 2L;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            IteratedFunctionSystem system = (IteratedFunctionSystem) object.definition();
            int dimension = system.dimension();
            int count = settings.integer("points");
            SplittableRandom random = new SplittableRandom(settings.integer("seed"));
            double total = system.maps().stream().mapToDouble(ObjectDefinition.AffineMap::weight).sum();
            double[] cumulative = new double[system.maps().size()];
            double running = 0;
            for (int index = 0; index < cumulative.length; index++) {
                running += system.maps().get(index).weight() / total;
                cumulative[index] = running;
            }
            double[] point = new double[dimension];
            double[] next = new double[dimension];
            float[] points = new float[count * dimension];
            float[] chosen = new float[count];
            for (int step = -20; step < count; step++) {
                if (step > 0 && step % 100_000 == 0) {
                    progress.checkpoint((double) step / count, "playing");
                }
                double draw = random.nextDouble();
                int map = 0;
                while (map < cumulative.length - 1 && draw > cumulative[map]) {
                    map++;
                }
                ObjectDefinition.AffineMap affine = system.maps().get(map);
                for (int row = 0; row < dimension; row++) {
                    double sum = affine.translation().get(row);
                    for (int column = 0; column < dimension; column++) {
                        sum += affine.matrix().get(row).get(column) * point[column];
                    }
                    next[row] = sum;
                }
                System.arraycopy(next, 0, point, 0, dimension);
                if (step >= 0) {
                    for (int axis = 0; axis < dimension; axis++) {
                        points[step * dimension + axis] = (float) point[axis];
                    }
                    chosen[step] = map;
                }
            }
            return new Dataset.PointCloud(dimension, points, chosen, "map");
        }
    }
}
