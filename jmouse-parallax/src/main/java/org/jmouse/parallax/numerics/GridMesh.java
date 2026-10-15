package org.jmouse.parallax.numerics;

import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.object.Interval;

import java.util.Arrays;
import java.util.List;

/**
 * Triangulates a map from a rectangle of parameters (u, v) into space. Cells touching a non-finite sample are left
 * out, so a pole or a domain gap leaves a hole rather than a spike to infinity.
 */
public final class GridMesh {

    @FunctionalInterface
    public interface Map2 {
        /** Writes the point for (u, v) into `target` (dimension coordinates). */
        void at(double u, double v, double[] target);
    }

    @FunctionalInterface
    public interface Scalar2 {
        double at(double u, double v, double[] point);
    }

    private GridMesh() {
    }

    public static Dataset.Mesh build(Map2 map, int dimension, Interval u, Interval v, int columns, int rows,
                                     Scalar2 scalar, String scalarName, List<Dataset.Seam> seams,
                                     ComputationProgress progress) {
        int vertexCount = columns * rows;
        float[] positions = new float[vertexCount * dimension];
        float[] scalars = scalar == null ? null : new float[vertexCount];
        boolean[] finite = new boolean[vertexCount];
        double[] point = new double[dimension];
        double[] minimum = new double[dimension];
        double[] maximum = new double[dimension];
        Arrays.fill(minimum, Double.POSITIVE_INFINITY);
        Arrays.fill(maximum, Double.NEGATIVE_INFINITY);
        for (int row = 0; row < rows; row++) {
            if (row % 16 == 0) {
                progress.checkpoint(0.8 * row / rows, "sampling");
            }
            double parameterV = v.sample(row, rows);
            for (int column = 0; column < columns; column++) {
                double parameterU = u.sample(column, columns);
                map.at(parameterU, parameterV, point);
                int vertex = row * columns + column;
                boolean finitePoint = true;
                for (int axis = 0; axis < dimension; axis++) {
                    if (!Double.isFinite(point[axis])) {
                        finitePoint = false;
                    }
                }
                finite[vertex] = finitePoint;
                for (int axis = 0; axis < dimension; axis++) {
                    positions[vertex * dimension + axis] = finitePoint ? (float) point[axis] : Float.NaN;
                    if (finitePoint) {
                        minimum[axis] = Math.min(minimum[axis], point[axis]);
                        maximum[axis] = Math.max(maximum[axis], point[axis]);
                    }
                }
                if (scalars != null) {
                    scalars[vertex] = finitePoint ? (float) scalar.at(parameterU, parameterV, point) : Float.NaN;
                }
            }
        }
        int[] indices = triangulate(columns, rows, finite);
        float[] normals = dimension == 3 ? vertexNormals(positions, indices, vertexCount) : null;
        return new Dataset.Mesh(dimension, positions, normals, indices, scalars, scalarName, bounded(minimum),
                bounded(maximum), seams == null ? List.of() : seams);
    }

    private static double[] bounded(double[] values) {
        double[] result = values.clone();
        for (int index = 0; index < result.length; index++) {
            if (!Double.isFinite(result[index])) {
                result[index] = 0;
            }
        }
        return result;
    }

    private static int[] triangulate(int columns, int rows, boolean[] finite) {
        int[] indices = new int[Math.max(0, (columns - 1) * (rows - 1) * 6)];
        int count = 0;
        for (int row = 0; row < rows - 1; row++) {
            for (int column = 0; column < columns - 1; column++) {
                int a = row * columns + column;
                int b = a + 1;
                int c = a + columns;
                int d = c + 1;
                if (finite[a] && finite[b] && finite[c]) {
                    indices[count++] = a;
                    indices[count++] = b;
                    indices[count++] = c;
                }
                if (finite[b] && finite[d] && finite[c]) {
                    indices[count++] = b;
                    indices[count++] = d;
                    indices[count++] = c;
                }
            }
        }
        return Arrays.copyOf(indices, count);
    }

    /** Area-weighted vertex normals from the triangles around each vertex. */
    public static float[] vertexNormals(float[] positions, int[] indices, int vertexCount) {
        double[] accumulated = new double[vertexCount * 3];
        for (int index = 0; index < indices.length; index += 3) {
            int a = indices[index];
            int b = indices[index + 1];
            int c = indices[index + 2];
            double abX = positions[b * 3] - positions[a * 3];
            double abY = positions[b * 3 + 1] - positions[a * 3 + 1];
            double abZ = positions[b * 3 + 2] - positions[a * 3 + 2];
            double acX = positions[c * 3] - positions[a * 3];
            double acY = positions[c * 3 + 1] - positions[a * 3 + 1];
            double acZ = positions[c * 3 + 2] - positions[a * 3 + 2];
            double normalX = abY * acZ - abZ * acY;
            double normalY = abZ * acX - abX * acZ;
            double normalZ = abX * acY - abY * acX;
            for (int vertex : new int[]{a, b, c}) {
                accumulated[vertex * 3] += normalX;
                accumulated[vertex * 3 + 1] += normalY;
                accumulated[vertex * 3 + 2] += normalZ;
            }
        }
        float[] normals = new float[vertexCount * 3];
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            double x = accumulated[vertex * 3];
            double y = accumulated[vertex * 3 + 1];
            double z = accumulated[vertex * 3 + 2];
            double length = Math.sqrt(x * x + y * y + z * z);
            if (length > 0) {
                normals[vertex * 3] = (float) (x / length);
                normals[vertex * 3 + 1] = (float) (y / length);
                normals[vertex * 3 + 2] = (float) (z / length);
            }
        }
        return normals;
    }
}
