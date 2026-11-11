package org.jmouse.cad.simulation.elaboration;

/**
 * An element the bench puts into a circuit that no document writes — what a failed part became, an offset in series
 * with a source —, between two nets. Its value is the solver's: a break and a bridge it can hold in a matrix, a volt it
 * adds, never a part's physics.
 *
 * @param value ohms for a conductance, volts for a voltage source, amperes for a current source
 */
public record EngineElement(String id, Kind kind, String positive, String negative, double value) {

    /** What the engine makes of it. */
    public enum Kind {
        CONDUCTANCE, VOLTAGE_SOURCE, CURRENT_SOURCE
    }
}
