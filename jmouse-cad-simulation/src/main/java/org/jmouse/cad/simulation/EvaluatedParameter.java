package org.jmouse.cad.simulation;

/**
 * A component's parameter as the engine runs it: its value in SI, whether it is counted per unit of a package, what it
 * is, and the unit it was written in.
 *
 * @param unit the SI unit it was written in — {@code Ohm}, {@code F} —; empty for a bare number or an expression
 */
public record EvaluatedParameter(String name, double value, boolean perUnit, String description, String unit) {
}
