package org.jmouse.cad.simulation.engine;

/**
 * How much local error a step may make, measured where it matters: in what the circuit stores. A capacitor's error is a
 * charge, an inductor's a flux, a model's its charges and states — never a node's voltage as such. A node behind a
 * blocking diode with a few picofarads on it may wander by millivolts without anything in the circuit noticing; judged
 * by its voltage, it kept the whole circuit at nanosecond steps. These are SPICE's own tolerances and its own rule:
 * {@code trtol · (reltol · |stored| + chgtol)}.
 */
final class Truncation {

    static final double RELATIVE = 1e-3;
    /** SPICE's chgtol, coulombs — and the same for a flux in webers. */
    static final double CHARGE = 1e-14;
    /** A model's own continuous state: a share of its value, or this much. */
    static final double STATE = 1e-4;
    /** SPICE's trtol: how much the truncation estimate is trusted over the tolerance. */
    static final double TRUST = 7;

    private Truncation() {
    }

    /** The share of the allowed error a change of {@code error} in something stored at {@code stored} uses. */
    static double share(double error, double stored, double absolute) {
        return Math.abs(error) / (TRUST * (RELATIVE * Math.abs(stored) + absolute));
    }
}
