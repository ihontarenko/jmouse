package org.jmouse.parallax.circuit;

import org.jmouse.formula.Complex;

import java.util.Arrays;

/**
 * The small-signal system at one angular frequency, linearized at the operating point: every device stamps its
 * admittance there, and every source is excited as a phasor of its value with phase 0.
 */
final class PhasorAssembly {

    private static final String UNSOLVABLE =
            "The circuit has no unique solution: a node that nothing ties to node 0, or a loop of voltage sources.";

    private final Complex[][] matrix;
    private final Complex[]   injected;
    private final double      angular;
    private final double[]    operatingPoint;

    PhasorAssembly(int size, double angular, double[] operatingPoint) {
        this.matrix = new Complex[size][size];
        this.injected = new Complex[size];
        this.angular = angular;
        this.operatingPoint = operatingPoint;
        for (int row = 0; row < size; row++) {
            injected[row] = Complex.ZERO;
            for (int column = 0; column < size; column++) {
                matrix[row][column] = Complex.ZERO;
            }
        }
    }

    double angular() {
        return angular;
    }

    /** Any unknown's value at the operating point. */
    double operatingValue(int index) {
        return operatingPoint[index];
    }

    /** Adds to one entry of the matrix; a row or column of node 0 is dropped. */
    void add(int row, int column, Complex value) {
        if (row != Netlist.GROUND && column != Netlist.GROUND) {
            matrix[row][column] = matrix[row][column].add(value);
        }
    }

    /** A node's DC voltage, where a nonlinear device is linearized. */
    double operatingVoltage(int node) {
        return node == Netlist.GROUND ? 0 : operatingPoint[node];
    }

    void admittance(int positive, int negative, Complex admittance) {
        if (positive != Netlist.GROUND) {
            matrix[positive][positive] = matrix[positive][positive].add(admittance);
        }
        if (negative != Netlist.GROUND) {
            matrix[negative][negative] = matrix[negative][negative].add(admittance);
        }
        if (positive != Netlist.GROUND && negative != Netlist.GROUND) {
            matrix[positive][negative] = matrix[positive][negative].subtract(admittance);
            matrix[negative][positive] = matrix[negative][positive].subtract(admittance);
        }
    }

    void branch(int positive, int negative, int branch) {
        if (positive != Netlist.GROUND) {
            matrix[positive][branch] = matrix[positive][branch].add(Complex.ONE);
            matrix[branch][positive] = matrix[branch][positive].add(Complex.ONE);
        }
        if (negative != Netlist.GROUND) {
            matrix[negative][branch] = matrix[negative][branch].subtract(Complex.ONE);
            matrix[branch][negative] = matrix[branch][negative].subtract(Complex.ONE);
        }
    }

    void branchCoefficient(int branch, Complex coefficient) {
        matrix[branch][branch] = matrix[branch][branch].add(coefficient);
    }

    void branchRightSide(int branch, double value) {
        injected[branch] = injected[branch].add(Complex.of(value));
    }

    /** Takes every excitation away, leaving the admittances: a noise analysis drives one source at a time. */
    void clearInjections() {
        Arrays.fill(injected, Complex.ZERO);
    }

    /** A source current pushed out of {@code positive} into the circuit. */
    void sourceCurrent(int positive, int negative, double value) {
        if (positive != Netlist.GROUND) {
            injected[positive] = injected[positive].add(Complex.of(value));
        }
        if (negative != Netlist.GROUND) {
            injected[negative] = injected[negative].subtract(Complex.of(value));
        }
    }

    /** Gauss–Jordan elimination with partial pivoting, over the complex numbers. */
    Complex[] solve() {
        int size = matrix.length;
        Complex[][] working = new Complex[size][size + 1];
        for (int row = 0; row < size; row++) {
            System.arraycopy(matrix[row], 0, working[row], 0, size);
            working[row][size] = injected[row];
        }
        for (int column = 0; column < size; column++) {
            int pivot = column;
            for (int row = column + 1; row < size; row++) {
                if (working[row][column].modulus() > working[pivot][column].modulus()) {
                    pivot = row;
                }
            }
            if (working[pivot][column].modulus() < 1e-14) {
                throw new IllegalArgumentException(UNSOLVABLE);
            }
            Complex[] swap = working[pivot];
            working[pivot] = working[column];
            working[column] = swap;
            for (int row = 0; row < size; row++) {
                if (row != column) {
                    Complex factor = working[row][column].divide(working[column][column]);
                    for (int inner = column; inner <= size; inner++) {
                        working[row][inner] = working[row][inner].subtract(factor.multiply(working[column][inner]));
                    }
                }
            }
        }
        Complex[] solution = new Complex[size];
        for (int row = 0; row < size; row++) {
            solution[row] = working[row][size].divide(working[row][row]);
        }
        return solution;
    }
}
