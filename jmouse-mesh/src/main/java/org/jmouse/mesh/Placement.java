package org.jmouse.mesh;

import java.util.Arrays;

/**
 * Where a part of a body is put in the file's own frame: an affine placement as a 3 × 4 matrix, row by row — what a
 * format's scene graph composes from its nodes (VRML's {@code Transform}, glTF's node) down to a body's corners.
 */
public record Placement(double[] matrix) {

    public static final Placement IDENTITY = new Placement(new double[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0});

    public Placement {
        if (matrix.length != 12) {
            throw new IllegalArgumentException("A placement is three rows of four numbers");
        }
        matrix = matrix.clone();
    }

    public static Placement translation(double x, double y, double z) {
        return new Placement(new double[] {1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z});
    }

    public static Placement scale(double x, double y, double z) {
        return new Placement(new double[] {x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0});
    }

    /** About an axis by an angle: {@code x y z radians}, Rodrigues' formula. */
    public static Placement rotation(double[] axisAngle) {
        double length = Math.sqrt(axisAngle[0] * axisAngle[0] + axisAngle[1] * axisAngle[1] + axisAngle[2] * axisAngle[2]);
        if (length == 0 || axisAngle[3] == 0) {
            return IDENTITY;
        }
        double x = axisAngle[0] / length;
        double y = axisAngle[1] / length;
        double z = axisAngle[2] / length;
        double cosine = Math.cos(axisAngle[3]);
        double sine = Math.sin(axisAngle[3]);
        double turn = 1 - cosine;
        return new Placement(new double[] {
                turn * x * x + cosine, turn * x * y - sine * z, turn * x * z + sine * y, 0,
                turn * x * y + sine * z, turn * y * y + cosine, turn * y * z - sine * x, 0,
                turn * x * z - sine * y, turn * y * z + sine * x, turn * z * z + cosine, 0});
    }

    /** A turn as a unit quaternion {@code x y z w}; one not of unit length is made so. */
    public static Placement quaternion(double[] quaternion) {
        double length = Math.sqrt(quaternion[0] * quaternion[0] + quaternion[1] * quaternion[1]
                + quaternion[2] * quaternion[2] + quaternion[3] * quaternion[3]);
        if (length == 0) {
            return IDENTITY;
        }
        double x = quaternion[0] / length;
        double y = quaternion[1] / length;
        double z = quaternion[2] / length;
        double w = quaternion[3] / length;
        return new Placement(new double[] {
                1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w), 0,
                2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w), 0,
                2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y), 0});
    }

    /** A 4 × 4 matrix written column by column, as glTF and OpenGL write one; its last row is taken as {@code 0 0 0 1}. */
    public static Placement columnMajor(double[] columns) {
        if (columns.length != 16) {
            throw new IllegalArgumentException("A 4 × 4 matrix is sixteen numbers");
        }
        return new Placement(new double[] {
                columns[0], columns[4], columns[8], columns[12],
                columns[1], columns[5], columns[9], columns[13],
                columns[2], columns[6], columns[10], columns[14]});
    }

    /** The placement as a 4 × 4 matrix written column by column, as glTF and OpenGL write one. */
    public double[] columns() {
        double[] m = matrix;
        return new double[] {m[0], m[4], m[8], 0, m[1], m[5], m[9], 0, m[2], m[6], m[10], 0, m[3], m[7], m[11], 1};
    }

    public boolean identity() {
        return Arrays.equals(matrix, IDENTITY.matrix);
    }

    /** This placement after {@code inner}: a point is placed by {@code inner} first. */
    public Placement then(Placement inner) {
        double[] outer = matrix;
        double[] first = inner.matrix;
        double[] product = new double[12];
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 4; column++) {
                double sum = column == 3 ? outer[row * 4 + 3] : 0;
                for (int index = 0; index < 3; index++) {
                    sum += outer[row * 4 + index] * first[index * 4 + column];
                }
                product[row * 4 + column] = sum;
            }
        }
        return new Placement(product);
    }

    public double[] apply(double x, double y, double z) {
        double[] m = matrix;
        return new double[] {m[0] * x + m[1] * y + m[2] * z + m[3], m[4] * x + m[5] * y + m[6] * z + m[7],
                             m[8] * x + m[9] * y + m[10] * z + m[11]};
    }

    /** Whether it turns a body inside out — a negative scale —, which reverses which side a face is seen from. */
    public boolean mirrors() {
        double[] m = matrix;
        double determinant = m[0] * (m[5] * m[10] - m[6] * m[9]) - m[1] * (m[4] * m[10] - m[6] * m[8])
                + m[2] * (m[4] * m[9] - m[5] * m[8]);
        return determinant < 0;
    }
}
