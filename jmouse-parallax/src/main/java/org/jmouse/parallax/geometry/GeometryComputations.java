package org.jmouse.parallax.geometry;

import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.PlaneWindow;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.Sampling;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.numerics.GridMesh;
import org.jmouse.parallax.numerics.MarchingSquares;
import org.jmouse.parallax.numerics.MarchingTetrahedra;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.SurfaceGluing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** The object itself drawn: surfaces, curves, implicit sets, points, constructions, L-system paths. */
public final class GeometryComputations {

    private GeometryComputations() {
    }

    public static class Surface implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.SURFACE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.ExplicitSurface || definition instanceof ObjectDefinition.ParametricSurface;
        }

        public List<SettingSpecification> settings() {
            return List.of(Sampling.RESOLUTION);
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return Sampling.square(settings.integer("resolution")) * 3;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            int resolution = settings.integer("resolution");
            return switch (object.definition()) {
                case ObjectDefinition.ExplicitSurface surface -> {
                    BoundFunction height = object.function(surface.expression(), surface.variables());
                    yield GridMesh.build((x, y, target) -> {
                        target[0] = x;
                        target[1] = y;
                        target[2] = height.at(x, y);
                    }, 3, surface.x(), surface.y(), resolution, resolution, (x, y, point) -> point[2], "height", List.of(),
                            progress);
                }
                case ObjectDefinition.ParametricSurface surface -> parametric(object, surface, resolution, progress);
                default -> throw new IllegalArgumentException("not a surface");
            };
        }

        public static Dataset.Mesh parametric(CompiledObject object, ObjectDefinition.ParametricSurface surface, int resolution,
                                       ComputationProgress progress) {
            List<BoundFunction> components = object.functions(surface.components(), surface.variables());
            int dimension = components.size();
            Dataset.Mesh mesh = GridMesh.build((u, v, target) -> {
                for (int axis = 0; axis < dimension; axis++) {
                    target[axis] = components.get(axis).at(u, v);
                }
            }, dimension, surface.u(), surface.v(), resolution, resolution, (u, v, point) -> v, "v", List.of(), progress);
            return new Dataset.Mesh(mesh.dimension(), mesh.positions(), mesh.normals(), mesh.indices(), mesh.scalars(),
                    mesh.scalarName(), mesh.minimum(), mesh.maximum(), seams(components, surface, dimension));
        }

        /** The edges of the parameter square that the gluing identifies, and whether the identification reverses. */
        private static List<Dataset.Seam> seams(List<BoundFunction> components, ObjectDefinition.ParametricSurface surface,
                                                int dimension) {
            SurfaceGluing gluing = surface.gluing();
            if (gluing == SurfaceGluing.NONE) {
                return List.of();
            }
            boolean uGlued = gluing != SurfaceGluing.SPHERE;
            boolean vGlued = gluing == SurfaceGluing.TORUS || gluing == SurfaceGluing.KLEIN_BOTTLE
                    || gluing == SurfaceGluing.PROJECTIVE_PLANE;
            boolean uReversed = gluing == SurfaceGluing.MOBIUS || gluing == SurfaceGluing.KLEIN_BOTTLE
                    || gluing == SurfaceGluing.PROJECTIVE_PLANE;
            boolean vReversed = gluing == SurfaceGluing.PROJECTIVE_PLANE;
            List<Dataset.Seam> seams = new ArrayList<>();
            if (uGlued) {
                seams.add(new Dataset.Seam("u-edges", edge(components, surface.u().minimum(), surface.v(), true, dimension), uReversed));
            }
            if (vGlued) {
                seams.add(new Dataset.Seam("v-edges", edge(components, surface.v().minimum(), surface.u(), false, dimension), vReversed));
            }
            return seams;
        }

        private static float[] edge(List<BoundFunction> components, double fixed, Interval along, boolean fixedIsU,
                                    int dimension) {
            int samples = 128;
            float[] points = new float[samples * dimension];
            for (int index = 0; index < samples; index++) {
                double moving = along.sample(index, samples);
                for (int axis = 0; axis < dimension; axis++) {
                    points[index * dimension + axis] = (float) (fixedIsU ? components.get(axis).at(fixed, moving)
                            : components.get(axis).at(moving, fixed));
                }
            }
            return points;
        }
    }

    public static class Curve implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.CURVE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.ExplicitCurve || definition instanceof ObjectDefinition.ParametricCurve;
        }

        public List<SettingSpecification> settings() {
            return List.of(Sampling.CURVE_SAMPLES);
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return settings.integer("samples") * 3L;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            int samples = settings.integer("samples");
            return switch (object.definition()) {
                case ObjectDefinition.ExplicitCurve curve -> {
                    BoundFunction function = object.function(curve.expression(), curve.variables());
                    yield polylines(2, samples, curve.x(), t -> new double[]{t, function.at(t)}, false);
                }
                case ObjectDefinition.ParametricCurve curve -> {
                    List<BoundFunction> components = object.functions(curve.components(), curve.variables());
                    yield polylines(components.size(), samples, curve.t(), t -> {
                        double[] point = new double[components.size()];
                        for (int axis = 0; axis < point.length; axis++) {
                            point[axis] = components.get(axis).at(t);
                        }
                        return point;
                    }, curve.closed());
                }
                default -> throw new IllegalArgumentException("not a curve");
            };
        }

        /** Samples a curve, breaking it where a value is not finite (the branches of tan x). */
        public static Dataset.Polylines polylines(int dimension, int samples, Interval parameter,
                                           java.util.function.DoubleFunction<double[]> point, boolean closed) {
            List<Dataset.Polyline> lines = new ArrayList<>();
            double[] minimum = new double[dimension];
            double[] maximum = new double[dimension];
            Arrays.fill(minimum, Double.POSITIVE_INFINITY);
            Arrays.fill(maximum, Double.NEGATIVE_INFINITY);
            List<Float> current = new ArrayList<>();
            List<Float> parameters = new ArrayList<>();
            for (int index = 0; index < samples; index++) {
                double t = parameter.sample(index, samples);
                double[] value = point.apply(t);
                boolean finite = Arrays.stream(value).allMatch(Double::isFinite);
                if (!finite) {
                    flush(lines, current, parameters, false);
                    continue;
                }
                for (int axis = 0; axis < dimension; axis++) {
                    current.add((float) value[axis]);
                    minimum[axis] = Math.min(minimum[axis], value[axis]);
                    maximum[axis] = Math.max(maximum[axis], value[axis]);
                }
                parameters.add((float) t);
            }
            flush(lines, current, parameters, closed && lines.isEmpty());
            for (int axis = 0; axis < dimension; axis++) {
                if (minimum[axis] > maximum[axis]) {
                    minimum[axis] = 0;
                    maximum[axis] = 0;
                }
            }
            return new Dataset.Polylines(dimension, lines, minimum, maximum);
        }

        private static void flush(List<Dataset.Polyline> lines, List<Float> current, List<Float> parameters, boolean closed) {
            if (current.size() > 2) {
                float[] points = new float[current.size()];
                float[] scalars = new float[parameters.size()];
                for (int index = 0; index < points.length; index++) {
                    points[index] = current.get(index);
                }
                for (int index = 0; index < scalars.length; index++) {
                    scalars[index] = parameters.get(index);
                }
                lines.add(new Dataset.Polyline(points, scalars, null, closed));
            }
            current.clear();
            parameters.clear();
        }
    }

    /**
     * f(x, y, z) = 0 triangulated by marching tetrahedra. A hypersurface f(x, y, z, w) = 0 is the same surface with
     * its fourth input held at {@code slice} — the hypersphere's spheres that grow and shrink as w moves.
     */
    public static class ImplicitSurface implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.IMPLICIT_SURFACE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.ImplicitSurface || definition instanceof ObjectDefinition.ImplicitHypersurface;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("resolution", 48, 8, 160, "Cubes per axis"),
                    SettingSpecification.number("slice", 0, -1e6, 1e6, "The value a hypersurface's w is held at"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            long resolution = settings.integer("resolution") + 1L;
            return resolution * resolution * resolution;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            int resolution = settings.integer("resolution");
            return switch (object.definition()) {
                case ObjectDefinition.ImplicitSurface surface -> {
                    BoundFunction function = object.function(surface.expression(), surface.variables());
                    yield MarchingTetrahedra.build(function::at, surface.x(), surface.y(), surface.z(), resolution, progress);
                }
                case ObjectDefinition.ImplicitHypersurface hypersurface -> {
                    BoundFunction function = object.function(hypersurface.expression(), List.of("x", "y", "z", "w"));
                    double w = settings.number("slice");
                    yield MarchingTetrahedra.build((x, y, z) -> function.at(x, y, z, w), hypersurface.x(), hypersurface.y(),
                            hypersurface.z(), resolution, progress);
                }
                default -> throw new IllegalArgumentException("An implicit surface is f(x, y, z) = 0 or a slice of f(x, y, z, w) = 0.");
            };
        }
    }

    public static class Points implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.POINTS;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.PointSet;
        }

        public List<SettingSpecification> settings() {
            return List.of();
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return ((ObjectDefinition.PointSet) object.definition()).points().size();
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            ObjectDefinition.PointSet set = (ObjectDefinition.PointSet) object.definition();
            float[] points = new float[set.points().size() * set.dimension()];
            for (int index = 0; index < set.points().size(); index++) {
                for (int axis = 0; axis < set.dimension(); axis++) {
                    points[index * set.dimension() + axis] = set.points().get(index).get(axis).floatValue();
                }
            }
            return new Dataset.PointCloud(set.dimension(), points, null, null);
        }
    }

    public static class TurtlePath implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.TURTLE_PATH;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.LindenmayerSystem;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("maximumSymbols", 400_000, 1_000, 2_000_000,
                    "The rewritten string is refused beyond this length"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return settings.integer("maximumSymbols");
        }

        /** F and G draw forward, f moves without drawing, + and - turn, [ and ] push and pop the turtle. */
        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            ObjectDefinition.LindenmayerSystem system = (ObjectDefinition.LindenmayerSystem) object.definition();
            int maximumSymbols = settings.integer("maximumSymbols");
            String word = system.axiom();
            for (int iteration = 0; iteration < system.iterations(); iteration++) {
                StringBuilder next = new StringBuilder();
                for (char symbol : word.toCharArray()) {
                    next.append(system.rules().getOrDefault(String.valueOf(symbol), String.valueOf(symbol)));
                    if (next.length() > maximumSymbols) {
                        throw new IllegalArgumentException("The rewritten string exceeds " + maximumSymbols
                                + " symbols; lower the iterations.");
                    }
                }
                word = next.toString();
                progress.checkpoint((iteration + 1.0) / (system.iterations() + 1), "rewriting");
            }
            double angle = Math.toRadians(system.angle());
            double x = 0;
            double y = 0;
            double heading = Math.PI / 2;
            java.util.ArrayDeque<double[]> stack = new java.util.ArrayDeque<>();
            List<Dataset.Polyline> lines = new ArrayList<>();
            List<Float> current = new ArrayList<>(List.of(0f, 0f));
            double[] minimum = {0, 0};
            double[] maximum = {0, 0};
            for (char symbol : word.toCharArray()) {
                switch (symbol) {
                    case 'F', 'G' -> {
                        x += Math.cos(heading);
                        y += Math.sin(heading);
                        current.add((float) x);
                        current.add((float) y);
                        minimum[0] = Math.min(minimum[0], x);
                        minimum[1] = Math.min(minimum[1], y);
                        maximum[0] = Math.max(maximum[0], x);
                        maximum[1] = Math.max(maximum[1], y);
                    }
                    case 'f' -> {
                        x += Math.cos(heading);
                        y += Math.sin(heading);
                        current = restart(lines, current, x, y);
                    }
                    case '+' -> heading += angle;
                    case '-' -> heading -= angle;
                    case '[' -> stack.push(new double[]{x, y, heading});
                    case ']' -> {
                        if (!stack.isEmpty()) {
                            double[] saved = stack.pop();
                            x = saved[0];
                            y = saved[1];
                            heading = saved[2];
                            current = restart(lines, current, x, y);
                        }
                    }
                    default -> {
                    }
                }
            }
            restart(lines, current, x, y);
            return new Dataset.Polylines(2, lines, minimum, maximum);
        }

        private static List<Float> restart(List<Dataset.Polyline> lines, List<Float> current, double x, double y) {
            if (current.size() >= 4) {
                float[] points = new float[current.size()];
                for (int index = 0; index < points.length; index++) {
                    points[index] = current.get(index);
                }
                lines.add(new Dataset.Polyline(points, null, null, false));
            }
            List<Float> fresh = new ArrayList<>();
            fresh.add((float) x);
            fresh.add((float) y);
            return fresh;
        }
    }

    public static class Construction implements Computation {

        private final ConstructionSolver solver = new ConstructionSolver();

        public RepresentationKind kind() {
            return RepresentationKind.CONSTRUCTION;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ObjectDefinition.GeometricConstruction;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.number("extent", 10, 0.1, 1e6, "Half-length a line is drawn with"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return ((ObjectDefinition.GeometricConstruction) object.definition()).elements().size() * 10L;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            ConstructionSolver.Solution solution = solver.solve(object,
                    (ObjectDefinition.GeometricConstruction) object.definition(), settings.number("extent"));
            return new Dataset.Composite(Map.of("elements", solution.features(), "drawing", solution.drawing()));
        }
    }
}
