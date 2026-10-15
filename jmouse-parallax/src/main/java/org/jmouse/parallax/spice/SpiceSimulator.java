package org.jmouse.parallax.spice;

/**
 * A SPICE program the engine can hand a deck to — the second engine beside its own, for the batch analyses and for
 * «compare with SPICE». The deck carries its own {@code .control} block; what comes back is what the program printed.
 * How the program is reached — an executable on this machine, a container — is the implementation's business.
 */
public interface SpiceSimulator {

    /**
     * Runs one deck in batch mode.
     *
     * @return everything the program printed
     * @throws SpiceRunException when the program cannot be started, fails, or takes longer than it is allowed
     */
    String run(String deck);
}
