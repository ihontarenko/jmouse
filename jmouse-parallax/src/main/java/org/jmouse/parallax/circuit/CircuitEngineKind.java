package org.jmouse.parallax.circuit;

/** The engines a circuit's batch analyses can be computed by — what the {@code engine} setting chooses. */
public enum CircuitEngineKind {

    /** This library's own engine: the one the live bench, the computed graphs and the datasheet checks run on. */
    PARALLAX,

    /** SPICE, the second engine: the circuit exported as a deck and run by the simulator the host configured. */
    SPICE
}
