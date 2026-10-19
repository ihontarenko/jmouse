package org.jmouse.parallax;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jmouse.parallax.object.Interval;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * What the backend answers instead of a picture: geometry and numbers a renderer draws. Float arrays are flat
 * and interleaved (x0 y0 z0 x1 y1 z1 …) so they go straight into a GPU buffer.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "format")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Dataset.Mesh.class, name = "MESH"),
        @JsonSubTypes.Type(value = Dataset.Polylines.class, name = "POLYLINES"),
        @JsonSubTypes.Type(value = Dataset.ScalarGrid.class, name = "SCALAR_GRID"),
        @JsonSubTypes.Type(value = Dataset.BinaryGrid.class, name = "BINARY_GRID"),
        @JsonSubTypes.Type(value = Dataset.VectorSamples.class, name = "VECTOR_SAMPLES"),
        @JsonSubTypes.Type(value = Dataset.PointCloud.class, name = "POINT_CLOUD"),
        @JsonSubTypes.Type(value = Dataset.FeaturePoints.class, name = "FEATURE_POINTS"),
        @JsonSubTypes.Type(value = Dataset.ComplexGrid.class, name = "COMPLEX_GRID"),
        @JsonSubTypes.Type(value = Dataset.IterationGrid.class, name = "ITERATION_GRID"),
        @JsonSubTypes.Type(value = Dataset.TimeSeries.class, name = "TIME_SERIES"),
        @JsonSubTypes.Type(value = Dataset.MatrixAnalysis.class, name = "MATRIX_ANALYSIS"),
        @JsonSubTypes.Type(value = Dataset.Symbolic.class, name = "SYMBOLIC"),
        @JsonSubTypes.Type(value = Dataset.Measurements.class, name = "MEASUREMENTS"),
        @JsonSubTypes.Type(value = Dataset.Composite.class, name = "COMPOSITE")
})
public sealed interface Dataset {

    /** How many numbers the dataset carries; what limits and the cache account in. */
    long elementCount();

    /** What came out, from a short universal list (DECISIONS §14): a page draws a layer by it, not by a representation. */
    enum Shape { LINES, RASTER, POINTS, VECTORS, MESH, VALUES, COMPOSITE }

    /** Where it is drawn: the Plane (axes, zoom, pan), Space (a camera), or no canvas at all — numbers in a panel. */
    enum Canvas { PLANE, SPACE, NONE }

    @JsonProperty(value = "shape", access = JsonProperty.Access.READ_ONLY)
    default Shape shape() {
        return switch (this) {
            case Mesh mesh -> Shape.MESH;
            case Polylines polylines -> Shape.LINES;
            case TimeSeries series -> Shape.LINES;
            case ScalarGrid grid -> Shape.RASTER;
            case BinaryGrid grid -> Shape.RASTER;
            case ComplexGrid grid -> Shape.RASTER;
            case IterationGrid grid -> Shape.RASTER;
            case VectorSamples vectors -> Shape.VECTORS;
            case PointCloud cloud -> Shape.POINTS;
            case FeaturePoints features -> Shape.POINTS;
            case MatrixAnalysis matrix -> Shape.VALUES;
            case Symbolic symbolic -> Shape.VALUES;
            case Measurements measurements -> Shape.VALUES;
            case Composite composite -> Shape.COMPOSITE;
        };
    }

    @JsonProperty(value = "canvas", access = JsonProperty.Access.READ_ONLY)
    default Canvas canvas() {
        return switch (this) {
            case Mesh mesh -> Canvas.SPACE;
            case Polylines polylines -> polylines.dimension() < 3 ? Canvas.PLANE : Canvas.SPACE;
            case VectorSamples vectors -> vectors.dimension() < 3 ? Canvas.PLANE : Canvas.SPACE;
            case PointCloud cloud -> cloud.dimension() < 3 ? Canvas.PLANE : Canvas.SPACE;
            case FeaturePoints features -> features.features().stream()
                    .filter(feature -> feature.position() != null)
                    .allMatch(feature -> feature.position().length < 3) ? Canvas.PLANE : Canvas.SPACE;
            case TimeSeries series -> Canvas.PLANE;
            case ScalarGrid grid -> Canvas.PLANE;
            case BinaryGrid grid -> Canvas.PLANE;
            case ComplexGrid grid -> Canvas.PLANE;
            case IterationGrid grid -> Canvas.PLANE;
            case MatrixAnalysis matrix -> Canvas.NONE;
            case Symbolic symbolic -> Canvas.NONE;
            case Measurements measurements -> Canvas.NONE;
            case Composite composite -> composite.parts().values().stream().map(Dataset::canvas)
                    .reduce(Canvas.NONE, (first, second) -> first == Canvas.SPACE || second == Canvas.SPACE ? Canvas.SPACE
                            : first == Canvas.PLANE || second == Canvas.PLANE ? Canvas.PLANE : Canvas.NONE);
        };
    }

