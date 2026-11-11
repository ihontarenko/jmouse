package org.jmouse.cad.simulation.engine;

/**
 * Newton–Raphson on a circuit's modified-nodal system — the one loop the operating point and every transient step run.
 * A linear circuit is solved in one pass; a nonlinear one is stepped, each step limited as SPICE limits it
 * ({@link NewtonSteps}), until no unknown moves by more than its {@link Settling} allows.
 *
 * <p>With {@code searching}, a line search: a step is kept only where the equations hold better than where it started;
 * otherwise it is halved, back towards the point it left. A saturating model — an op-amp's slew, s / √(1 + s²) — sends
 * a full Newton step from s to −s³ once |s| > 1, and plain Newton bounced further out every iteration. It is never the
 * first resort: where a diode is off its rows are a few picosiemens, and the residual they give is too rough a measure
 * for a circuit plain Newton solves at once.</p>
 */
final class CircuitNewton {

    /** How short the line search lets a Newton step become before it takes it anyway. */
    private static final double SMALLEST_FRACTION = 1.0 / 1024;

    /**
     * When a run has settled: no unknown moves by more than {@code relative · |x| + absolute}. {@code confirming} asks for
     * one iteration past the first before any is believed — an operating point starts from a guess nothing was solved
     * at; a transient step starts from a prediction its first iteration is linearized exactly at.
     */
    record Settling(double relative, double absolute, boolean confirming) {

        /** The operating point's: SPICE's 1e-6 volts, relative and absolute, confirmed by a second iteration. */
        static final Settling OPERATING_POINT = new Settling(1e-6, 1e-6, true);
        /** A transient step's: a tenth of a thousandth of each unknown and a microvolt, believed at once. */
        static final Settling TRANSIENT = new Settling(1e-4, 1e-6, false);

        boolean settled(double[] guess, double[] next) {
            for (int index = 0; index < next.length; index++) {
                if (Math.abs(next[index] - guess[index]) > relative * Math.abs(next[index]) + absolute) {
                    return false;
                }
            }
            return true;
        }
    }

    /** What a run stamps: the system linearized at a guess. */
    interface Linearization {

        /** Assembles the system at {@code guess}, the run's {@code iteration}-th. */
        void assemble(Assembly assembly, double[] guess, int iteration);

        /**
         * The system is singular at the run's first guess: throws when that is the circuit's own — no unique solution
         * anywhere —; returns when it is the guess's, and the run answers nothing.
         */
        void singularAtStart(Assembly assembly);
    }

    private final int       size;
    private final int       nodes;
    private final int       branches;
    private final boolean   nonlinear;
    private final Settling  settling;

    CircuitNewton(Netlist netlist, boolean nonlinear, Settling settling) {
        this.size = netlist.size();
        this.nodes = netlist.nodes().size();
        this.branches = netlist.branches().size();
        this.nonlinear = nonlinear;
        this.settling = settling;
    }

    boolean nonlinear() {
        return nonlinear;
    }

    /**
     * One run from {@code start}, of at most {@code iterations}: the solution, or null where it did not settle.
     *
     * @param searching whether to keep only the steps that lower the residual ({@link CircuitNewton})
     */
    double[] solve(Assembly assembly, double[] start, int iterations, boolean searching, Linearization linearization) {
        if (size == 0) {
            return new double[0];
        }
        double[] guess = start;
        double[] accepted = null;
        double[] direction = null;
        double acceptedResidual = Double.POSITIVE_INFINITY;
        double fraction = 1;
        for (int iteration = 0; iteration < iterations; iteration++) {
            linearization.assemble(assembly, guess, iteration);
            if (searching) {
                double residual = assembly.residual();
                if (accepted != null && !(residual < acceptedResidual) && fraction > SMALLEST_FRACTION) {
                    fraction /= 2;
                    guess = along(accepted, direction, fraction);
                    continue;
                }
                accepted = guess;
                acceptedResidual = residual;
                fraction = 1;
            }
            double[] next = assembly.solve();
            if (next == null) {
                if (iteration == 0) {
                    linearization.singularAtStart(assembly);
                }
                return null;
            }
            if (!nonlinear) {
                return next;
            }
            if ((iteration > 0 || !settling.confirming()) && settling.settled(guess, next)) {
                return next;
            }
            for (double value : next) {
                if (!Double.isFinite(value)) {
                    return null;
                }
            }
            double[] stepped = NewtonSteps.limited(guess, next, nodes, branches);
            if (searching) {
                direction = new double[stepped.length];
                for (int index = 0; index < stepped.length; index++) {
                    direction[index] = stepped[index] - guess[index];
                }
            }
            guess = stepped;
        }
        return null;
    }

    /** The point a share of the way along a Newton step. */
    private static double[] along(double[] from, double[] direction, double fraction) {
        double[] point = new double[from.length];
        for (int index = 0; index < from.length; index++) {
            point[index] = from[index] + direction[index] * fraction;
        }
        return point;
    }
}
