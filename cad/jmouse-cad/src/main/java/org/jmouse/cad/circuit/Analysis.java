package org.jmouse.cad.circuit;

/**
 * How a circuit is run: {@code transient}, and for how long, in seconds.
 *
 * @param kind     how it is run: {@code transient}
 * @param duration how long it runs, seconds
 */
public record Analysis(String kind, double duration) {
}
