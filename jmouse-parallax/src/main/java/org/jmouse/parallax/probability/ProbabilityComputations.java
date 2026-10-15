package org.jmouse.parallax.probability;

import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.PlaneWindow;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.Sampling;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.DifferentialSystem;
import org.jmouse.parallax.object.ObjectDefinition.DiscreteMap;
import org.jmouse.parallax.object.ObjectDefinition.ProbabilityDistribution;
import org.jmouse.parallax.object.ObjectDefinition.Sequence;
import org.jmouse.parallax.object.Randomness;
import org.jmouse.parallax.object.StochasticFunction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Chance made visible, by engines that know no distribution and no process by name: a density is an expression the
 * engine samples and integrates, a random walk is an iteration whose updates draw, Brownian motion is a derivative
 * driven by white noise. (A Markov chain is a matrix: its evolution is the matrix analysis acting on a vector.) What an
 * object is decides which of these applies — the shape of its definition, never a family word.
 */
public final class ProbabilityComputations {

    private ProbabilityComputations() {
    }

    static SettingSpecification seed() {
        return SettingSpecification.integer("seed", 42, 0, Integer.MAX_VALUE, "Random seed: the same seed gives the same run");
    }

    /** A distribution's density, compiled over its variables. */
    static final class Density {

        private final BoundFunction function;

        Density(CompiledObject object) {
            ProbabilityDistribution distribution = (ProbabilityDistribution) object.definition();
            this.function = object.function(distribution.density(), distribution.variables());
        }

        double at(double x, double y) {
            return function.variableCount() == 1 ? function.at(x) : function.at(x, y);
        }
    }

