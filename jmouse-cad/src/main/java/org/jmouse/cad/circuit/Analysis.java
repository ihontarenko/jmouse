package org.jmouse.cad.circuit;

/** How a circuit is run: {@code transient}, and for how long, in seconds. */
public record Analysis(String kind, double duration) {
}
