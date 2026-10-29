package org.jmouse.cad.simulation.engine;

import org.jmouse.numerics.SparseSystem;


/**
 * The modified-nodal system being assembled for one Newton iteration, and everything a device needs to stamp itself
 * into it: the current guess, the analysis it is part of, and — in a transient — the accepted state of the previous
 * time point and the step to the next one.
 *
 * <p>Unknowns are the node voltages, then the currents of the elements that carry their own (voltage sources,
 * inductors). A node's row says that the currents leaving it through the devices equal the current injected into
 * it. A current "through a device from p to n" leaves node p and enters node n.</p>
 */
final class Assembly {

    /** How a transient step turns d/dt into algebra: backward Euler after a discontinuity, trapezoidal otherwise. */
    enum Integration { BACKWARD_EULER, TRAPEZOIDAL }

    private final int        size;
    private final boolean    transientAnalysis;
    private final double[]   companion;
    private final double[]   previous;
    private final double     time;
    private final double     step;
    private final Integration integration;
    private final double     sourceScale;

    /** The system every iteration assembles into; owned by the solver and reused, so nothing is allocated per iteration. */
    private final SparseSystem system;
    private double[]   guess;

    private Assembly(SparseSystem system, boolean transientAnalysis, double[] previous, double[] companion,
                     double time, double step, Integration integration, double sourceScale) {
        this.system = system;
        this.size = system.size();
        this.transientAnalysis = transientAnalysis;
        this.previous = previous;
        this.companion = companion;
        this.time = time;
        this.step = step;
        this.integration = integration;
        this.sourceScale = sourceScale;
    }

    /** The DC state: capacitors open, inductors shorted, every source at its constant part times {@code scale}. */
    static Assembly operatingPoint(SparseSystem system, int devices, double scale) {
        return new Assembly(system, false, new double[system.size()], new double[devices], 0, 0, null, scale);
    }

    /** One transient step from the accepted state {@code previous} to {@code time}, {@code step} later. */
    static Assembly transientStep(SparseSystem system, double[] previous, double[] companion, double time, double step, Integration integration) {
        return new Assembly(system, true, previous, companion, time, step, integration, 1);
    }

    /** Starts an iteration at the given guess with an empty system. */
    void begin(double[] guess) {
        this.guess = guess;
        system.clear();
    }

    /** Keeps what is assembled so far — what nothing in a run changes — for {@link #beginFromConstant}. */
    void keepAsConstant() {
        system.keepAsConstant();
    }

    /** Starts an iteration at the given guess with the part {@link #keepAsConstant} kept already in place. */
    void beginFromConstant(double[] guess) {
        this.guess = guess;
        system.clearToConstant();
    }

    /** Keeps what is assembled so far — what does not depend on the guess — for {@link #beginFromBase}. */
    void keepAsBase() {
        system.keepAsBase();
    }

    /** Starts an iteration at the given guess with the part {@link #keepAsBase} kept already in place. */
    void beginFromBase(double[] guess) {
        this.guess = guess;
        system.clearToBase();
    }

    /** Solves what was assembled; null when it is singular. */
    double[] solve() {
        return system.solve();
    }

    /** How far the equations assembled at the current guess are from holding there. */
    double residual() {
        return system.residualNorm(guess);
    }

    /** After a failed solve: the unknown no row or column of the system mentions, or -1. */
    int unconstrained() {
        return system.unconstrained();
    }

    boolean isTransient() {
        return transientAnalysis;
    }

    double time() {
        return time;
    }

    double step() {
        return step;
    }

    Integration integration() {
        return integration;
    }

    double sourceScale() {
        return sourceScale;
    }

    int size() {
        return size;
    }

    /** Any unknown's value in the current guess. */
    double unknown(int index) {
        return guess[index];
    }

    /** A conductance between two nodes through slots found once ({@link ConductanceSlots}). */
    void conductance(ConductanceSlots kept, int positive, int negative, double conductance) {
        int[] slots = kept.resolve(system, this, positive, negative);
        addAt(slots[0], conductance);
        addAt(slots[1], conductance);
        addAt(slots[2], -conductance);
        addAt(slots[3], -conductance);
    }

    /** Adds to one entry of the matrix; a row or column of node 0 is dropped. */
    void add(int row, int column, double value) {
        if (row != Netlist.GROUND && column != Netlist.GROUND) {
            system.add(row, column, value);
        }
    }

    /** The system's slot of one entry, for {@link #addAt}; -1 for a row or column of node 0, which is dropped. */
    int slot(int row, int column) {
        if (row == Netlist.GROUND || column == Netlist.GROUND) {
            return -1;
        }
        return system.slot(row, column);
    }

    /** Adds to the entry a {@link #slot} named; nothing for node 0's. */
    void addAt(int slot, double value) {
        if (slot >= 0) {
            system.addAt(slot, value);
        }
    }

    /** The system assembled into: the slots a device resolved belong to it alone. */
    SparseSystem system() {
        return system;
    }

    /** Adds to one entry of the right side; node 0 has none. */
    void inject(int row, double value) {
        if (row != Netlist.GROUND) {
            system.addRightSide(row, value);
        }
    }

    /** A node's voltage in the current guess; node 0 is 0. */
    double voltage(int node) {
        return node == Netlist.GROUND ? 0 : guess[node];
    }

    /** A node's voltage at the previous accepted time point. */
    double previousVoltage(int node) {
        return node == Netlist.GROUND ? 0 : previous[node];
    }

    /** Every unknown at the previous accepted time point; read, never written. */
    double[] previousState() {
        return previous;
    }

    /** An unknown's value at the previous accepted time point — a branch current, for an inductor. */
    double previousValue(int index) {
        return previous[index];
    }

    /** The device's own memory between time points: a capacitor's current. */
    double companion(int slot) {
        return companion[slot];
    }

    void rememberCompanion(int slot, double value) {
        companion[slot] = value;
    }

    /** A conductance between two nodes. */
    void conductance(int positive, int negative, double conductance) {
        if (positive != Netlist.GROUND) {
            system.add(positive, positive, conductance);
        }
        if (negative != Netlist.GROUND) {
            system.add(negative, negative, conductance);
        }
        if (positive != Netlist.GROUND && negative != Netlist.GROUND) {
            system.add(positive, negative, -conductance);
            system.add(negative, positive, -conductance);
        }
    }

    /** A fixed current flowing through the device from {@code positive} to {@code negative}. */
    void current(int positive, int negative, double current) {
        if (positive != Netlist.GROUND) {
            system.addRightSide(positive, -current);
        }
        if (negative != Netlist.GROUND) {
            system.addRightSide(negative, current);
        }
    }

    /** A branch current entering the KCL rows of its terminals; its own row starts as V(+) − V(−). */
    void branch(int positive, int negative, int branch) {
        if (positive != Netlist.GROUND) {
            system.add(positive, branch, 1);
            system.add(branch, positive, 1);
        }
        if (negative != Netlist.GROUND) {
            system.add(negative, branch, -1);
            system.add(branch, negative, -1);
        }
    }

    void branchRightSide(int branch, double value) {
        system.addRightSide(branch, value);
    }
}