    /**
     * The density drawn — a curve over one variable, a heat map over two — and, for one variable, what follows from it
     * without being written: the cumulative function, the mass over the support, the mean and the variance, each by
     * the trapezoidal rule over the samples.
     */
    public static class DensityView implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.DENSITY;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ProbabilityDistribution;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("resolution", 200, 8, 2000, "Samples per axis"), PlaneWindow.specification());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            int resolution = settings.integer("resolution");
            return object.definition().ambientDimension() == 3 ? Sampling.square(resolution) : resolution;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            ProbabilityDistribution distribution = (ProbabilityDistribution) object.definition();
            Density density = new Density(object);
            int resolution = settings.integer("resolution");
            if (distribution.variables().size() == 2) {
                Interval[] region = PlaneWindow.region(settings, distribution.support().get(0), distribution.support().get(1));
                Interval x = region[0];
                Interval y = region[1];
                float[] values = Sampling.grid(density::at, x, y, resolution, resolution, progress);
                double[] range = Sampling.range(values);
                return new Dataset.ScalarGrid(resolution, resolution, x, y, values, range[0], range[1], "density");
            }
            Interval support = distribution.support().getFirst();
            float[] pdf = new float[resolution * 2];
            float[] axis = new float[resolution];
            float[] cumulative = new float[resolution];
            double running = 0;
            double firstMoment = 0;
            double secondMoment = 0;
            double previousX = support.minimum();
            double previous = density.at(previousX, 0);
            double width = support.length() / (resolution - 1);
            for (int index = 0; index < resolution; index++) {
                double x = support.sample(index, resolution);
                double value = density.at(x, 0);
                if (index > 0) {
                    running += (previous + value) / 2 * width;
                    firstMoment += (previousX * previous + x * value) / 2 * width;
                    secondMoment += (previousX * previousX * previous + x * x * value) / 2 * width;
                }
                previous = value;
                previousX = x;
                pdf[index * 2] = (float) x;
                pdf[index * 2 + 1] = (float) value;
                axis[index] = (float) x;
                cumulative[index] = (float) running;
            }
            double mean = firstMoment / running;
            Map<String, Double> mass = new LinkedHashMap<>();
            mass.put("massOverSupport", running);
            mass.put("mean", mean);
            mass.put("variance", secondMoment / running - mean * mean);
            return new Dataset.Composite(Map.of(
                    "pdf", new Dataset.Polylines(2, List.of(new Dataset.Polyline(pdf, null, "pdf", false)),
                            new double[]{support.minimum(), 0}, new double[]{support.maximum(), 0}),
                    "cdf", new Dataset.TimeSeries("x", axis, List.of("F(x)"), List.of(cumulative)),
                    "mass", new Dataset.Measurements(mass, Map.of("mean", "over the support, of the density normalized to one"))));
        }
    }

    /**
     * Draws from the distribution by rejection under its density's maximum over the support — the same method for
     * every density — and bins the draws into a histogram beside the running mean.
     */
    public static class Samples implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.SAMPLES;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ProbabilityDistribution;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("count", 10_000, 1, 2_000_000, "Draws"),
                    SettingSpecification.integer("bins", 60, 2, 1000, "Histogram bins per axis"), seed());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return settings.integer("count") * 8L;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            ProbabilityDistribution distribution = (ProbabilityDistribution) object.definition();
            Density density = new Density(object);
            int count = settings.integer("count");
            int bins = settings.integer("bins");
            int dimension = distribution.variables().size();
            SplittableRandom random = new SplittableRandom(settings.integer("seed"));
            Interval x = distribution.support().get(0);
            Interval y = dimension == 2 ? distribution.support().get(1) : Interval.of(0, 1);
            double ceiling = 0;
            for (int index = 0; index < 4000; index++) {
                ceiling = Math.max(ceiling, density.at(x.at(random.nextDouble()), y.at(random.nextDouble())));
            }
            ceiling *= 1.2;
            if (!(ceiling > 0)) {
                throw new IllegalArgumentException("The density is zero over its support.");
            }
            float[] points = new float[count * dimension];
            float[] histogram = new float[dimension == 2 ? bins * bins : bins];
            double sum = 0;
            int drawn = 0;
            int attempts = 0;
            while (drawn < count && attempts < count * 200) {
                attempts++;
                double candidateX = x.at(random.nextDouble());
                double candidateY = y.at(random.nextDouble());
                if (random.nextDouble() * ceiling > density.at(candidateX, candidateY)) {
                    continue;
                }
                points[drawn * dimension] = (float) candidateX;
                int column = Math.min(bins - 1, (int) ((candidateX - x.minimum()) / x.length() * bins));
                if (dimension == 2) {
                    points[drawn * dimension + 1] = (float) candidateY;
                    int row = Math.min(bins - 1, (int) ((candidateY - y.minimum()) / y.length() * bins));
                    histogram[row * bins + column]++;
                } else {
                    histogram[column]++;
                }
                sum += candidateX;
                drawn++;
                if (drawn % 100_000 == 0) {
                    progress.checkpoint((double) drawn / count, "drawing");
                }
            }
            double[] range = Sampling.range(histogram);
            Map<String, Dataset> parts = new LinkedHashMap<>();
            parts.put("points", new Dataset.PointCloud(dimension, java.util.Arrays.copyOf(points, drawn * dimension), null, null));
            parts.put("histogram", new Dataset.ScalarGrid(bins, dimension == 2 ? bins : 1, x, y, histogram, range[0], range[1], "count"));
            parts.put("estimates", new Dataset.Measurements(Map.of("drawn", (double) drawn, "mean", drawn == 0 ? Double.NaN : sum / drawn,
                    "acceptance", (double) drawn / attempts), Map.of()));
            return new Dataset.Composite(parts);
        }
    }

    /**
     * Sample paths of a stochastic iteration or a stochastic differential equation, each from the state's written
     * start: an iteration steps its updates, every random call drawing afresh; a derivative driven by
     * {@code white_noise()} is integrated by Euler–Maruyama over the system's time span. A one-variable process is
     * drawn against its step or its time, two or three variables as a path in the plane or in space.
     */
    public static class RandomWalk implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.RANDOM_WALK;
        }

        public boolean accepts(ObjectDefinition definition) {
            return (definition instanceof DiscreteMap || definition instanceof DifferentialSystem)
                    && Randomness.isStochastic(definition);
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("paths", 20, 1, 2000, "Independent paths"),
                    SettingSpecification.integer("steps", 1000, 2, 1_000_000, "Steps per path"), seed());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return (long) settings.integer("paths") * settings.integer("steps") * object.definition().ambientDimension();
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            Process process = Process.of(object);
            int paths = settings.integer("paths");
            int steps = settings.integer("steps");
            int variables = process.variables();
            int shown = variables == 1 ? 2 : Math.min(3, variables);
            SplittableRandom random = new SplittableRandom(settings.integer("seed"));
            List<Dataset.Polyline> lines = new ArrayList<>();
            double finalSquares = 0;
            for (int path = 0; path < paths; path++) {
                progress.checkpoint((double) path / paths, "walking");
                float[] points = new float[(steps + 1) * shown];
                double[] state = process.starts();
                double[] start = state.clone();
                write(points, 0, shown, process.clock(0, steps), state);
                for (int step = 1; step <= steps; step++) {
                    state = process.advance(state, step - 1, steps, random);
                    write(points, step, shown, process.clock(step, steps), state);
                }
                for (int variable = 0; variable < variables; variable++) {
                    double displacement = state[variable] - start[variable];
                    finalSquares += displacement * displacement;
                }
                lines.add(new Dataset.Polyline(points, null, "path-" + path, false));
            }
            return new Dataset.Composite(Map.of(
                    "paths", new Dataset.Polylines(shown, lines, new double[shown], new double[shown]),
                    "statistics", new Dataset.Measurements(Map.of("meanSquaredDisplacement", finalSquares / paths), Map.of())));
        }

        /** One point of a path: against the clock for one variable, the first two or three variables otherwise. */
        private static void write(float[] points, int index, int shown, double clock, double[] state) {
            if (state.length == 1) {
                points[index * 2] = (float) clock;
                points[index * 2 + 1] = (float) state[0];
                return;
            }
            for (int axis = 0; axis < shown; axis++) {
                points[index * shown + axis] = (float) state[axis];
            }
        }
    }

    /** A stochastic system ready to step: its random functions bound, its starts evaluated. */
    private abstract static class Process {

        private final double[] starts;

        Process(CompiledObject object, List<String> starts) {
            this.starts = starts.stream().mapToDouble(object::constant).toArray();
        }

        static Process of(CompiledObject object) {
            return switch (object.definition()) {
                case DiscreteMap map -> new Iterated(object, map);
                case DifferentialSystem system -> new Integrated(object, system);
                default -> throw new IllegalArgumentException("Sample paths are drawn for a stochastic iteration or derivative.");
            };
        }

        double[] starts() {
            return starts.clone();
        }

        int variables() {
            return starts.length;
        }

        /** What a one-variable path is drawn against at a step: the step itself, or the time it is reached. */
        abstract double clock(int step, int steps);

        abstract double[] advance(double[] state, int step, int steps, SplittableRandom random);
    }

    /** {@code x_next = …}: the step's values first, in order, then every update from the same state. */
    private static final class Iterated extends Process {

        private final List<StochasticFunction> values;
        private final List<StochasticFunction> updates;
        private final double[]                 arguments;

        Iterated(CompiledObject object, DiscreteMap map) {
            super(object, map.starts());
            List<String> names = new ArrayList<>(map.variables());
            names.add("n");
            this.values = new ArrayList<>();
            for (ObjectDefinition.StepValue value : map.stepValues()) {
                values.add(object.stochasticFunction(value.expression(), List.copyOf(names), false));
                names.add(value.name());
            }
            this.updates = map.updates().stream().map(update -> object.stochasticFunction(update, names, false)).toList();
            this.arguments = new double[names.size()];
        }

        double clock(int step, int steps) {
            return step;
        }

        double[] advance(double[] state, int step, int steps, SplittableRandom random) {
            System.arraycopy(state, 0, arguments, 0, state.length);
            arguments[state.length] = step;
            for (int index = 0; index < values.size(); index++) {
                arguments[state.length + 1 + index] = values.get(index).at(arguments, random, 1);
            }
            double[] next = new double[state.length];
            for (int index = 0; index < next.length; index++) {
                next[index] = updates.get(index).at(arguments, random, 1);
            }
            return next;
        }
    }

    /** {@code d(x)/dt = …}: one Euler–Maruyama step of the system's time span divided into the path's steps. */
    private static final class Integrated extends Process {

        private final List<StochasticFunction> derivatives;
        private final Interval                 time;
        private final double[]                 arguments;

        Integrated(CompiledObject object, DifferentialSystem system) {
            super(object, system.starts());
            List<String> names = new ArrayList<>(system.variables());
            names.add("t");
            this.derivatives = system.equations().stream()
                    .map(equation -> object.stochasticFunction(equation, names, true)).toList();
            this.time = system.time();
            this.arguments = new double[names.size()];
        }

        double clock(int step, int steps) {
            return time.minimum() + step * (time.length() / steps);
        }

        double[] advance(double[] state, int step, int steps, SplittableRandom random) {
            double timeStep = time.length() / steps;
            System.arraycopy(state, 0, arguments, 0, state.length);
            arguments[state.length] = clock(step, steps);
            double[] next = new double[state.length];
            for (int index = 0; index < next.length; index++) {
                next[index] = state[index] + timeStep * derivatives.get(index).at(arguments, random, timeStep);
            }
            return next;
        }
    }

    /** The terms of a(n), their partial sums and successive ratios: a numeric set, and whether its series converges. */
    public static class SequenceTerms implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.SEQUENCE_TERMS;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof Sequence;
        }

        public List<SettingSpecification> settings() {
            return List.of();
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            Sequence sequence = (Sequence) object.definition();
            return sequence.last() - sequence.first() + 1L;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            Sequence sequence = (Sequence) object.definition();
            BoundFunction term = object.function(sequence.expression(), sequence.variables());
            int count = sequence.last() - sequence.first() + 1;
            float[] axis = new float[count];
            float[] terms = new float[count];
            float[] sums = new float[count];
            float[] ratios = new float[count];
            double running = 0;
            double previous = Double.NaN;
            for (int index = 0; index < count; index++) {
                int n = sequence.first() + index;
                double value = term.at(n);
                running += value;
                axis[index] = n;
                terms[index] = (float) value;
                sums[index] = (float) running;
                ratios[index] = (float) (index == 0 ? Double.NaN : value / previous);
                previous = value;
            }
            String index = sequence.variables().getFirst();
            List<String> columns = List.of("a(" + index + ")", "S(" + index + ")", "a(" + index + ")/a(" + index + "-1)");
            return new Dataset.Composite(Map.of(
                    "terms", new Dataset.TimeSeries(index, axis, columns, List.of(terms, sums, ratios)),
                    "summary", new Dataset.Measurements(Map.of("lastTerm", (double) terms[count - 1], "partialSum", running), Map.of())));
        }
    }
}