    /**
     * Triangles. `positions` holds `dimension` coordinates per vertex (4 for a surface in R⁴ that is projected on
     * the client); `scalars` (optional) one value per vertex for colouring — height, curvature, phase.
     */
    record Mesh(int dimension, float[] positions, float[] normals, int[] indices, float[] scalars, String scalarName,
                double[] minimum, double[] maximum, List<Seam> seams) implements Dataset {
        public long elementCount() {
            return (long) positions.length + (normals == null ? 0 : normals.length) + indices.length
                    + (scalars == null ? 0 : scalars.length);
        }
    }

    /** An edge of the parameter domain that is glued, and how: the topology lab draws and animates it. */
    record Seam(String name, float[] points, boolean reversed) {
    }

    record Polylines(int dimension, List<Polyline> lines, double[] minimum, double[] maximum) implements Dataset {
        public long elementCount() {
            return lines.stream().mapToLong(line -> line.points().length + (line.scalars() == null ? 0 : line.scalars().length))
                    .sum();
        }
    }

    record Polyline(float[] points, float[] scalars, String label, boolean closed) {
    }

    /** Values on a regular grid: a heat map, a curvature map, a density. Row-major, y outer. */
    record ScalarGrid(int columns, int rows, Interval x, Interval y, float[] values, double minimum, double maximum,
                      String quantity) implements Dataset {
        public long elementCount() {
            return values.length;
        }
    }

    /** A bounded row-major bitmap computed by a user-authored discrete program. */
    record BinaryGrid(int columns, int rows, int[] values) implements Dataset {
        public BinaryGrid {
            if (columns <= 0 || rows <= 0 || (long) columns * rows != values.length) {
                throw new IllegalArgumentException("A binary grid needs one value per pixel.");
            }
        }

        public long elementCount() {
            return values.length;
        }
    }

    /** Arrows: origin and vector per sample, interleaved, with their magnitudes. */
    record VectorSamples(int dimension, float[] origins, float[] vectors, float[] magnitudes, double maximumMagnitude)
            implements Dataset {
        public long elementCount() {
            return (long) origins.length + vectors.length + magnitudes.length;
        }
    }

    record PointCloud(int dimension, float[] points, float[] values, String valueName) implements Dataset {
        public long elementCount() {
            return (long) points.length + (values == null ? 0 : values.length);
        }
    }

    /** Named points with what is known about them: critical points, equilibria, intersections, roots. */
    record FeaturePoints(List<Feature> features) implements Dataset {
        public long elementCount() {
            return features.size() * 8L;
        }
    }

    /** A feature an analysis found; `position` is null when it has no point to stand at (a complex eigenvalue). */
    record Feature(double[] position, Double value, String classification, Map<String, Object> facts) {
    }

    /** f over a rectangle of the complex plane: modulus and phase per sample, for domain colouring. */
    record ComplexGrid(int columns, int rows, Interval real, Interval imaginary, float[] modulus, float[] phase)
            implements Dataset {
        public long elementCount() {
            return (long) modulus.length + phase.length;
        }
    }

    /**
     * An escape-time or basin picture: a smooth iteration count per sample (NaN for points that never escaped),
     * or the index of the root a Newton start converged to (-1 for none) in `basins`.
     */
    record IterationGrid(int columns, int rows, Interval real, Interval imaginary, float[] values, int[] basins,
                         int maximumIterations, List<double[]> roots) implements Dataset {
        public long elementCount() {
            return (long) values.length + (basins == null ? 0 : basins.length);
        }
    }

