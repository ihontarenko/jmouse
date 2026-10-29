package org.jmouse.numerics;

import java.util.Optional;

/**
 * Roots of F: Rⁿ → Rⁿ by Newton's method with a finite-difference Jacobian and step halving: the heat a part settles
 * at, the point where a system of equations holds.
 */
public final class NewtonSolver {

    @FunctionalInterface
    public interface VectorFunction {
        void evaluate(double[] point, double[] value);
    }

    private NewtonSolver() {
    }

    public static Optional<double[]> solve(VectorFunction function, double[] start, int maximumIterations,
                                           double tolerance) {
        int size = start.length;
        double[] point = start.clone();
        double[] value = new double[size];
        double[] shifted = new double[size];
        double[][] jacobian = new double[size][size];
        for (int iteration = 0; iteration < maximumIterations; iteration++) {
            function.evaluate(point, value);
            double residual = norm(value);
            if (!Double.isFinite(residual)) {
                return Optional.empty();
            }
            if (residual < tolerance) {
                return Optional.of(point);
            }
            for (int column = 0; column < size; column++) {
                double step = 1e-7 * Math.max(1, Math.abs(point[column]));
                double saved = point[column];
                point[column] = saved + step;
                function.evaluate(point, shifted);
                point[column] = saved;
                for (int row = 0; row < size; row++) {
                    jacobian[row][column] = (shifted[row] - value[row]) / step;
                }
            }
            double[] negative = new double[size];
            for (int row = 0; row < size; row++) {
                negative[row] = -value[row];
            }
            double[] delta = step(jacobian, negative);
            if (delta == null) {
                return Optional.empty();
            }
            double damping = 1;
            double[] candidate = new double[size];
            for (int attempt = 0; attempt < 20; attempt++) {
                for (int index = 0; index < size; index++) {
                    candidate[index] = point[index] + damping * delta[index];
                }
                function.evaluate(candidate, shifted);
                if (norm(shifted) < residual) {
                    break;
                }
                damping /= 2;
            }
            System.arraycopy(candidate, 0, point, 0, size);
        }
        function.evaluate(point, value);
        return norm(value) < Math.sqrt(tolerance) ? Optional.of(point) : Optional.empty();
    }

    /** The Newton step: J · delta = −F, solved by the sparse system — null where the Jacobian is singular. */
    private static double[] step(double[][] jacobian, double[] negative) {
        SparseSystem system = new SparseSystem(negative.length);
        for (int row = 0; row < negative.length; row++) {
            for (int column = 0; column < negative.length; column++) {
                if (jacobian[row][column] != 0) {
                    system.add(row, column, jacobian[row][column]);
                }
            }
            system.addRightSide(row, negative[row]);
        }
        return system.solve();
    }

    public static double norm(double[] vector) {
        double sum = 0;
        for (double value : vector) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }
}
