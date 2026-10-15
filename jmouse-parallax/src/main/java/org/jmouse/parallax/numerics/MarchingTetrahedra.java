package org.jmouse.parallax.numerics;

import org.jmouse.parallax.ComputationProgress;
import org.jmouse.parallax.Dataset;
import org.jmouse.parallax.object.Interval;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The surface f(x, y, z) = 0, triangulated over a sampling box. Each cube is split into six tetrahedra, which has
 * no ambiguous cases (unlike marching cubes) and needs no lookup table. Vertices on a shared edge are shared.
 */
public final class MarchingTetrahedra {

    @FunctionalInterface
    public interface Field3 {
        double at(double x, double y, double z);
    }

    /** The six tetrahedra of a cube, by the cube's corner indices (bit 0 = x, bit 1 = y, bit 2 = z). */
    private static final int[][] TETRAHEDRA = {
            {0, 5, 1, 6}, {0, 1, 2, 6}, {0, 2, 3, 6}, {0, 3, 7, 6}, {0, 7, 4, 6}, {0, 4, 5, 6}
    };
    private static final int[][] CORNERS = {
            {0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}
    };

    private MarchingTetrahedra() {
    }

    public static Dataset.Mesh build(Field3 field, Interval x, Interval y, Interval z, int resolution,
                                     ComputationProgress progress) {
        int samples = resolution + 1;
        float[] values = new float[samples * samples * samples];
        for (int k = 0; k < samples; k++) {
            progress.checkpoint(0.5 * k / samples, "sampling");
            double zValue = z.sample(k, samples);
            for (int j = 0; j < samples; j++) {
                double yValue = y.sample(j, samples);
                for (int i = 0; i < samples; i++) {
                    values[index(i, j, k, samples)] = (float) field.at(x.sample(i, samples), yValue, zValue);
                }
            }
        }
        Builder builder = new Builder(x, y, z, samples, values);
        int[] corners = new int[8];
        for (int k = 0; k < resolution; k++) {
            progress.checkpoint(0.5 + 0.5 * k / resolution, "triangulating");
            for (int j = 0; j < resolution; j++) {
                for (int i = 0; i < resolution; i++) {
                    for (int corner = 0; corner < 8; corner++) {
                        corners[corner] = index(i + CORNERS[corner][0], j + CORNERS[corner][1], k + CORNERS[corner][2], samples);
                    }
                    for (int[] tetrahedron : TETRAHEDRA) {
                        builder.tetrahedron(corners[tetrahedron[0]], corners[tetrahedron[1]], corners[tetrahedron[2]],
                                corners[tetrahedron[3]]);
                    }
                }
            }
        }
        return builder.mesh();
    }

    private static int index(int i, int j, int k, int samples) {
        return (k * samples + j) * samples + i;
    }

    private static final class Builder {
        private final Interval x;
        private final Interval y;
        private final Interval z;
        private final int samples;
        private final float[] values;
        private final Map<Long, Integer> vertexByEdge = new HashMap<>();
        private float[] positions = new float[3 * 4096];
        private int vertexCount;
        private int[] indices = new int[3 * 8192];
        private int indexCount;

        Builder(Interval x, Interval y, Interval z, int samples, float[] values) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.samples = samples;
            this.values = values;
        }

        void tetrahedron(int a, int b, int c, int d) {
            int[] vertices = {a, b, c, d};
            int inside = 0;
            for (int vertex : vertices) {
                if (!Float.isFinite(values[vertex])) {
                    return;
                }
                if (values[vertex] < 0) {
                    inside++;
                }
            }
            if (inside == 0 || inside == 4) {
                return;
            }
            int[] negative = new int[4];
            int[] positive = new int[4];
            int negativeCount = 0;
            int positiveCount = 0;
            for (int vertex : vertices) {
                if (values[vertex] < 0) {
                    negative[negativeCount++] = vertex;
                } else {
                    positive[positiveCount++] = vertex;
                }
            }
            if (negativeCount == 1) {
                triangle(crossing(negative[0], positive[0]), crossing(negative[0], positive[1]), crossing(negative[0], positive[2]));
            } else if (negativeCount == 3) {
                triangle(crossing(positive[0], negative[0]), crossing(positive[0], negative[2]), crossing(positive[0], negative[1]));
            } else {
                int first = crossing(negative[0], positive[0]);
                int second = crossing(negative[0], positive[1]);
                int third = crossing(negative[1], positive[1]);
                int fourth = crossing(negative[1], positive[0]);
                triangle(first, second, third);
                triangle(first, third, fourth);
            }
        }

        private int crossing(int from, int to) {
            long key = from < to ? ((long) from << 32) | to : ((long) to << 32) | from;
            Integer existing = vertexByEdge.get(key);
            if (existing != null) {
                return existing;
            }
            double fromValue = values[from];
            double toValue = values[to];
            double fraction = fromValue / (fromValue - toValue);
            double[] fromPoint = point(from);
            double[] toPoint = point(to);
            if (positions.length < (vertexCount + 1) * 3) {
                positions = Arrays.copyOf(positions, positions.length * 2);
            }
            for (int axis = 0; axis < 3; axis++) {
                positions[vertexCount * 3 + axis] = (float) (fromPoint[axis] + fraction * (toPoint[axis] - fromPoint[axis]));
            }
            vertexByEdge.put(key, vertexCount);
            return vertexCount++;
        }

        private double[] point(int index) {
            int i = index % samples;
            int j = (index / samples) % samples;
            int k = index / (samples * samples);
            return new double[]{x.sample(i, samples), y.sample(j, samples), z.sample(k, samples)};
        }

        private void triangle(int a, int b, int c) {
            if (indices.length < indexCount + 3) {
                indices = Arrays.copyOf(indices, indices.length * 2);
            }
            indices[indexCount++] = a;
            indices[indexCount++] = b;
            indices[indexCount++] = c;
        }

        Dataset.Mesh mesh() {
            float[] finalPositions = Arrays.copyOf(positions, vertexCount * 3);
            int[] finalIndices = Arrays.copyOf(indices, indexCount);
            float[] normals = GridMesh.vertexNormals(finalPositions, finalIndices, vertexCount);
            return new Dataset.Mesh(3, finalPositions, normals, finalIndices, null, null,
                    new double[]{x.minimum(), y.minimum(), z.minimum()}, new double[]{x.maximum(), y.maximum(), z.maximum()},
                    List.of());
        }
    }
}