    /**
     * Columns over a common axis: x(t), y(t); a bifurcation diagram's samples; a vector a matrix carries; a circuit's
     * transient. {@code units} says what each column holds, in the order of {@code columns} — "V", "A", "W", or "" for a
     * plain number: a circuit's columns are named as the circuit names them (a node `out`, a part `R1`, a pin
     * `U1.output`), so the unit is what tells a voltage from a current, never a notation around the name.
     *
     * <p>{@code peaks}, by column, holds what happened between the samples where the source saw it: a sample is one
     * instant, and a transistor's overshoot a nanosecond long between two samples 25 ns apart is in neither. A column
     * it leaves out has none; a series that is not a run through time has none at all.</p>
     */
    record TimeSeries(String axis, float[] axisValues, List<String> columns, List<float[]> values, List<String> units,
                      AxisScale axisScale, Map<String, Band> peaks) implements Dataset {

        /**
         * The lowest and highest value a column reached over each sample's interval — from the sample before to this one
         * —, over every step the engine took between: what a peak-detecting oscilloscope keeps. The first sample's
         * interval is its own instant.
         */
        public record Band(float[] lows, float[] highs) {
        }

        /**
         * How the axis is read: evenly, or in decades — a frequency response. Declared by what computed the series, never
         * guessed from its spacing: a sweep refined around a resonance is no longer evenly spaced in decades, and was
         * drawn on a linear axis with negative frequencies.
         */
        public enum AxisScale {
            LINEAR,
            LOGARITHMIC
        }

        public TimeSeries {
            axisScale = axisScale == null ? AxisScale.LINEAR : axisScale;
            units = units == null || units.isEmpty() ? Collections.nCopies(columns.size(), "") : List.copyOf(units);
            if (units.size() != columns.size()) {
                throw new IllegalArgumentException("A series has a unit for every column: " + columns.size() + " columns, "
                        + units.size() + " units.");
            }
            peaks = peaks == null ? Map.of() : Map.copyOf(peaks);
            if (!columns.containsAll(peaks.keySet())) {
                throw new IllegalArgumentException("A series keeps peaks only for its own columns.");
            }
        }

        /** Columns with their units, on an axis read as {@code axisScale} says, without peaks. */
        public TimeSeries(String axis, float[] axisValues, List<String> columns, List<float[]> values, List<String> units,
                          AxisScale axisScale) {
            this(axis, axisValues, columns, values, units, axisScale, Map.of());
        }

        /** Columns of plain numbers. */
        public TimeSeries(String axis, float[] axisValues, List<String> columns, List<float[]> values) {
            this(axis, axisValues, columns, values, List.of(), AxisScale.LINEAR);
        }

        /** Columns with their units, on an even axis. */
        public TimeSeries(String axis, float[] axisValues, List<String> columns, List<float[]> values, List<String> units) {
            this(axis, axisValues, columns, values, units, AxisScale.LINEAR);
        }

        public long elementCount() {
            return axisValues.length + values.stream().mapToLong(column -> column.length).sum()
                    + peaks.values().stream().mapToLong(band -> band.lows().length + band.highs().length).sum();
        }
    }

    record MatrixAnalysis(int rows, int columns, double[][] matrix, Double determinant, int rank, Double trace,
                          List<Eigenpair> eigenpairs, double[][] singularLeft, double[] singularValues,
                          double[][] singularRight, List<double[]> nullSpace, List<double[]> columnSpace)
            implements Dataset {
        public long elementCount() {
            return (long) rows * columns * 4;
        }
    }

    /** An eigenvalue (possibly complex) and, for a real one, its eigenvector. */
    record Eigenpair(double real, double imaginary, double[] vector) {
    }

    /** An expression the server derived: a derivative, a Jacobian entry, a gradient component. */
    record Symbolic(Map<String, String> text, Map<String, String> latex) implements Dataset {
        public long elementCount() {
            return text.size();
        }
    }

    /**
     * Named scalar results: an integral's value, a program's estimate, a stationary distribution, a circuit's operating
     * point. {@code units} gives a value's unit by its name ("V", "A", "W"); a name it leaves out is a plain number.
     */
    record Measurements(Map<String, Double> values, Map<String, String> notes, Map<String, String> units) implements Dataset {

        public Measurements {
            units = units == null ? Map.of() : Map.copyOf(units);
        }

        /** Plain numbers. */
        public Measurements(Map<String, Double> values, Map<String, String> notes) {
            this(values, notes, Map.of());
        }

        public long elementCount() {
            return values.size();
        }
    }

    /** Several datasets that are only meaningful together: a phase portrait, an SVD's three steps. */
    record Composite(Map<String, Dataset> parts) implements Dataset {
        public long elementCount() {
            return parts.values().stream().mapToLong(Dataset::elementCount).sum();
        }
    }
}
