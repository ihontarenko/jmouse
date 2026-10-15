package org.jmouse.parallax.dynamics;

import org.jmouse.formula.Complex;
import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.numerics.LinearAlgebra;
import org.jmouse.parallax.numerics.NewtonSolver;
import org.jmouse.parallax.numerics.OdeIntegrator;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.DefinitionCompiler;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.DifferentialSystem;
import org.jmouse.parallax.object.ObjectDefinition.DiscreteMap;
import org.jmouse.parallax.object.ObjectDefinition.EscapeTimeFractal;
import org.jmouse.parallax.object.Randomness;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Differential equations and discrete maps: paths, phase portraits, equilibria and their stability, bifurcations. */
public final class DynamicsComputations {

    private DynamicsComputations() {
    }

    /**
     * Whether a system draws no random numbers: the paths, portraits and equilibria here are of deterministic systems;
     * a stochastic one is drawn as sample paths by {@code ProbabilityComputations.RandomWalk}.
     */
    static boolean deterministic(ObjectDefinition definition) {
        return !Randomness.isStochastic(definition);
    }

    static SettingSpecification initial() {
        return SettingSpecification.vector("initial", List.of(), 0, -1e9, 1e9, "Initial state, one value per variable; empty starts where the document's state does");
    }

    static SettingSpecification time() {
        return SettingSpecification.vector("time", List.of(), 2, -1e9, 1e9, "[t₀, t₁]; the system's own span when empty");
    }

    static double[] span(DifferentialSystem system, ComputationSettings settings) {
        return settings.vectorOr("time", new double[]{system.time().minimum(), system.time().maximum()});
    }

    /** The first two or three state variables as a path in space; the full state is in the series. */
    static Dataset.Polyline path(OdeIntegrator.Solution solution, int shown, String label) {
        double[][] states = solution.states();
        float[] points = new float[states.length * shown];
        float[] times = new float[states.length];
        for (int index = 0; index < states.length; index++) {
            for (int axis = 0; axis < shown; axis++) {
                points[index * shown + axis] = (float) states[index][axis];
            }
            times[index] = (float) solution.times()[index];
        }
        return new Dataset.Polyline(points, times, label, false);
    }

    static Dataset.TimeSeries series(OdeIntegrator.Solution solution, List<String> variables) {
        float[] times = new float[solution.times().length];
        List<float[]> columns = new ArrayList<>();
        for (int variable = 0; variable < variables.size(); variable++) {
            columns.add(new float[times.length]);
        }
        for (int index = 0; index < times.length; index++) {
            times[index] = (float) solution.times()[index];
            for (int variable = 0; variable < variables.size(); variable++) {
                columns.get(variable)[index] = (float) solution.states()[index][variable];
            }
        }
        return new Dataset.TimeSeries("t", times, variables, columns);
    }

