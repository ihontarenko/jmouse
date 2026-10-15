package org.jmouse.parallax.numerics;

import org.jmouse.parallax.Dataset;
import org.jmouse.formula.Complex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Dense linear algebra for the small matrices the linear-algebra visualizer works with (up to 8×8): determinant,
 * rank, eigenvalues (complex ones included), eigenvectors, the singular value decomposition, null and column space.
 */
public final class LinearAlgebra {

    public static final double TOLERANCE = 1e-10;

    private LinearAlgebra() {
    }

    public record Svd(double[][] left, double[] values, double[][] right) {
    }

    public static double determinant(double[][] matrix) {
        int size = matrix.length;
        double[][] working = copy(matrix);
        double determinant = 1;
        for (int column = 0; column < size; column++) {
            int pivot = column;
            for (int row = column + 1; row < size; row++) {
                if (Math.abs(working[row][column]) > Math.abs(working[pivot][column])) {
                    pivot = row;
                }
            }
            if (Math.abs(working[pivot][column]) < 1e-300) {
                return 0;
            }
            if (pivot != column) {
                double[] swap = working[pivot];
                working[pivot] = working[column];
                working[column] = swap;
                determinant = -determinant;
            }
            determinant *= working[column][column];
            for (int row = column + 1; row < size; row++) {
                double factor = working[row][column] / working[column][column];
                for (int inner = column; inner < size; inner++) {
                    working[row][inner] -= factor * working[column][inner];
                }
            }
        }
        return determinant;
    }

    public static int rank(double[][] matrix) {
        double[] singular = svd(matrix).values();
        double largest = singular.length == 0 ? 0 : singular[0];
        int rank = 0;
        for (double value : singular) {
            if (value > Math.max(TOLERANCE, largest * 1e-12)) {
                rank++;
            }
        }
        return rank;
    }

    public static double trace(double[][] matrix) {
        double trace = 0;
        for (int index = 0; index < matrix.length; index++) {
            trace += matrix[index][index];
        }
        return trace;
    }

    /** One-sided Jacobi: A = U Σ Vᵀ with singular values in descending order. */
    public static Svd svd(double[][] matrix) {
        int rows = matrix.length;
        int columns = matrix[0].length;
        double[][] working = copy(matrix);
        double[][] right = identity(columns);
        for (int sweep = 0; sweep < 60; sweep++) {
            double off = 0;
            for (int first = 0; first < columns - 1; first++) {
                for (int second = first + 1; second < columns; second++) {
                    double alpha = 0;
                    double beta = 0;
                    double gamma = 0;
                    for (int row = 0; row < rows; row++) {
                        alpha += working[row][first] * working[row][first];
                        beta += working[row][second] * working[row][second];
                        gamma += working[row][first] * working[row][second];
                    }
                    off = Math.max(off, Math.abs(gamma) / Math.sqrt(Math.max(alpha * beta, 1e-300)));
                    if (Math.abs(gamma) < 1e-300) {
                        continue;
                    }
                    double zeta = (beta - alpha) / (2 * gamma);
                    double tangent = Math.signum(zeta == 0 ? 1 : zeta) / (Math.abs(zeta) + Math.sqrt(1 + zeta * zeta));
                    double cosine = 1 / Math.sqrt(1 + tangent * tangent);
                    double sine = cosine * tangent;
                    for (int row = 0; row < rows; row++) {
                        double a = working[row][first];
                        double b = working[row][second];
                        working[row][first] = cosine * a - sine * b;
                        working[row][second] = sine * a + cosine * b;
                    }
                    for (int row = 0; row < columns; row++) {
                        double a = right[row][first];
                        double b = right[row][second];
                        right[row][first] = cosine * a - sine * b;
                        right[row][second] = sine * a + cosine * b;
                    }
                }
            }
            if (off < 1e-15) {
                break;
            }
        }
        double[] values = new double[columns];
        double[][] left = new double[rows][columns];
        for (int column = 0; column < columns; column++) {
            double norm = 0;
            for (int row = 0; row < rows; row++) {
                norm += working[row][column] * working[row][column];
            }
            norm = Math.sqrt(norm);
            values[column] = norm;
            for (int row = 0; row < rows; row++) {
                left[row][column] = norm > 1e-300 ? working[row][column] / norm : 0;
            }
        }
        Integer[] order = new Integer[columns];
        for (int index = 0; index < columns; index++) {
            order[index] = index;
        }
        Arrays.sort(order, (first, second) -> Double.compare(values[second], values[first]));
        double[] sortedValues = new double[columns];
        double[][] sortedLeft = new double[rows][columns];
        double[][] sortedRight = new double[columns][columns];
        for (int position = 0; position < columns; position++) {
            int source = order[position];
            sortedValues[position] = values[source];
            for (int row = 0; row < rows; row++) {
                sortedLeft[row][position] = left[row][source];
            }
            for (int row = 0; row < columns; row++) {
                sortedRight[row][position] = right[row][source];
            }
        }
        return new Svd(sortedLeft, sortedValues, sortedRight);
    }

