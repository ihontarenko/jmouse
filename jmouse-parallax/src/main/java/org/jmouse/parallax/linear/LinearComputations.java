package org.jmouse.parallax.linear;

import org.jmouse.formula.Complex;
import org.jmouse.parallax.Computation;
import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.ComputationSettings;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.SettingSpecification;
import org.jmouse.parallax.numerics.LinearAlgebra;
import org.jmouse.parallax.object.BoundFunction;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.Interval;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.object.ObjectDefinition.AffineTransformation;
import org.jmouse.parallax.object.ObjectDefinition.ComplexMapping;
import org.jmouse.parallax.object.ObjectDefinition.MatrixObject;
import org.jmouse.parallax.object.ObjectDefinition.NonlinearTransformation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Matrices as geometry: the grid, basis and unit square before and after the map (a complex w = f(z) is a map of
 * the plane too); eigenvectors as the directions it only stretches; the determinant as the change of area; the SVD
 * as rotate · stretch · rotate; a vector carried step by step by the matrix, and the vector it leaves fixed.
 */
public final class LinearComputations {

    private LinearComputations() {
    }

    static boolean isMatrix(ObjectDefinition definition) {
        return definition instanceof MatrixObject || definition instanceof AffineTransformation;
    }

    static double[][] matrixOf(CompiledObject object) {
        List<List<String>> entries = switch (object.definition()) {
            case MatrixObject matrix -> matrix.entries();
            case AffineTransformation transformation -> transformation.matrix();
            default -> throw new IllegalArgumentException("not a matrix");
        };
        double[][] matrix = new double[entries.size()][entries.getFirst().size()];
        for (int row = 0; row < matrix.length; row++) {
            for (int column = 0; column < matrix[row].length; column++) {
                matrix[row][column] = object.constant(entries.get(row).get(column));
            }
        }
        return matrix;
    }

    static double[] translationOf(CompiledObject object, int size) {
        if (object.definition() instanceof AffineTransformation transformation) {
            return transformation.translation().stream().mapToDouble(object::constant).toArray();
        }
        return new double[size];
    }

    static Dataset.MatrixAnalysis analyse(double[][] matrix) {
        int rows = matrix.length;
        int columns = matrix[0].length;
        boolean square = rows == columns;
        LinearAlgebra.Svd svd = LinearAlgebra.svd(matrix);
        return new Dataset.MatrixAnalysis(rows, columns, matrix,
                square ? LinearAlgebra.determinant(matrix) : null, LinearAlgebra.rank(matrix),
                square ? LinearAlgebra.trace(matrix) : null, square ? LinearAlgebra.eigenpairs(matrix) : List.of(),
                svd.left(), svd.values(), svd.right(), LinearAlgebra.nullSpace(matrix), LinearAlgebra.columnSpace(matrix));
    }

