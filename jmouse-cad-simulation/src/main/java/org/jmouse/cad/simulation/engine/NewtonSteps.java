package org.jmouse.cad.simulation.engine;

/**
 * How far one Newton iteration may move the unknowns — shared by every solver of a circuit, so that a circuit settles
 * the same way whether it is solved for its operating point, live, or as a computed transient.
 */
final class NewtonSteps {

    /** No node voltage and no state moves more than this in one iteration, volts or the state's own unit. */
    static final double STEP_LIMIT = 10;

    private NewtonSteps() {
    }

    /**
     * The Newton step, shortened so that no node voltage and no state moves more than {@link #STEP_LIMIT} in one
     * iteration. A model in saturation has an almost flat equation, and an undamped step off a flat equation lands
     * anywhere — SPICE limits its steps for the same reason. Branch currents are not limited: they follow.
     *
     * <p>The node voltages are shortened together, keeping their step's direction; each state on its own. Shortened
     * with the nodes, one state's step — an op-amp's gain stage asked to jump by its open-loop gain, 10⁶ V — scaled every
     * node's by 10⁻⁵, and the circuit stood still for all its iterations: no op-amp circuit had an operating point, and
     * a relay driver's transistor turning off never let its collector rise.</p>
     */
    static double[] limited(double[] guess, double[] next, int nodes, int branches) {
        double largest = 0;
        for (int index = 0; index < nodes; index++) {
            largest = Math.max(largest, Math.abs(next[index] - guess[index]));
        }
        double scale = largest <= STEP_LIMIT ? 1 : STEP_LIMIT / largest;
        double[] step = next.clone();
        for (int index = 0; index < next.length; index++) {
            double change = next[index] - guess[index];
            if (index < nodes) {
                step[index] = guess[index] + change * scale;
            } else if (index >= nodes + branches) {
                step[index] = guess[index] + Math.max(-STEP_LIMIT, Math.min(STEP_LIMIT, change));
            }
        }
        return step;
    }
}