    /**
     * Eigenvalues as the roots of the characteristic polynomial (Faddeev–LeVerrier, then Durand–Kerner), which is
     * accurate enough at n ≤ 8 and finds complex pairs; a real eigenvalue gets its eigenvector from the null space
     * of A − λI.
     */
    public static List<Dataset.Eigenpair> eigenpairs(double[][] matrix) {
        int size = matrix.length;
        double[] coefficients = characteristicPolynomial(matrix);
        List<Complex> roots = polynomialRoots(coefficients);
        List<Dataset.Eigenpair> pairs = new ArrayList<>();
        for (Complex root : roots) {
            double real = Math.abs(root.real()) < 1e-12 ? 0 : root.real();
            double imaginary = Math.abs(root.imaginary()) < 1e-9 ? 0 : root.imaginary();
            double[] vector = null;
            if (imaginary == 0) {
                double[][] shifted = copy(matrix);
                for (int index = 0; index < size; index++) {
                    shifted[index][index] -= real;
                }
                Svd decomposition = svd(shifted);
                vector = new double[size];
                for (int row = 0; row < size; row++) {
                    vector[row] = decomposition.right()[row][size - 1];
                }
                normalizeSign(vector);
            }
            pairs.add(new Dataset.Eigenpair(real, imaginary, vector));
        }
        pairs.sort((first, second) -> Double.compare(Math.hypot(second.real(), second.imaginary()),
                Math.hypot(first.real(), first.imaginary())));
        return pairs;
    }