    /**
     * One path from the document's start — or a family of them, the start swept across one variable's bound
     * ({@code family 13 across y}: rays at thirteen impact parameters) —, drawn in the first two or three variables
     * ({@code axes 2}: a body's path on the plane while its velocity rides along in the state), with every variable's
     * series over t beside the first.
     */
    public static class Trajectory implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.TRAJECTORY;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof DifferentialSystem && deterministic(definition);
        }

        public List<SettingSpecification> settings() {
            return List.of(initial(), time(), SettingSpecification.integer("samples", 4000, 16, 200_000, "Points of the path"),
                    SettingSpecification.integer("axes", 3, 2, 3, "How many of the first variables the path is drawn in"),
                    SettingSpecification.integer("family", 1, 1, 64, "Paths, their starts spread across one variable's bound"),
                    SettingSpecification.text("across", "", 64, "The variable the family's starts are spread across"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return settings.integer("samples") * 20L * object.definition().ambientDimension() * settings.integer("family");
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            SystemView system = SystemView.of(object);
            DifferentialSystem definition = (DifferentialSystem) object.definition();
            double[] span = span(definition, settings);
            double[] start = system.initialOr(settings.vectorOr("initial", new double[0]));
            int shown = Math.min(settings.integer("axes"), system.dimension());
            List<double[]> starts = familyStarts(definition, start, settings.integer("family"), settings.text("across"));
            List<Dataset.Polyline> paths = new ArrayList<>();
            OdeIntegrator.Solution solution = null;
            for (int member = 0; member < starts.size(); member++) {
                progress.checkpoint((double) member / starts.size(), "integrating");
                OdeIntegrator.Solution integrated = system.integrate(starts.get(member), span[0], span[1], settings.integer("samples"),
                        starts.size() == 1 ? progress : ComputationProgress.NONE);
                paths.add(path(integrated, shown, starts.size() == 1 ? "trajectory" : "path-" + member));
                solution = solution == null ? integrated : solution;
            }
            Map<String, Dataset> parts = new LinkedHashMap<>();
            parts.put("path", new Dataset.Polylines(shown, paths, new double[shown], new double[shown]));
            parts.put("series", series(solution, system.variables()));
            parts.put("facts", new Dataset.Measurements(Map.of("steps", (double) solution.steps(),
                    "escaped", solution.escaped() ? 1.0 : 0.0), Map.of()));
            return new Dataset.Composite(parts);
        }
    }

    /** The start alone, or {@code family} starts with {@code across} spread evenly over its bound, ends included. */
    static List<double[]> familyStarts(DifferentialSystem system, double[] start, int family, String across) {
        if (family <= 1) {
            return List.of(start);
        }
        int variable = system.variables().indexOf(across);
        if (variable < 0) {
            throw new IllegalArgumentException("A family of " + family + " paths is spread across one variable: 'across' is one of "
                    + system.variables() + ".");
        }
        if (system.bounds().size() <= variable) {
            throw new IllegalArgumentException("'" + across + "' needs a bound — 'in a .. b' in the state — to spread the starts over.");
        }
        Interval bound = system.bounds().get(variable);
        List<double[]> starts = new ArrayList<>();
        for (int member = 0; member < family; member++) {
            double[] memberStart = start.clone();
            memberStart[variable] = bound.at((double) member / (family - 1));
            starts.add(memberStart);
        }
        return starts;
    }

    /**
     * Equilibria of a flow (F = 0) or fixed points of a map (G(X) = X), each classified from the eigenvalues of the
     * Jacobian there: node, focus, saddle or centre for a flow; attracting, repelling or saddle for a map.
     */
    public static class Equilibria implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.EQUILIBRIA;
        }

        public boolean accepts(ObjectDefinition definition) {
            return ((definition instanceof DifferentialSystem system && !system.bounds().isEmpty())
                    || (definition instanceof DiscreteMap map && !map.bounds().isEmpty())) && deterministic(definition);
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("seeds", 8, 2, 30, "Newton starts per axis"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return (long) Math.pow(settings.integer("seeds"), Math.min(4, object.definition().ambientDimension())) * 600;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            return new Dataset.FeaturePoints(equilibria(SystemView.of(object), settings.integer("seeds")));
        }

        static List<Dataset.Feature> equilibria(SystemView system, int seeds) {
            int dimension = system.dimension();
            double[] minimum = system.bounds().stream().mapToDouble(Interval::minimum).toArray();
            double[] maximum = system.bounds().stream().mapToDouble(Interval::maximum).toArray();
            double[] image = new double[dimension];
            List<double[]> roots = NewtonSolver.rootsInBox((point, value) -> {
                system.apply(0, point, image);
                for (int axis = 0; axis < dimension; axis++) {
                    value[axis] = system.continuous() ? image[axis] : image[axis] - point[axis];
                }
            }, minimum, maximum, Math.min(seeds, dimension > 3 ? 4 : seeds), 1e-11);
            List<List<BoundFunction>> jacobian = system.jacobian();
            List<Dataset.Feature> features = new ArrayList<>();
            for (double[] root : roots) {
                List<Dataset.Eigenpair> eigenpairs = LinearAlgebra.eigenpairs(system.jacobianAt(jacobian, root, 0));
                Map<String, Object> facts = new LinkedHashMap<>();
                facts.put("eigenvalues", eigenpairs.stream().map(pair -> new double[]{pair.real(), pair.imaginary()}).toList());
                features.add(new Dataset.Feature(root, null,
                        system.continuous() ? classifyFlow(eigenpairs) : classifyMap(eigenpairs), facts));
            }
            return features;
        }

        static String classifyFlow(List<Dataset.Eigenpair> eigenpairs) {
            boolean anyPositive = eigenpairs.stream().anyMatch(pair -> pair.real() > 1e-9);
            boolean anyNegative = eigenpairs.stream().anyMatch(pair -> pair.real() < -1e-9);
            boolean oscillating = eigenpairs.stream().anyMatch(pair -> Math.abs(pair.imaginary()) > 1e-9);
            boolean allZeroReal = eigenpairs.stream().allMatch(pair -> Math.abs(pair.real()) <= 1e-9);
            if (anyPositive && anyNegative) {
                return "SADDLE";
            }
            if (allZeroReal) {
                return oscillating ? "CENTER" : "DEGENERATE";
            }
            if (oscillating) {
                return anyPositive ? "UNSTABLE_FOCUS" : "STABLE_FOCUS";
            }
            return anyPositive ? "UNSTABLE_NODE" : "STABLE_NODE";
        }

        static String classifyMap(List<Dataset.Eigenpair> eigenpairs) {
            boolean anyOutside = eigenpairs.stream().anyMatch(pair -> Math.hypot(pair.real(), pair.imaginary()) > 1 + 1e-9);
            boolean anyInside = eigenpairs.stream().anyMatch(pair -> Math.hypot(pair.real(), pair.imaginary()) < 1 - 1e-9);
            if (anyOutside && anyInside) {
                return "SADDLE";
            }
            if (anyOutside) {
                return "REPELLING";
            }
            return anyInside ? "ATTRACTING" : "NEUTRAL";
        }
    }

    /**
     * Phase portrait: trajectories from a lattice of starts, the equilibria they organize around, and the direction
     * field beneath them — a vortex, a source, a sink and a saddle are all visible at once.
     */
    public static class PhasePortrait implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.PHASE_PORTRAIT;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof DifferentialSystem system && system.variables().size() >= 2
                    && system.variables().size() <= 3 && !system.bounds().isEmpty() && deterministic(definition);
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("starts", 6, 2, 24, "Starts per axis"),
                    SettingSpecification.number("duration", 10, 0.01, 10_000, "How long each trajectory runs"),
                    SettingSpecification.integer("samples", 300, 16, 5000, "Points per trajectory"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return (long) Math.pow(settings.integer("starts"), object.definition().ambientDimension())
                    * settings.integer("samples") * 20;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            SystemView system = SystemView.of(object);
            int dimension = system.dimension();
            int starts = settings.integer("starts");
            double duration = settings.number("duration");
            int samples = settings.integer("samples");
            List<Dataset.Polyline> lines = new ArrayList<>();
            int total = (int) Math.pow(starts, dimension);
            for (int index = 0; index < total; index++) {
                progress.checkpoint((double) index / total, "integrating");
                double[] start = new double[dimension];
                int remainder = index;
                for (int axis = 0; axis < dimension; axis++) {
                    start[axis] = system.bounds().get(axis).at((remainder % starts + 0.5) / starts);
                    remainder /= starts;
                }
                OdeIntegrator.Solution solution = system.integrate(start, 0, duration, samples, ComputationProgress.NONE);
                lines.add(path(solution, dimension, "start-" + index));
            }
            double[] minimum = system.bounds().stream().mapToDouble(Interval::minimum).toArray();
            double[] maximum = system.bounds().stream().mapToDouble(Interval::maximum).toArray();
            Map<String, Dataset> parts = new LinkedHashMap<>();
            parts.put("trajectories", new Dataset.Polylines(dimension, lines, minimum, maximum));
            parts.put("equilibria", new Dataset.FeaturePoints(Equilibria.equilibria(system, 8)));
            return new Dataset.Composite(parts);
        }
    }

    /**
     * Iterates a map: the orbit as points, its values over n, and for a one-dimensional map the cobweb. A complex
     * iteration z ← g(z) or z ← g(z, c) is followed from one {@code point} of its plane — its start, or its constant —
     * until it leaves the bailout: why a point of a Julia or a Mandelbrot set escapes, or what it circles.
     */
    public static class Orbit implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.ORBIT;
        }

        public boolean accepts(ObjectDefinition definition) {
            return (definition instanceof DiscreteMap && deterministic(definition)) || definition instanceof EscapeTimeFractal;
        }

        public List<SettingSpecification> settings() {
            return List.of(initial(), SettingSpecification.integer("iterations", 500, 1, 1_000_000, "Iterations"),
                    SettingSpecification.integer("transient", 0, 0, 1_000_000, "Iterations discarded first"),
                    SettingSpecification.vector("point", List.of(), 2, -1e12, 1e12,
                            "A complex iteration's point (re, im): its start, or its constant; the origin when empty"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return (long) (settings.integer("iterations") + settings.integer("transient")) * object.definition().ambientDimension();
        }

        record Iterated(float[] points, int dimension, Dataset.TimeSeries series) {
        }

        static Iterated iterate(SystemView system, double[] start, int iterations, int transientIterations) {
            int dimension = system.dimension();
            double[] state = start.clone();
            double[] next = new double[dimension];
            for (int step = 0; step < transientIterations; step++) {
                system.apply(step, state, next);
                System.arraycopy(next, 0, state, 0, dimension);
            }
            float[] points = new float[iterations * dimension];
            float[] axis = new float[iterations];
            List<float[]> columns = new ArrayList<>();
            for (int variable = 0; variable < dimension; variable++) {
                columns.add(new float[iterations]);
            }
            for (int step = 0; step < iterations; step++) {
                axis[step] = step + transientIterations;
                for (int variable = 0; variable < dimension; variable++) {
                    points[step * dimension + variable] = (float) state[variable];
                    columns.get(variable)[step] = (float) state[variable];
                }
                system.apply(step + transientIterations, state, next);
                System.arraycopy(next, 0, state, 0, dimension);
            }
            return new Iterated(points, dimension, new Dataset.TimeSeries("n", axis, system.variables(), columns));
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            if (object.definition() instanceof EscapeTimeFractal fractal) {
                return complexOrbit(object, fractal, settings.vectorOr("point", new double[]{0, 0}),
                        settings.integer("iterations"), settings.integer("transient"));
            }
            SystemView system = SystemView.of(object);
            double[] start = system.initialOr(settings.vectorOr("initial", new double[0]));
            Iterated orbit = iterate(system, start, settings.integer("iterations"), settings.integer("transient"));
            Map<String, Dataset> parts = new LinkedHashMap<>();
            parts.put("points", new Dataset.PointCloud(orbit.dimension(), orbit.points(), null, null));
            parts.put("series", orbit.series());
            if (orbit.dimension() == 1) {
                parts.put("cobweb", cobweb(orbit.points()));
            }
            return new Dataset.Composite(parts);
        }

        /**
         * z₀, z₁, z₂ … from the chosen point of the plane — z₀ itself when the iteration's state is its input (a Julia
         * set), else the document's start at c with c held as the constant (the Mandelbrot set) — drawn as a path,
         * stopped where |z| passes the bailout.
         */
        private static Dataset complexOrbit(CompiledObject object, EscapeTimeFractal fractal, double[] point, int iterations,
                                            int transientIterations) {
            CompiledObject.ComplexBinding iteration = object.complexFunction(fractal.iteration(),
                    DefinitionCompiler.variablesOf(fractal));
            boolean pointIsState = fractal.state().equals(fractal.input());
            Complex chosen = new Complex(point[0], point[1]);
            Complex z = object.complexFunction(fractal.start(), List.of(fractal.input())).at(chosen);
            List<Float> path = new ArrayList<>();
            int escapedAt = -1;
            for (int step = 0; step <= transientIterations + iterations; step++) {
                if (step >= transientIterations) {
                    path.add((float) z.real());
                    path.add((float) z.imaginary());
                }
                if (z.modulus() > fractal.bailout()) {
                    escapedAt = step;
                    break;
                }
                z = pointIsState ? iteration.at(z) : iteration.at(z, chosen);
            }
            float[] flat = new float[path.size()];
            for (int index = 0; index < flat.length; index++) {
                flat[index] = path.get(index);
            }
            Map<String, Dataset> parts = new LinkedHashMap<>();
            parts.put("orbit", new Dataset.Polylines(2, List.of(new Dataset.Polyline(flat, null, "orbit", false)),
                    new double[2], new double[2]));
            parts.put("facts", new Dataset.Measurements(Map.of("escapedAt", (double) escapedAt, "steps", (double) (flat.length / 2)),
                    Map.of("escapedAt", "-1 when the orbit stayed bounded")));
            return new Dataset.Composite(parts);
        }

        /** x₀ → (x₀, x₁) → (x₁, x₁) → (x₁, x₂) …: the staircase between y = G(x) and y = x. */
        private static Dataset.Polylines cobweb(float[] values) {
            int steps = Math.min(values.length - 1, 2000);
            float[] points = new float[(steps * 2 + 1) * 2];
            points[0] = values[0];
            points[1] = 0;
            int cursor = 2;
            for (int step = 0; step < steps; step++) {
                points[cursor++] = values[step];
                points[cursor++] = values[step + 1];
                points[cursor++] = values[step + 1];
                points[cursor++] = values[step + 1];
            }
            return new Dataset.Polylines(2, List.of(new Dataset.Polyline(points, null, "cobweb", false)), new double[2], new double[2]);
        }
    }

    /**
     * Sweeps one parameter and records where the system settles: stable → periodic → chaotic, read off as the
     * branches of the diagram (the logistic map's period-doubling cascade). For a flow the settled values are the
     * local maxima of the first variable.
     */
    public static class Bifurcation implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.BIFURCATION;
        }

        public boolean accepts(ObjectDefinition definition) {
            return (definition instanceof DiscreteMap || definition instanceof DifferentialSystem) && deterministic(definition);
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.text("parameter", "", 32, "The parameter swept; the first one when empty"),
                    SettingSpecification.vector("range", List.of(), 2, -1e9, 1e9, "[from, to]; the parameter's range when empty"),
                    SettingSpecification.integer("steps", 600, 2, 20_000, "Parameter values"),
                    SettingSpecification.integer("transient", 500, 0, 100_000, "Iterations (or time) discarded"),
                    SettingSpecification.integer("kept", 120, 1, 5000, "Settled values recorded per parameter value"),
                    initial());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            long perStep = settings.integer("transient") + settings.integer("kept");
            return settings.integer("steps") * perStep * (object.definition() instanceof DifferentialSystem ? 40L : 1L);
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            if (object.parameters().isEmpty()) {
                throw new IllegalArgumentException("A bifurcation diagram sweeps a parameter, and this object has none.");
            }
            String name = settings.text("parameter").isBlank() ? object.parameters().getFirst().name() : settings.text("parameter");
            var parameter = object.parameters().stream().filter(candidate -> candidate.name().equals(name)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("'" + name + "' is not a parameter of this object."));
            double[] range = settings.vectorOr("range", new double[]{parameter.minimum(), parameter.maximum()});
            int steps = settings.integer("steps");
            int transientIterations = settings.integer("transient");
            int kept = settings.integer("kept");
            List<Float> points = new ArrayList<>();
            for (int step = 0; step < steps; step++) {
                if (step % 20 == 0) {
                    progress.checkpoint((double) step / steps, "sweeping " + name);
                }
                double value = range[0] + (range[1] - range[0]) * step / (steps - 1);
                CompiledObject variant = object.withParameters(Map.of(name, value));
                SystemView system = SystemView.of(variant);
                double[] start = system.initialOr(settings.vectorOr("initial", new double[0]));
                if (system.continuous()) {
                    OdeIntegrator.Solution solution = system.integrate(start, 0, transientIterations + kept, (transientIterations + kept) * 20,
                            ComputationProgress.NONE);
                    double[][] states = solution.states();
                    int from = states.length * transientIterations / (transientIterations + kept);
                    for (int index = Math.max(1, from); index < states.length - 1; index++) {
                        if (states[index][0] > states[index - 1][0] && states[index][0] >= states[index + 1][0]) {
                            points.add((float) value);
                            points.add((float) states[index][0]);
                        }
                    }
                } else {
                    Orbit.Iterated orbit = Orbit.iterate(system, start, kept, transientIterations);
                    for (int index = 0; index < kept; index++) {
                        float settled = orbit.points()[index * orbit.dimension()];
                        if (Float.isFinite(settled)) {
                            points.add((float) value);
                            points.add(settled);
                        }
                    }
                }
            }
            float[] flat = new float[points.size()];
            for (int index = 0; index < flat.length; index++) {
                flat[index] = points.get(index);
            }
            return new Dataset.PointCloud(2, flat, null, name);
        }
    }
}
