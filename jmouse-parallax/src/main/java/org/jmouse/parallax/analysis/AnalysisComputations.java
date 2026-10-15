package org.jmouse.parallax.analysis;

import org.jmouse.formula.FormulaPrinter;
import org.jmouse.formula.Quadrature;
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
import org.jmouse.parallax.numerics.NewtonSolver;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.ExplicitCurve;
import org.jmouse.parallax.object.ObjectDefinition.ExplicitSurface;
import org.jmouse.parallax.object.ObjectDefinition.ImplicitCurve;
import org.jmouse.parallax.object.ObjectDefinition.ImplicitSurface;
import org.jmouse.parallax.object.ObjectDefinition.ParametricCurve;
import org.jmouse.parallax.object.ObjectDefinition.ParametricSurface;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What calculus says about an object: level sets, sections, projections, slopes, curvature, extrema, integrals. */
public final class AnalysisComputations {

    private AnalysisComputations() {
    }

    public enum SectionAxis { X, Y, Z }

    /**
     * Where a function of two inputs takes a value, traced cell by cell by marching squares: an implicit curve's
     * f(x, y) = 0; evenly spaced levels of z = f(x, y), with the heights beneath them; an implicit surface's
     * f(x, y, z) = 0 in the plane where one input is held at {@code offset} — the circle that grows and shrinks as
     * the plane moves through a sphere.
     */
    public static class LevelSet implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.LEVEL_SET;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ImplicitCurve || definition instanceof ExplicitSurface
                    || definition instanceof ImplicitSurface;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("resolution", 200, 16, 1024, "Samples per axis"),
                    SettingSpecification.integer("levels", 12, 1, 100, "Evenly spaced levels of a surface z = f(x, y)"),
                    SettingSpecification.choice("axis", SectionAxis.Z, "The input an implicit surface's cutting plane holds"),
                    SettingSpecification.number("offset", 0, -1e6, 1e6, "The value that input is held at"),
                    PlaneWindow.specification());
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return Sampling.square(settings.integer("resolution"));
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            int resolution = settings.integer("resolution");
            return switch (object.definition()) {
                case ImplicitCurve curve -> {
                    BoundFunction function = object.function(curve.expression(), curve.variables());
                    Interval[] region = PlaneWindow.region(settings, curve.x(), curve.y());
                    float[] values = Sampling.grid(function::at, region[0], region[1], resolution, resolution, progress);
                    yield zeroLevel(values, resolution, region, "0");
                }
                case ExplicitSurface surface -> levels(object, surface, resolution, settings, progress);
                case ImplicitSurface surface -> section(object, surface, resolution, settings, progress);
                default -> throw new IllegalArgumentException("A level set is of an output of two inputs, or of three with one held.");
            };
        }

        private static Dataset.Polylines zeroLevel(float[] values, int resolution, Interval[] region, String label) {
            return new Dataset.Polylines(2, MarchingSquares.contours(values, resolution, resolution, region[0], region[1], 0, label),
                    new double[]{region[0].minimum(), region[1].minimum()}, new double[]{region[0].maximum(), region[1].maximum()});
        }

        private static Dataset levels(CompiledObject object, ExplicitSurface surface, int resolution, ComputationSettings settings,
                                      ComputationProgress progress) {
            int levels = settings.integer("levels");
            BoundFunction function = object.function(surface.expression(), surface.variables());
            Interval[] region = PlaneWindow.region(settings, surface.x(), surface.y());
            float[] values = Sampling.grid(function::at, region[0], region[1], resolution, resolution, progress);
            double[] range = Sampling.range(values);
            List<Dataset.Polyline> lines = new ArrayList<>();
            for (int level = 1; level <= levels; level++) {
                double value = range[0] + (range[1] - range[0]) * level / (levels + 1);
                for (Dataset.Polyline line : MarchingSquares.contours(values, resolution, resolution, region[0], region[1],
                        value, Double.toString(value))) {
                    float[] scalars = new float[line.points().length / 2];
                    java.util.Arrays.fill(scalars, (float) value);
                    lines.add(new Dataset.Polyline(line.points(), scalars, line.label(), line.closed()));
                }
            }
            return new Dataset.Composite(Map.of(
                    "lines", new Dataset.Polylines(2, lines, new double[]{region[0].minimum(), region[1].minimum()},
                            new double[]{region[0].maximum(), region[1].maximum()}),
                    "heights", new Dataset.ScalarGrid(resolution, resolution, region[0], region[1], values, range[0], range[1],
                            "height")));
        }

        private static Dataset section(CompiledObject object, ImplicitSurface surface, int resolution, ComputationSettings settings,
                                       ComputationProgress progress) {
            SectionAxis axis = settings.choice("axis", SectionAxis.class);
            double offset = settings.number("offset");
            BoundFunction function = object.function(surface.expression(), surface.variables());
            Interval[] region = PlaneWindow.region(settings, axis == SectionAxis.X ? surface.y() : surface.x(),
                    axis == SectionAxis.Z ? surface.y() : surface.z());
            float[] values = Sampling.grid((first, second) -> switch (axis) {
                case X -> function.at(offset, first, second);
                case Y -> function.at(first, offset, second);
                case Z -> function.at(first, second, offset);
            }, region[0], region[1], resolution, resolution, progress);
            return zeroLevel(values, resolution, region, axis.name().toLowerCase(java.util.Locale.ROOT) + " = " + offset);
        }
    }

    /**
     * A vertical plane cut through z = f(x, y): x = c or y = c gives the profile curve, and the curve in space it lies
     * on. (A horizontal cut is a level of the surface, and an implicit surface's section is a level set too.)
     */
    public static class CrossSection implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.CROSS_SECTION;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ExplicitSurface;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.choice("axis", SectionAxis.Y, "The axis the cutting plane is perpendicular to"),
                    SettingSpecification.number("offset", 0, -1e6, 1e6, "Where the plane cuts that axis"),
                    SettingSpecification.integer("resolution", 400, 16, 4000, "Samples along the section"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return settings.integer("resolution");
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            ExplicitSurface surface = (ExplicitSurface) object.definition();
            SectionAxis axis = settings.choice("axis", SectionAxis.class);
            double offset = settings.number("offset");
            int resolution = settings.integer("resolution");
            if (axis == SectionAxis.Z) {
                throw new IllegalArgumentException("A horizontal section of z = f(x, y) is one of its levels; use LEVEL_SET.");
            }
            BoundFunction function = object.function(surface.expression(), surface.variables());
            Interval along = axis == SectionAxis.X ? surface.y() : surface.x();
            float[] space = new float[resolution * 3];
            float[] profile = new float[resolution * 2];
            for (int index = 0; index < resolution; index++) {
                double moving = along.sample(index, resolution);
                double x = axis == SectionAxis.X ? offset : moving;
                double y = axis == SectionAxis.X ? moving : offset;
                double z = function.at(x, y);
                space[index * 3] = (float) x;
                space[index * 3 + 1] = (float) y;
                space[index * 3 + 2] = (float) z;
                profile[index * 2] = (float) moving;
                profile[index * 2 + 1] = (float) z;
            }
            String section = axis == SectionAxis.X ? "f(" + offset + ", y)" : "f(x, " + offset + ")";
            return new Dataset.Composite(Map.of(
                    "space", new Dataset.Polylines(3, List.of(new Dataset.Polyline(space, null, section, false)),
                            new double[]{surface.x().minimum(), surface.y().minimum(), 0},
                            new double[]{surface.x().maximum(), surface.y().maximum(), 0}),
                    "profile", new Dataset.Polylines(2, List.of(new Dataset.Polyline(profile, null, section, false)),
                            new double[]{along.minimum(), 0}, new double[]{along.maximum(), 0})));
        }
    }

    public enum ProjectionKind { ORTHOGRAPHIC, PERSPECTIVE, STEREOGRAPHIC, POLAR, CYLINDRICAL, SPHERICAL, MERCATOR }

    /**
     * The object's geometry carried to the plane by a projection. Orthographic and perspective look along a view
     * direction (azimuth, elevation); stereographic projects the unit sphere from its north pole; the map
     * projections read the point in spherical or cylindrical coordinates.
     */
    public static class Projection implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.PROJECTION;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ExplicitSurface
                    || (definition instanceof ParametricSurface surface && surface.components().size() == 3)
                    || (definition instanceof ParametricCurve curve && curve.components().size() == 3);
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.choice("projection", ProjectionKind.ORTHOGRAPHIC, "How space reaches the plane"),
                    SettingSpecification.number("azimuth", 45, -360, 360, "View azimuth, degrees"),
                    SettingSpecification.number("elevation", 30, -90, 90, "View elevation, degrees"),
                    SettingSpecification.number("distance", 6, 0.1, 1000, "Eye distance for the perspective projection"),
                    SettingSpecification.integer("resolution", 64, 8, 256, "Samples per axis"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return Sampling.square(settings.integer("resolution")) * 3;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            ProjectionKind projection = settings.choice("projection", ProjectionKind.class);
            double azimuth = Math.toRadians(settings.number("azimuth"));
            double elevation = Math.toRadians(settings.number("elevation"));
            double distance = settings.number("distance");
            int resolution = settings.integer("resolution");
            if (object.definition() instanceof ParametricCurve curve) {
                List<BoundFunction> components = object.functions(curve.components(), curve.variables());
                float[] points = new float[resolution * 4 * 2];
                int samples = resolution * 4;
                double[] point = new double[3];
                for (int index = 0; index < samples; index++) {
                    double t = curve.t().sample(index, samples);
                    for (int axis = 0; axis < 3; axis++) {
                        point[axis] = components.get(axis).at(t);
                    }
                    double[] planar = project(point, projection, azimuth, elevation, distance);
                    points[index * 2] = (float) planar[0];
                    points[index * 2 + 1] = (float) planar[1];
                }
                return new Dataset.Polylines(2, List.of(new Dataset.Polyline(points, null, projection.name(), curve.closed())),
                        new double[]{0, 0}, new double[]{0, 0});
            }
            Dataset.Mesh mesh = switch (object.definition()) {
                case ExplicitSurface surface -> {
                    BoundFunction height = object.function(surface.expression(), surface.variables());
                    yield GridMesh.build((x, y, target) -> {
                        target[0] = x;
                        target[1] = y;
                        target[2] = height.at(x, y);
                    }, 3, surface.x(), surface.y(), resolution, resolution, null, null, List.of(), progress);
                }
                case ParametricSurface surface -> org.jmouse.parallax.geometry
                        .GeometryComputations.Surface.parametric(object, surface, resolution, progress);
                default -> throw new IllegalArgumentException("nothing to project");
            };
            float[] positions = mesh.positions();
            int vertices = positions.length / 3;
            float[] planar = new float[vertices * 2];
            double[] minimum = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
            double[] maximum = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
            for (int vertex = 0; vertex < vertices; vertex++) {
                double[] projected = project(new double[]{positions[vertex * 3], positions[vertex * 3 + 1], positions[vertex * 3 + 2]},
                        projection, azimuth, elevation, distance);
                planar[vertex * 2] = (float) projected[0];
                planar[vertex * 2 + 1] = (float) projected[1];
                if (Double.isFinite(projected[0]) && Double.isFinite(projected[1])) {
                    for (int axis = 0; axis < 2; axis++) {
                        minimum[axis] = Math.min(minimum[axis], projected[axis]);
                        maximum[axis] = Math.max(maximum[axis], projected[axis]);
                    }
                }
            }
            return new Dataset.Mesh(2, planar, null, mesh.indices(), null, null, minimum, maximum, List.of());
        }

        static double[] project(double[] point, ProjectionKind projection, double azimuth, double elevation, double distance) {
            double x = point[0];
            double y = point[1];
            double z = point[2];
            return switch (projection) {
                case ORTHOGRAPHIC, PERSPECTIVE -> {
                    double rotatedX = x * Math.cos(azimuth) - y * Math.sin(azimuth);
                    double rotatedY = x * Math.sin(azimuth) + y * Math.cos(azimuth);
                    double screenY = z * Math.cos(elevation) - rotatedY * Math.sin(elevation);
                    double depth = z * Math.sin(elevation) + rotatedY * Math.cos(elevation);
                    if (projection == ProjectionKind.ORTHOGRAPHIC) {
                        yield new double[]{rotatedX, screenY};
                    }
                    double scale = distance / Math.max(1e-6, distance + depth);
                    yield new double[]{rotatedX * scale, screenY * scale};
                }
                case STEREOGRAPHIC -> {
                    double radius = Math.sqrt(x * x + y * y + z * z);
                    double unitZ = radius == 0 ? 0 : z / radius;
                    double denominator = 1 - unitZ;
                    yield new double[]{(radius == 0 ? 0 : x / radius) / denominator, (radius == 0 ? 0 : y / radius) / denominator};
                }
                case POLAR -> new double[]{Math.atan2(y, x), Math.hypot(x, y)};
                case CYLINDRICAL -> new double[]{Math.atan2(y, x), z};
                case SPHERICAL -> new double[]{Math.atan2(y, x), Math.acos(z / Math.max(1e-12, Math.sqrt(x * x + y * y + z * z)))};
                case MERCATOR -> {
                    double latitude = Math.asin(Math.max(-1, Math.min(1, z / Math.max(1e-12, Math.sqrt(x * x + y * y + z * z)))));
                    yield new double[]{Math.atan2(y, x), Math.log(Math.tan(Math.PI / 4 + latitude / 2))};
                }
            };
        }
    }

    /** ∇f on a grid, exact, with the symbolic partial derivatives. */
    public static class Gradient implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.GRADIENT;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ExplicitSurface;
        }

        public List<SettingSpecification> settings() {
            return List.of(Sampling.FIELD_RESOLUTION);
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return Sampling.square(settings.integer("resolution")) * 2;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            ExplicitSurface surface = (ExplicitSurface) object.definition();
            Differentials differentials = new Differentials(object, surface.expression());
            int resolution = settings.integer("resolution");
            float[] origins = new float[resolution * resolution * 3];
            float[] vectors = new float[resolution * resolution * 3];
            float[] magnitudes = new float[resolution * resolution];
            double largest = 0;
            for (int row = 0; row < resolution; row++) {
                progress.checkpoint((double) row / resolution, "gradient");
                double y = surface.y().sample(row, resolution);
                for (int column = 0; column < resolution; column++) {
                    double x = surface.x().sample(column, resolution);
                    int index = row * resolution + column;
                    double p = differentials.dx.at(x, y);
                    double q = differentials.dy.at(x, y);
                    origins[index * 3] = (float) x;
                    origins[index * 3 + 1] = (float) y;
                    origins[index * 3 + 2] = (float) differentials.value.at(x, y);
                    vectors[index * 3] = (float) p;
                    vectors[index * 3 + 1] = (float) q;
                    double magnitude = Math.hypot(p, q);
                    magnitudes[index] = (float) magnitude;
                    if (Double.isFinite(magnitude)) {
                        largest = Math.max(largest, magnitude);
                    }
                }
            }
            return new Dataset.Composite(Map.of(
                    "field", new Dataset.VectorSamples(3, origins, vectors, magnitudes, largest),
                    "symbolic", new Dataset.Symbolic(differentials.symbolicText(), differentials.symbolicLatex())));
        }
    }

    /** Unit normals on a coarse grid: (-fx, -fy, 1) for z = f(x, y), r_u × r_v for a parametric surface. */
    public static class Normals implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.NORMALS;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ExplicitSurface
                    || (definition instanceof ParametricSurface surface && surface.components().size() == 3);
        }

        public List<SettingSpecification> settings() {
            return List.of(Sampling.FIELD_RESOLUTION);
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return Sampling.square(settings.integer("resolution")) * 9;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            int resolution = settings.integer("resolution");
            float[] origins = new float[resolution * resolution * 3];
            float[] vectors = new float[resolution * resolution * 3];
            float[] magnitudes = new float[resolution * resolution];
            SurfaceFrame frame = SurfaceFrame.of(object);
            for (int row = 0; row < resolution; row++) {
                progress.checkpoint((double) row / resolution, "normals");
                double v = frame.v().sample(row, resolution);
                for (int column = 0; column < resolution; column++) {
                    double u = frame.u().sample(column, resolution);
                    int index = row * resolution + column;
                    double[] position = frame.position(u, v);
                    double[] normal = frame.unitNormal(u, v);
                    for (int axis = 0; axis < 3; axis++) {
                        origins[index * 3 + axis] = (float) position[axis];
                        vectors[index * 3 + axis] = (float) normal[axis];
                    }
                    magnitudes[index] = 1;
                }
            }
            return new Dataset.VectorSamples(3, origins, vectors, magnitudes, 1);
        }
    }

    public enum CurvatureMeasure { GAUSSIAN, MEAN, PRINCIPAL_MAXIMUM, PRINCIPAL_MINIMUM }

    /** The surface coloured by its curvature, or a curve's curvature κ along it. */
    public static class Curvature implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.CURVATURE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ExplicitSurface || definition instanceof ExplicitCurve
                    || (definition instanceof ParametricSurface surface && surface.components().size() == 3)
                    || (definition instanceof ParametricCurve curve && curve.components().size() <= 3);
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.choice("measure", CurvatureMeasure.GAUSSIAN, "Which curvature colours a surface"),
                    Sampling.RESOLUTION);
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return Sampling.square(settings.integer("resolution")) * 12;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            int resolution = settings.integer("resolution");
            CurvatureMeasure measure = settings.choice("measure", CurvatureMeasure.class);
            return switch (object.definition()) {
                case ExplicitCurve curve -> {
                    String variable = curve.variables().getFirst();
                    org.jmouse.formula.Formula firstExpression = object.derivativeExpression(curve.expression(), variable);
                    BoundFunction first = object.function(firstExpression, curve.variables());
                    BoundFunction second = object.function(object.derivativeExpression(firstExpression, variable), curve.variables());
                    BoundFunction function = object.function(curve.expression(), curve.variables());
                    yield curveWithCurvature(resolution * 4, curve.x(), t -> new double[]{t, function.at(t)},
                            t -> Math.abs(second.at(t)) / Math.pow(1 + first.at(t) * first.at(t), 1.5), 2);
                }
                case ParametricCurve curve -> {
                    int dimension = curve.components().size();
                    List<BoundFunction> position = object.functions(curve.components(), curve.variables());
                    List<BoundFunction> velocity = new ArrayList<>();
                    List<BoundFunction> acceleration = new ArrayList<>();
                    String parameter = curve.variables().getFirst();
                    for (String component : curve.components()) {
                        org.jmouse.formula.Formula derivative = object.derivativeExpression(component, parameter);
                        velocity.add(object.function(derivative, curve.variables()));
                        acceleration.add(object.function(object.derivativeExpression(derivative, parameter), curve.variables()));
                    }
                    yield curveWithCurvature(resolution * 4, curve.t(), t -> evaluate(position, t),
                            t -> curvature(evaluate(velocity, t), evaluate(acceleration, t)), dimension);
                }
                default -> {
                    SurfaceFrame frame = SurfaceFrame.of(object);
                    Dataset.Mesh mesh = GridMesh.build((u, v, target) -> System.arraycopy(frame.position(u, v), 0, target, 0, 3),
                            3, frame.u(), frame.v(), resolution, resolution, (u, v, point) -> frame.curvature(u, v, measure),
                            measure.name().toLowerCase() + "-curvature", List.of(), progress);
                    yield mesh;
                }
            };
        }

        private static double[] evaluate(List<BoundFunction> functions, double t) {
            double[] values = new double[functions.size()];
            for (int index = 0; index < values.length; index++) {
                values[index] = functions.get(index).at(t);
            }
            return values;
        }

        /** κ = |r' × r''| / |r'|³ (in the plane: |x'y'' - y'x''| / |r'|³). */
        private static double curvature(double[] velocity, double[] acceleration) {
            double speed = Math.sqrt(dot(velocity, velocity));
            double crossNorm;
            if (velocity.length == 2) {
                crossNorm = Math.abs(velocity[0] * acceleration[1] - velocity[1] * acceleration[0]);
            } else {
                double cx = velocity[1] * acceleration[2] - velocity[2] * acceleration[1];
                double cy = velocity[2] * acceleration[0] - velocity[0] * acceleration[2];
                double cz = velocity[0] * acceleration[1] - velocity[1] * acceleration[0];
                crossNorm = Math.sqrt(cx * cx + cy * cy + cz * cz);
            }
            return crossNorm / (speed * speed * speed);
        }

        private static Dataset.Polylines curveWithCurvature(int samples, Interval parameter,
                                                            java.util.function.DoubleFunction<double[]> position,
                                                            java.util.function.DoubleUnaryOperator curvature, int dimension) {
            float[] points = new float[samples * dimension];
            float[] scalars = new float[samples];
            for (int index = 0; index < samples; index++) {
                double t = parameter.sample(index, samples);
                double[] point = position.apply(t);
                for (int axis = 0; axis < dimension; axis++) {
                    points[index * dimension + axis] = (float) point[axis];
                }
                scalars[index] = (float) curvature.applyAsDouble(t);
            }
            return new Dataset.Polylines(dimension, List.of(new Dataset.Polyline(points, scalars, "curvature", false)),
                    new double[dimension], new double[dimension]);
        }
    }

    /**
     * The tangent plane of z = f(x, y) at (x₀, y₀), or the tangent line of y = f(x) at x₀ together with the secant
     * through x₀ and x₀ + h — as h shrinks the secant turns into the tangent.
     */
    public static class TangentPlane implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.TANGENT_PLANE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ExplicitSurface || definition instanceof ExplicitCurve;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.vector("at", List.of(), 2, -1e6, 1e6, "The point of tangency (x₀, y₀); x₀ alone for a curve"),
                    SettingSpecification.number("size", 1, 0.01, 1e4, "Half-width of the drawn patch"),
                    SettingSpecification.number("secant", 0.5, -1e4, 1e4, "h of the secant through x₀ and x₀ + h"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return 32;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            double size = settings.number("size");
            return switch (object.definition()) {
                case ExplicitSurface surface -> {
                    double[] at = settings.vectorOr("at", new double[]{surface.x().at(0.5), surface.y().at(0.5)});
                    Differentials differentials = new Differentials(object, surface.expression());
                    double x0 = at[0];
                    double y0 = at[1];
                    double z0 = differentials.value.at(x0, y0);
                    double p = differentials.dx.at(x0, y0);
                    double q = differentials.dy.at(x0, y0);
                    Dataset.Mesh patch = GridMesh.build((x, y, target) -> {
                        target[0] = x;
                        target[1] = y;
                        target[2] = z0 + p * (x - x0) + q * (y - y0);
                    }, 3, Interval.of(x0 - size, x0 + size), Interval.of(y0 - size, y0 + size), 2, 2, null, null, List.of(),
                            ComputationProgress.NONE);
                    Map<String, String> text = Map.of("plane", "z = " + z0 + " + " + p + "(x - " + x0 + ") + " + q + "(y - " + y0 + ")");
                    Map<String, Double> values = new LinkedHashMap<>();
                    values.put("x0", x0);
                    values.put("y0", y0);
                    values.put("z0", z0);
                    values.put("fx", p);
                    values.put("fy", q);
                    yield new Dataset.Composite(Map.of("plane", patch, "equation", new Dataset.Symbolic(text, text),
                            "values", new Dataset.Measurements(values, Map.of())));
                }
                case ExplicitCurve curve -> {
                    double x0 = settings.vectorOr("at", new double[]{curve.x().at(0.5), 0})[0];
                    double h = settings.number("secant");
                    BoundFunction function = object.function(curve.expression(), curve.variables());
                    BoundFunction slope = object.function(object.derivativeExpression(curve.expression(),
                            curve.variables().getFirst()), curve.variables());
                    double y0 = function.at(x0);
                    double tangentSlope = slope.at(x0);
                    double secantSlope = h == 0 ? tangentSlope : (function.at(x0 + h) - y0) / h;
                    float[] tangent = {(float) (x0 - size), (float) (y0 - size * tangentSlope), (float) (x0 + size), (float) (y0 + size * tangentSlope)};
                    float[] secant = {(float) (x0 - size), (float) (y0 - size * secantSlope), (float) (x0 + size), (float) (y0 + size * secantSlope)};
                    Map<String, Double> values = new LinkedHashMap<>();
                    values.put("x0", x0);
                    values.put("y0", y0);
                    values.put("slope", tangentSlope);
                    values.put("secantSlope", secantSlope);
                    values.put("h", h);
                    yield new Dataset.Composite(Map.of(
                            "lines", new Dataset.Polylines(2, List.of(new Dataset.Polyline(tangent, null, "tangent", false),
                                    new Dataset.Polyline(secant, null, "secant", false)), new double[]{x0 - size, 0}, new double[]{x0 + size, 0}),
                            "values", new Dataset.Measurements(values, Map.of())));
                }
                default -> throw new IllegalArgumentException("no tangent for this object");
            };
        }
    }

    /**
     * Where the slope vanishes. For z = f(x, y): ∇f = 0 solved from a grid of starts and classified by the Hessian
     * (minimum, maximum, saddle, degenerate). For y = f(x): extrema (f' = 0) and inflection points (f'' = 0).
     */
    public static class CriticalPoints implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.CRITICAL_POINTS;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ExplicitSurface || definition instanceof ExplicitCurve;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("seeds", 12, 2, 40, "Newton starts per axis"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return Sampling.square(settings.integer("seeds")) * 400;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            int seeds = settings.integer("seeds");
            List<Dataset.Feature> features = new ArrayList<>();
            switch (object.definition()) {
                case ExplicitSurface surface -> {
                    Differentials differentials = new Differentials(object, surface.expression());
                    List<double[]> roots = NewtonSolver.rootsInBox((point, value) -> {
                        value[0] = differentials.dx.at(point[0], point[1]);
                        value[1] = differentials.dy.at(point[0], point[1]);
                    }, new double[]{surface.x().minimum(), surface.y().minimum()},
                            new double[]{surface.x().maximum(), surface.y().maximum()}, seeds, 1e-10);
                    for (double[] root : roots) {
                        double fxx = differentials.dxx.at(root[0], root[1]);
                        double fyy = differentials.dyy.at(root[0], root[1]);
                        double fxy = differentials.dxy.at(root[0], root[1]);
                        double determinant = fxx * fyy - fxy * fxy;
                        String classification = Math.abs(determinant) < 1e-9 ? "DEGENERATE"
                                : determinant < 0 ? "SADDLE" : fxx > 0 ? "MINIMUM" : "MAXIMUM";
                        Map<String, Object> facts = new LinkedHashMap<>();
                        facts.put("hessianDeterminant", determinant);
                        facts.put("fxx", fxx);
                        facts.put("fyy", fyy);
                        facts.put("fxy", fxy);
                        double z = differentials.value.at(root[0], root[1]);
                        features.add(new Dataset.Feature(new double[]{root[0], root[1], z}, z, classification, facts));
                    }
                }
                case ExplicitCurve curve -> {
                    BoundFunction function = object.function(curve.expression(), curve.variables());
                    org.jmouse.formula.Formula firstExpression = object.derivativeExpression(curve.expression(), curve.variables().getFirst());
                    BoundFunction first = object.function(firstExpression, curve.variables());
                    BoundFunction second = object.function(object.derivativeExpression(firstExpression, curve.variables().getFirst()), curve.variables());
                    for (double[] root : NewtonSolver.rootsInBox((point, value) -> value[0] = first.at(point[0]),
                            new double[]{curve.x().minimum()}, new double[]{curve.x().maximum()}, seeds * 8, 1e-11)) {
                        double curvature = second.at(root[0]);
                        String classification = Math.abs(curvature) < 1e-9 ? "STATIONARY" : curvature > 0 ? "MINIMUM" : "MAXIMUM";
                        features.add(new Dataset.Feature(new double[]{root[0], function.at(root[0])}, function.at(root[0]),
                                classification, Map.of("secondDerivative", curvature)));
                    }
                    BoundFunction third = object.function(object.derivativeExpression(second.expression(), curve.variables().getFirst()), curve.variables());
                    for (double[] root : NewtonSolver.rootsInBox((point, value) -> value[0] = second.at(point[0]),
                            new double[]{curve.x().minimum()}, new double[]{curve.x().maximum()}, seeds * 8, 1e-11)) {
                        if (Math.abs(third.at(root[0])) > 1e-9) {
                            features.add(new Dataset.Feature(new double[]{root[0], function.at(root[0])}, function.at(root[0]),
                                    "INFLECTION", Map.of()));
                        }
                    }
                }
                default -> throw new IllegalArgumentException("no critical points for this object");
            }
            return new Dataset.FeaturePoints(features);
        }
    }

    /** The derivative as a formula and as a curve: f′ of y = f(x), the partials of z = f(x, y), the velocity of a path. */
    public static class Derivative implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.DERIVATIVE;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ExplicitCurve || definition instanceof ExplicitSurface
                    || definition instanceof ParametricCurve;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("order", 1, 1, 4, "Which derivative"), Sampling.CURVE_SAMPLES);
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return settings.integer("samples") * 4L;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            int order = settings.integer("order");
            int samples = settings.integer("samples");
            return switch (object.definition()) {
                case ExplicitCurve curve -> {
                    org.jmouse.formula.Formula current = object.expression(curve.expression());
                    Map<String, String> text = new LinkedHashMap<>();
                    Map<String, String> latex = new LinkedHashMap<>();
                    for (int step = 1; step <= order; step++) {
                        current = object.derivativeExpression(current, curve.variables().getFirst());
                        text.put("f" + "'".repeat(step), FormulaPrinter.text(current));
                        latex.put("f" + "'".repeat(step), FormulaPrinter.latex(current));
                    }
                    BoundFunction derivative = object.function(current, curve.variables());
                    yield new Dataset.Composite(Map.of("symbolic", new Dataset.Symbolic(text, latex),
                            "curve", org.jmouse.parallax.geometry.GeometryComputations.Curve
                                    .polylines(2, samples, curve.x(), x -> new double[]{x, derivative.at(x)}, false)));
                }
                case ExplicitSurface surface -> {
                    Differentials differentials = new Differentials(object, surface.expression());
                    yield new Dataset.Symbolic(differentials.symbolicText(), differentials.symbolicLatex());
                }
                case ParametricCurve curve -> {
                    Map<String, String> text = new LinkedHashMap<>();
                    Map<String, String> latex = new LinkedHashMap<>();
                    String[] axes = {"x", "y", "z", "w"};
                    for (int axis = 0; axis < curve.components().size(); axis++) {
                        org.jmouse.formula.Formula derivative = object.expression(curve.components().get(axis));
                        for (int step = 1; step <= order; step++) {
                            derivative = object.derivativeExpression(derivative, curve.variables().getFirst());
                        }
                        String argument = "(" + curve.variables().getFirst() + ")";
                        text.put(axes[axis] + "'".repeat(order) + argument, FormulaPrinter.text(derivative));
                        latex.put(axes[axis] + "'".repeat(order) + argument, FormulaPrinter.latex(derivative));
                    }
                    yield new Dataset.Symbolic(text, latex);
                }
                default -> throw new IllegalArgumentException("no derivative for this object");
            };
        }
    }

    public enum RiemannRule { LEFT, RIGHT, MIDPOINT, TRAPEZOID }

    /**
     * Accumulation made visible: ∫ f over [a, b] with the Riemann rectangles that approximate it and the running
     * integral F(x); the volume under z = f(x, y); the arc length of a path.
     */
    public static class Integral implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.INTEGRAL;
        }

        public boolean accepts(ObjectDefinition definition) {
            return definition instanceof ExplicitCurve || definition instanceof ExplicitSurface
                    || definition instanceof ParametricCurve;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.vector("bounds", List.of(), 2, -1e6, 1e6, "[a, b]; the domain when empty"),
                    SettingSpecification.integer("rectangles", 16, 1, 2000, "Riemann rectangles drawn"),
                    SettingSpecification.choice("rule", RiemannRule.MIDPOINT, "Where each rectangle samples f"),
                    SettingSpecification.integer("samples", 400, 16, 20_000, "Points of the accumulation curve"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            // One quadrature per sample of the accumulation curve and one for the exact value, plus the rectangles.
            return (settings.integer("samples") + 1L) * Quadrature.TYPICAL_EVALUATIONS + 3L * settings.integer("rectangles");
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            return switch (object.definition()) {
                case ExplicitCurve curve -> curveIntegral(object, curve, settings, progress);
                case ExplicitSurface surface -> {
                    BoundFunction function = object.function(surface.expression(), surface.variables());
                    double volume = Quadrature.integrate(x -> Quadrature.integrate(y -> function.copy().at(x, y),
                            surface.y().minimum(), surface.y().maximum()), surface.x().minimum(), surface.x().maximum());
                    yield new Dataset.Measurements(Map.of("volume", volume), Map.of("volume", "∬ f dA over the domain rectangle"));
                }
                case ParametricCurve curve -> {
                    List<BoundFunction> velocity = new ArrayList<>();
                    String parameter = curve.variables().getFirst();
                    for (String component : curve.components()) {
                        velocity.add(object.function(object.derivativeExpression(component, parameter), curve.variables()));
                    }
                    double length = Quadrature.integrate(t -> {
                        double sum = 0;
                        for (BoundFunction component : velocity) {
                            double value = component.at(t);
                            sum += value * value;
                        }
                        return Math.sqrt(sum);
                    }, curve.t().minimum(), curve.t().maximum());
                    yield new Dataset.Measurements(Map.of("arcLength", length), Map.of("arcLength", "∫ |r'(t)| dt"));
                }
                default -> throw new IllegalArgumentException("no integral for this object");
            };
        }

        private Dataset curveIntegral(CompiledObject object, ExplicitCurve curve, ComputationSettings settings,
                                      ComputationProgress progress) {
            double[] bounds = settings.vectorOr("bounds", new double[]{curve.x().minimum(), curve.x().maximum()});
            double from = bounds[0];
            double to = bounds[1];
            BoundFunction function = object.function(curve.expression(), curve.variables());
            double exact = Quadrature.integrate(function::at, from, to);
            int rectangles = settings.integer("rectangles");
            RiemannRule rule = settings.choice("rule", RiemannRule.class);
            double width = (to - from) / rectangles;
            double riemann = 0;
            List<Dataset.Polyline> shapes = new ArrayList<>();
            for (int index = 0; index < rectangles; index++) {
                double left = from + index * width;
                double right = left + width;
                double heightLeft = function.at(left);
                double heightRight = function.at(right);
                double height = switch (rule) {
                    case LEFT -> heightLeft;
                    case RIGHT -> heightRight;
                    case MIDPOINT -> function.at((left + right) / 2);
                    case TRAPEZOID -> (heightLeft + heightRight) / 2;
                };
                riemann += height * width;
                float[] outline = rule == RiemannRule.TRAPEZOID
                        ? new float[]{(float) left, 0, (float) left, (float) heightLeft, (float) right, (float) heightRight, (float) right, 0}
                        : new float[]{(float) left, 0, (float) left, (float) height, (float) right, (float) height, (float) right, 0};
                shapes.add(new Dataset.Polyline(outline, null, "rectangle", true));
            }
            int samples = settings.integer("samples");
            float[] axis = new float[samples];
            float[] accumulated = new float[samples];
            double running = 0;
            double previousX = from;
            for (int index = 0; index < samples; index++) {
                progress.checkpoint((double) index / samples, "integrating");
                double x = from + (to - from) * index / (samples - 1);
                if (index > 0) {
                    running += Quadrature.integrate(function::at, previousX, x);
                }
                previousX = x;
                axis[index] = (float) x;
                accumulated[index] = (float) running;
            }
            Map<String, Double> values = new LinkedHashMap<>();
            values.put("integral", exact);
            values.put("riemannSum", riemann);
            values.put("error", riemann - exact);
            values.put("from", from);
            values.put("to", to);
            return new Dataset.Composite(Map.of(
                    "values", new Dataset.Measurements(values, Map.of("rule", rule.name())),
                    "rectangles", new Dataset.Polylines(2, shapes, new double[]{from, 0}, new double[]{to, 0}),
                    "accumulation", new Dataset.TimeSeries(curve.variables().getFirst(), axis,
                            List.of("F(" + curve.variables().getFirst() + ")"), List.of(accumulated))));
        }
    }

    static double dot(double[] first, double[] second) {
        double sum = 0;
        for (int index = 0; index < first.length; index++) {
            sum += first[index] * second[index];
        }
        return sum;
    }
}