    /**
     * The lattice x = const, y = const (z = const in 3D), the basis vectors and the unit square or cube, each
     * before and after the map; `morph` interpolates for the before → after animation. The map is a matrix, a
     * nonlinear map of the coordinates, or a complex w = f(z) read as a map of the plane (Re z, Im z) → (Re w, Im w).
     */
    public static class TransformedGrid implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.TRANSFORMED_GRID;
        }

        public boolean accepts(ObjectDefinition definition) {
            return (definition instanceof MatrixObject matrix && matrix.entries().size() == matrix.entries().getFirst().size()
                    && matrix.entries().size() <= 3)
                    || (definition instanceof AffineTransformation transformation && transformation.matrix().size() <= 3)
                    || definition instanceof NonlinearTransformation
                    || definition instanceof ComplexMapping;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("lines", 11, 2, 64, "Grid lines per axis"),
                    SettingSpecification.number("extent", 3, 0.1, 1e4, "Half-width of the grid for a matrix"),
                    SettingSpecification.integer("samples", 64, 2, 1000, "Points per grid line"),
                    SettingSpecification.number("morph", 1, 0, 1, "0 before, 1 after the map"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return 3L * settings.integer("lines") * settings.integer("lines") * settings.integer("samples");
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            int lines = settings.integer("lines");
            int samples = settings.integer("samples");
            double morph = settings.number("morph");
            Mapping mapping = Mapping.of(object, settings.number("extent"));
            int dimension = mapping.dimension();
            List<Dataset.Polyline> before = new ArrayList<>();
            List<Dataset.Polyline> after = new ArrayList<>();
            int planes = dimension == 3 ? lines : 1;
            for (int axis = 0; axis < dimension; axis++) {
                for (int first = 0; first < lines; first++) {
                    for (int second = 0; second < planes; second++) {
                        float[] original = new float[samples * dimension];
                        float[] mapped = new float[samples * dimension];
                        for (int index = 0; index < samples; index++) {
                            double[] point = new double[dimension];
                            point[axis] = mapping.bounds().get(axis).sample(index, samples);
                            point[(axis + 1) % dimension] = mapping.bounds().get((axis + 1) % dimension).sample(first, lines);
                            if (dimension == 3) {
                                point[(axis + 2) % 3] = mapping.bounds().get((axis + 2) % 3).sample(second, planes);
                            }
                            double[] image = mapping.apply(point);
                            for (int component = 0; component < dimension; component++) {
                                original[index * dimension + component] = (float) point[component];
                                mapped[index * dimension + component] = (float) (point[component] + morph * (image[component] - point[component]));
                            }
                        }
                        before.add(new Dataset.Polyline(original, null, "axis-" + axis, false));
                        after.add(new Dataset.Polyline(mapped, null, "axis-" + axis, false));
                    }
                }
            }
            Map<String, Dataset> parts = new LinkedHashMap<>();
            parts.put("before", new Dataset.Polylines(dimension, before, new double[dimension], new double[dimension]));
            parts.put("after", new Dataset.Polylines(dimension, after, new double[dimension], new double[dimension]));
            if (mapping.matrix() != null) {
                parts.put("basis", basis(mapping, morph));
                parts.put("unitCell", unitCell(mapping, morph));
                parts.put("analysis", analyse(mapping.matrix()));
            }
            return new Dataset.Composite(parts);
        }

        private static Dataset.VectorSamples basis(Mapping mapping, double morph) {
            int dimension = mapping.dimension();
            float[] origins = new float[dimension * dimension];
            float[] vectors = new float[dimension * dimension];
            float[] magnitudes = new float[dimension];
            for (int axis = 0; axis < dimension; axis++) {
                double magnitude = 0;
                for (int component = 0; component < dimension; component++) {
                    double unit = component == axis ? 1 : 0;
                    double image = mapping.matrix()[component][axis];
                    double value = unit + morph * (image - unit);
                    vectors[axis * dimension + component] = (float) value;
                    magnitude += value * value;
                }
                magnitudes[axis] = (float) Math.sqrt(magnitude);
            }
            return new Dataset.VectorSamples(dimension, origins, vectors, magnitudes, 0);
        }

        private static Dataset.Polylines unitCell(Mapping mapping, double morph) {
            int dimension = mapping.dimension();
            double[][] corners = dimension == 2 ? new double[][]{{0, 0}, {1, 0}, {1, 1}, {0, 1}, {0, 0}}
                    : new double[][]{{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}, {0, 0, 1}};
            float[] points = new float[corners.length * dimension];
            for (int index = 0; index < corners.length; index++) {
                double[] image = LinearAlgebra.apply(mapping.matrix(), corners[index]);
                for (int component = 0; component < dimension; component++) {
                    points[index * dimension + component] = (float) (corners[index][component]
                            + morph * (image[component] - corners[index][component]));
                }
            }
            return new Dataset.Polylines(dimension, List.of(new Dataset.Polyline(points, null, "unit", dimension == 2)),
                    new double[dimension], new double[dimension]);
        }

        /**
         * The map being drawn, as the image of a point: a matrix with its translation (the matrix itself kept for the
         * basis and the unit cell), a nonlinear map of the coordinates, or a complex function of z = x + iy.
         */
        record Mapping(int dimension, double[][] matrix, UnaryOperator<double[]> image, List<Interval> bounds) {

            static Mapping of(CompiledObject object, double extent) {
                return switch (object.definition()) {
                    case NonlinearTransformation transformation -> {
                        int dimension = transformation.components().size();
                        List<String> variables = dimension == 2 ? List.of("x", "y") : List.of("x", "y", "z");
                        List<BoundFunction> components = object.functions(transformation.components(), variables);
                        yield new Mapping(dimension, null, point -> {
                            double[] image = new double[dimension];
                            for (int axis = 0; axis < dimension; axis++) {
                                image[axis] = components.get(axis).at(point);
                            }
                            return image;
                        }, transformation.bounds());
                    }
                    case ComplexMapping mapping -> {
                        CompiledObject.ComplexBinding function = object.complexFunction(mapping.expression(), mapping.variables());
                        yield new Mapping(2, null, point -> {
                            Complex value = function.at(new Complex(point[0], point[1]));
                            return new double[]{value.real(), value.imaginary()};
                        }, List.of(mapping.real(), mapping.imaginary()));
                    }
                    default -> {
                        double[][] matrix = matrixOf(object);
                        double[] translation = translationOf(object, matrix.length);
                        List<Interval> bounds = new ArrayList<>();
                        for (int axis = 0; axis < matrix.length; axis++) {
                            bounds.add(Interval.of(-extent, extent));
                        }
                        yield new Mapping(matrix.length, matrix, point -> {
                            double[] image = LinearAlgebra.apply(matrix, point);
                            for (int axis = 0; axis < image.length; axis++) {
                                image[axis] += translation[axis];
                            }
                            return image;
                        }, bounds);
                    }
                };
            }

            double[] apply(double[] point) {
                return image.apply(point);
            }
        }
    }

    /** Which way a vector is carried: x ← A x (a column vector), or x ← x A (a row vector, as a Markov chain's distribution). */
    public enum EvolutionSide { RIGHT, LEFT }

    /**
     * Everything the decomposition of a matrix knows — determinant, rank, trace, eigenpairs, SVD, null and column
     * spaces — and, as layers on request: the eigenvectors drawn as arrows, and a vector carried step by step by the
     * matrix with the vector it leaves fixed (eigenvalue 1, scaled to sum 1). The engine knows no Markov chain: a
     * document whose rows are distributions asks for {@code from [1, 0, 0] side LEFT} and gets its chain's evolution
     * and stationary distribution, while a Leslie or a Fibonacci matrix asks the same of its own vector.
     */
    public static class MatrixAnalysis implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.MATRIX_ANALYSIS;
        }

        public boolean accepts(ObjectDefinition definition) {
            return isMatrix(definition);
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.flag("eigenvectors", false,
                            "Each real eigenvector as an arrow scaled by its eigenvalue, each complex pair as the plane it turns"),
                    SettingSpecification.vector("from", List.of(), 0, -1e12, 1e12,
                            "A vector the matrix carries step by step; no evolution when empty"),
                    SettingSpecification.integer("steps", 50, 1, 100_000, "Steps of the evolution"),
                    SettingSpecification.choice("side", EvolutionSide.RIGHT, "x ← A x, or x ← x A for a row vector"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            long size = object.definition().ambientDimension();
            return 1000 + settings.integer("steps") * size * size;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            double[][] matrix = matrixOf(object);
            Dataset.MatrixAnalysis analysis = analyse(matrix);
            boolean arrows = settings.flag("eigenvectors");
            double[] start = settings.vectorOr("from", new double[0]);
            if (!arrows && start.length == 0) {
                return analysis;
            }
            if (matrix.length != matrix[0].length) {
                throw new IllegalArgumentException("Only a square matrix has eigenvectors and carries a vector to its own space.");
            }
            Map<String, Dataset> parts = new LinkedHashMap<>();
            parts.put("analysis", analysis);
            if (arrows) {
                parts.put("vectors", eigenvectorArrows(matrix, analysis.eigenpairs()));
            }
            if (start.length > 0) {
                if (start.length != matrix.length) {
                    throw new IllegalArgumentException("The vector carried by a " + matrix.length + "×" + matrix.length
                            + " matrix has " + matrix.length + " components.");
                }
                EvolutionSide side = settings.choice("side", EvolutionSide.class);
                List<String> names = componentNames(object.definition(), matrix.length);
                parts.put("evolution", evolution(matrix, start, settings.integer("steps"), side, names));
                fixedVector(matrix, side).ifPresent(fixed -> {
                    Map<String, Double> values = new LinkedHashMap<>();
                    for (int component = 0; component < fixed.length; component++) {
                        values.put(names.get(component), fixed[component]);
                    }
                    parts.put("stationary", new Dataset.Measurements(values, Map.of()));
                });
            }
            return new Dataset.Composite(parts);
        }

        /**
         * A real eigenvector v as the arrow λ v; a complex pair a ± bi, which keeps no real direction, as the two
         * arrows spanning the plane it turns, scaled by |λ|.
         */
        private static Dataset.VectorSamples eigenvectorArrows(double[][] matrix, List<Dataset.Eigenpair> pairs) {
            int dimension = matrix.length;
            List<Float> vectors = new ArrayList<>();
            List<Float> magnitudes = new ArrayList<>();
            for (Dataset.Eigenpair pair : pairs) {
                if (pair.vector() != null) {
                    for (int component = 0; component < dimension; component++) {
                        vectors.add((float) (pair.vector()[component] * pair.real()));
                    }
                    magnitudes.add((float) Math.abs(pair.real()));
                } else if (pair.imaginary() > 0) {
                    double modulus = Math.hypot(pair.real(), pair.imaginary());
                    for (double[] spanning : LinearAlgebra.invariantPlane(matrix, pair.real(), pair.imaginary())) {
                        for (int component = 0; component < dimension; component++) {
                            vectors.add((float) (spanning[component] * modulus));
                        }
                        magnitudes.add((float) modulus);
                    }
                }
            }
            return new Dataset.VectorSamples(dimension, new float[vectors.size()], toArray(vectors), toArray(magnitudes), 0);
        }

        /** The vector after every step, one column per component, over the step number. */
        private static Dataset.TimeSeries evolution(double[][] matrix, double[] start, int steps, EvolutionSide side,
                                                    List<String> names) {
            double[][] acting = side == EvolutionSide.LEFT ? LinearAlgebra.transpose(matrix) : matrix;
            float[] axis = new float[steps + 1];
            List<float[]> columns = new ArrayList<>();
            for (int component = 0; component < start.length; component++) {
                columns.add(new float[steps + 1]);
            }
            double[] vector = start.clone();
            for (int step = 0; step <= steps; step++) {
                axis[step] = step;
                for (int component = 0; component < vector.length; component++) {
                    columns.get(component)[step] = (float) vector[component];
                }
                vector = LinearAlgebra.apply(acting, vector);
            }
            return new Dataset.TimeSeries("step", axis, names, columns);
        }

        /**
         * The eigenvector of eigenvalue 1 — of A for x ← A x, of Aᵀ for x ← x A — scaled so its components sum to 1
         * (a distribution, when the matrix is a chain's), or left of unit length when they sum to zero; empty when 1
         * is not an eigenvalue.
         */
        private static Optional<double[]> fixedVector(double[][] matrix, EvolutionSide side) {
            double[][] acting = side == EvolutionSide.LEFT ? LinearAlgebra.transpose(matrix) : matrix;
            return LinearAlgebra.eigenpairs(acting).stream()
                    .filter(pair -> pair.vector() != null && pair.imaginary() == 0 && Math.abs(pair.real() - 1) < 1e-6)
                    .findFirst()
                    .map(pair -> {
                        double[] vector = pair.vector().clone();
                        double sum = java.util.Arrays.stream(vector).sum();
                        if (Math.abs(sum) > 1e-12) {
                            for (int component = 0; component < vector.length; component++) {
                                vector[component] /= sum;
                            }
                        }
                        return vector;
                    });
        }

        /** A matrix's own labels when it names its rows, {@code x1} … {@code xn} otherwise. */
        private static List<String> componentNames(ObjectDefinition definition, int size) {
            if (definition instanceof MatrixObject matrix && matrix.labels().size() == size) {
                return matrix.labels();
            }
            return java.util.stream.IntStream.rangeClosed(1, size).mapToObj(index -> "x" + index).toList();
        }
    }

    /**
     * A = U Σ Vᵀ shown as three successive maps of the unit circle (sphere): Vᵀ rotates, Σ stretches along the
     * axes, U rotates again. Each stage is the image of the previous one.
     */
    public static class SvdDecomposition implements Computation {

        public RepresentationKind kind() {
            return RepresentationKind.SVD_DECOMPOSITION;
        }

        public boolean accepts(ObjectDefinition definition) {
            return isMatrix(definition) && square(definition) && definition.ambientDimension() <= 3;
        }

        public List<SettingSpecification> settings() {
            return List.of(SettingSpecification.integer("samples", 128, 8, 2000, "Points of the unit circle"));
        }

        public long estimatedCost(CompiledObject object, ComputationSettings settings) {
            return settings.integer("samples") * 12L;
        }

        public Dataset compute(CompiledObject object, ComputationSettings settings, ComputationProgress progress) {
            double[][] matrix = matrixOf(object);
            int dimension = matrix.length;
            LinearAlgebra.Svd svd = LinearAlgebra.svd(matrix);
            double[][] sigma = new double[dimension][dimension];
            for (int index = 0; index < dimension; index++) {
                sigma[index][index] = svd.values()[index];
            }
            double[][] rightTransposed = LinearAlgebra.transpose(svd.right());
            List<double[]> circle = unitCircle(dimension, settings.integer("samples"));
            List<double[]> afterV = circle.stream().map(point -> LinearAlgebra.apply(rightTransposed, point)).toList();
            List<double[]> afterSigma = afterV.stream().map(point -> LinearAlgebra.apply(sigma, point)).toList();
            List<double[]> afterU = afterSigma.stream().map(point -> LinearAlgebra.apply(svd.left(), point)).toList();
            Map<String, Dataset> parts = new LinkedHashMap<>();
            parts.put("input", outline(circle, dimension, "x"));
            parts.put("rotateV", outline(afterV, dimension, "Vᵀx"));
            parts.put("stretch", outline(afterSigma, dimension, "ΣVᵀx"));
            parts.put("rotateU", outline(afterU, dimension, "UΣVᵀx = Ax"));
            parts.put("analysis", analyse(matrix));
            return new Dataset.Composite(parts);
        }

        private static List<double[]> unitCircle(int dimension, int samples) {
            List<double[]> points = new ArrayList<>();
            if (dimension == 2) {
                for (int index = 0; index <= samples; index++) {
                    double angle = 2 * Math.PI * index / samples;
                    points.add(new double[]{Math.cos(angle), Math.sin(angle)});
                }
                return points;
            }
            for (int ring = 0; ring < 3; ring++) {
                for (int index = 0; index <= samples; index++) {
                    double angle = 2 * Math.PI * index / samples;
                    double[] point = new double[3];
                    point[ring] = Math.cos(angle);
                    point[(ring + 1) % 3] = Math.sin(angle);
                    points.add(point);
                }
            }
            return points;
        }

        private static Dataset.Polylines outline(List<double[]> points, int dimension, String label) {
            float[] flat = new float[points.size() * dimension];
            for (int index = 0; index < points.size(); index++) {
                for (int axis = 0; axis < dimension; axis++) {
                    flat[index * dimension + axis] = (float) points.get(index)[axis];
                }
            }
            return new Dataset.Polylines(dimension, List.of(new Dataset.Polyline(flat, null, label, true)),
                    new double[dimension], new double[dimension]);
        }
    }

    static boolean square(ObjectDefinition definition) {
        return switch (definition) {
            case MatrixObject matrix -> matrix.entries().size() == matrix.entries().getFirst().size();
            case AffineTransformation transformation -> true;
            default -> false;
        };
    }

    static float[] toArray(List<Float> values) {
        float[] array = new float[values.size()];
        for (int index = 0; index < array.length; index++) {
            array[index] = values.get(index);
        }
        return array;
    }
}
