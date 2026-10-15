package org.jmouse.parallax.numerics;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Roots of F: Rⁿ → Rⁿ by Newton's method with a finite-difference Jacobian and step halving; and a multi-start
 * search that seeds a grid and keeps the distinct roots found — critical points of a surface (∇f = 0), equilibria
 * of a system (F = 0).
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
            double[] delta = LinearAlgebra.solve(jacobian, negative);
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

    /** Distinct roots inside the box, seeded from a regular grid of `seedsPerAxis` points per axis. */
    public static List<double[]> rootsInBox(VectorFunction function, double[] minimum, double[] maximum,
                                            int seedsPerAxis, double tolerance) {
        int size = minimum.length;
        List<double[]> roots = new ArrayList<>();
        int total = (int) Math.pow(seedsPerAxis, size);
        double[] seed = new double[size];
        double separation = 0;
        for (int axis = 0; axis < size; axis++) {
            separation = Math.max(separation, (maximum[axis] - minimum[axis]) * 1e-4);
        }
        for (int index = 0; index < total; index++) {
            int remainder = index;
            for (int axis = 0; axis < size; axis++) {
                int cell = remainder % seedsPerAxis;
                remainder /= seedsPerAxis;
                seed[axis] = minimum[axis] + (maximum[axis] - minimum[axis]) * (cell + 0.5) / seedsPerAxis;
            }
            Optional<double[]> root = solve(function, seed, 60, tolerance);
            if (root.isEmpty() || !inside(root.get(), minimum, maximum)) {
                continue;
            }
            boolean known = false;
            for (double[] existing : roots) {
                if (distance(existing, root.get()) < separation) {
                    known = true;
                    break;
                }
            }
            if (!known) {
                roots.add(root.get());
            }
        }
        return roots;
    }

    private static boolean inside(double[] point, double[] minimum, double[] maximum) {
        for (int axis = 0; axis < point.length; axis++) {
            if (point[axis] < minimum[axis] || point[axis] > maximum[axis]) {
                return false;
            }
        }
        return true;
    }

    private static double distance(double[] first, double[] second) {
        double sum = 0;
        for (int index = 0; index < first.length; index++) {
            sum += (first[index] - second[index]) * (first[index] - second[index]);
        }
        return Math.sqrt(sum);
    }

    public static double norm(double[] vector) {
        double sum = 0;
        for (double value : vector) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }
}
