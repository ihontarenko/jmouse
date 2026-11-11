package org.jmouse.cad.simulation.engine;

/**
 * A device's noise as the small-signal analysis sees it: a random current between two nodes, described by its one-sided
 * power spectral density — a resistor's 4kT/R, a junction's 2qI. Sources are uncorrelated, so their powers add.
 *
 * @param positive the node the current leaves, or {@link Netlist#GROUND}
 * @param negative the node it enters, or {@link Netlist#GROUND}
 * @param density  its white power spectral density, A²/Hz
 * @param corner   the frequency below which its 1/f noise rises over the white, Hz; 0 for none
 */
record NoiseCurrent(int positive, int negative, double density, double corner) {

    /** Its power spectral density at {@code frequency}: white, and 1/f below the corner. */
    double densityAt(double frequency) {
        return density * (1 + corner / frequency);
    }

}