    /**
     * The real plane a complex eigenvalue a ± bi turns: the real and imaginary parts u, w of an eigenvector u + iw,
     * since A(u + iw) = (a + bi)(u + iw) is the real system [[A − aI, bI], [−bI, A − aI]]·[u; w] = 0. Of the
     * eigenvector's phases the one is taken that makes u and w orthogonal with u the longer, scaled to |u| = 1, so
     * the pair draws the axes of the ellipse the map turns along.
     */
    public static double[][] invariantPlane(double[][] matrix, double real, double imaginary) {
        int size = matrix.length;
        double[][] system = new double[2 * size][2 * size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                double shifted = matrix[row][column] - (row == column ? real : 0);
                system[row][column] = shifted;
                system[size + row][size + column] = shifted;
            }
            system[row][size + row] = imaginary;
            system[size + row][row] = -imaginary;
        }
        Svd decomposition = svd(system);
        double[] u = new double[size];
        double[] w = new double[size];
        for (int row = 0; row < size; row++) {
            u[row] = decomposition.right()[row][2 * size - 1];
            w[row] = decomposition.right()[size + row][2 * size - 1];
        }
        double crossing = 0;
        double spread = 0;
        for (int row = 0; row < size; row++) {
            crossing += u[row] * w[row];
            spread += u[row] * u[row] - w[row] * w[row];
        }
        double phase = 0.5 * Math.atan2(-2 * crossing, spread);
        double cosine = Math.cos(phase);
        double sine = Math.sin(phase);
        double[] major = new double[size];
        double[] minor = new double[size];
        double length = 0;
        for (int row = 0; row < size; row++) {
            major[row] = u[row] * cosine - w[row] * sine;
            minor[row] = u[row] * sine + w[row] * cosine;
            length += major[row] * major[row];
        }
        length = Math.sqrt(length);
        for (int row = 0; row < size; row++) {
            major[row] /= length;
            minor[row] /= length;
        }
        return new double[][]{major, minor};
    }

    /** Coefficients of det(λI − A), highest power first: [1, c1, …, cn]. */
    static double[] characteristicPolynomial(double[][] matrix) {
        int size = matrix.length;
        double[] coefficients = new double[size + 1];
        coefficients[0] = 1;
        double[][] accumulated = new double[size][size];
        for (int step = 1; step <= size; step++) {
            double[][] product = multiply(matrix, accumulated);
            for (int index = 0; index < size; index++) {
                product[index][index] += coefficients[step - 1];
            }
            accumulated = product;
            coefficients[step] = -trace(multiply(matrix, accumulated)) / step;
        }
        return coefficients;
    }

    /** All complex roots of a monic polynomial by simultaneous Durand–Kerner iteration. */
    public static List<Complex> polynomialRoots(double[] coefficients) {
        int degree = coefficients.length - 1;
        List<Complex> roots = new ArrayList<>();
        if (degree < 1) {
            return roots;
        }
        Complex[] current = new Complex[degree];
        Complex seed = new Complex(0.4, 0.9);
        double radius = 1;
        for (int index = 1; index <= degree; index++) {
            radius = Math.max(radius, Math.abs(coefficients[index] / coefficients[0]) + 1);
        }
        for (int index = 0; index < degree; index++) {
            current[index] = seed.power(Complex.of(index)).multiply(Complex.of(radius / 2));
        }
        for (int iteration = 0; iteration < 2000; iteration++) {
            double change = 0;
            for (int index = 0; index < degree; index++) {
                Complex numerator = evaluate(coefficients, current[index]);
                Complex denominator = Complex.of(coefficients[0]);
                for (int other = 0; other < degree; other++) {
                    if (other != index) {
                        denominator = denominator.multiply(current[index].subtract(current[other]));
                    }
                }
                Complex next = current[index].subtract(numerator.divide(denominator));
                if (next.isFinite()) {
                    change = Math.max(change, next.subtract(current[index]).modulus());
                    current[index] = next;
                }
            }
            if (change < 1e-14) {
                break;
            }
        }
        Collections.addAll(roots, current);
        return roots;
    }

    public static Complex evaluate(double[] coefficients, Complex point) {
        Complex result = Complex.ZERO;
        for (double coefficient : coefficients) {
            result = result.multiply(point).add(Complex.of(coefficient));
        }
        return result;
    }

    public static List<double[]> nullSpace(double[][] matrix) {
        Svd decomposition = svd(matrix);
        int columns = matrix[0].length;
        double largest = decomposition.values().length == 0 ? 0 : decomposition.values()[0];
        List<double[]> basis = new ArrayList<>();
        for (int column = 0; column < columns; column++) {
            if (decomposition.values()[column] <= Math.max(TOLERANCE, largest * 1e-12)) {
                double[] vector = new double[columns];
                for (int row = 0; row < columns; row++) {
                    vector[row] = decomposition.right()[row][column];
                }
                normalizeSign(vector);
                basis.add(vector);
            }
        }
        return basis;
    }

    public static List<double[]> columnSpace(double[][] matrix) {
        Svd decomposition = svd(matrix);
        int rows = matrix.length;
        double largest = decomposition.values().length == 0 ? 0 : decomposition.values()[0];
        List<double[]> basis = new ArrayList<>();
        for (int column = 0; column < decomposition.values().length; column++) {
            if (decomposition.values()[column] > Math.max(TOLERANCE, largest * 1e-12)) {
                double[] vector = new double[rows];
                for (int row = 0; row < rows; row++) {
                    vector[row] = decomposition.left()[row][column];
                }
                basis.add(vector);
            }
        }
        return basis;
    }

    public static double[][] multiply(double[][] left, double[][] right) {
        int rows = left.length;
        int inner = right.length;
        int columns = right[0].length;
        double[][] product = new double[rows][columns];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                double sum = 0;
                for (int index = 0; index < inner; index++) {
                    sum += left[row][index] * right[index][column];
                }
                product[row][column] = sum;
            }
        }
        return product;
    }

    public static double[] apply(double[][] matrix, double[] vector) {
        double[] result = new double[matrix.length];
        for (int row = 0; row < matrix.length; row++) {
            double sum = 0;
            for (int column = 0; column < vector.length; column++) {
                sum += matrix[row][column] * vector[column];
            }
            result[row] = sum;
        }
        return result;
    }

    /** Solves A x = b by Gaussian elimination with partial pivoting; null when A is singular. */
    public static double[] solve(double[][] matrix, double[] rightSide) {
        int size = matrix.length;
        double[][] working = new double[size][size + 1];
        for (int row = 0; row < size; row++) {
            System.arraycopy(matrix[row], 0, working[row], 0, size);
            working[row][size] = rightSide[row];
        }
        for (int column = 0; column < size; column++) {
            int pivot = column;
            for (int row = column + 1; row < size; row++) {
                if (Math.abs(working[row][column]) > Math.abs(working[pivot][column])) {
                    pivot = row;
                }
            }
            if (Math.abs(working[pivot][column]) < 1e-14) {
                return null;
            }
            double[] swap = working[pivot];
            working[pivot] = working[column];
            working[column] = swap;
            for (int row = 0; row < size; row++) {
                if (row != column) {
                    double factor = working[row][column] / working[column][column];
                    for (int inner = column; inner <= size; inner++) {
                        working[row][inner] -= factor * working[column][inner];
                    }
                }
            }
        }
        double[] solution = new double[size];
        for (int row = 0; row < size; row++) {
            solution[row] = working[row][size] / working[row][row];
        }
        return solution;
    }

    public static double[][] transpose(double[][] matrix) {
        double[][] transposed = new double[matrix[0].length][matrix.length];
        for (int row = 0; row < matrix.length; row++) {
            for (int column = 0; column < matrix[0].length; column++) {
                transposed[column][row] = matrix[row][column];
            }
        }
        return transposed;
    }

    public static double[][] identity(int size) {
        double[][] identity = new double[size][size];
        for (int index = 0; index < size; index++) {
            identity[index][index] = 1;
        }
        return identity;
    }

    public static double[][] copy(double[][] matrix) {
        double[][] copy = new double[matrix.length][];
        for (int row = 0; row < matrix.length; row++) {
            copy[row] = matrix[row].clone();
        }
        return copy;
    }

    /** Makes the largest component positive, so the same eigenvector is not reported pointing two ways. */
    private static void normalizeSign(double[] vector) {
        int largest = 0;
        double norm = 0;
        for (int index = 0; index < vector.length; index++) {
            norm += vector[index] * vector[index];
            if (Math.abs(vector[index]) > Math.abs(vector[largest])) {
                largest = index;
            }
        }
        norm = Math.sqrt(norm);
        double sign = vector[largest] < 0 ? -1 : 1;
        for (int index = 0; index < vector.length; index++) {
            vector[index] = norm > 0 ? sign * vector[index] / norm : 0;
        }
    }
}
