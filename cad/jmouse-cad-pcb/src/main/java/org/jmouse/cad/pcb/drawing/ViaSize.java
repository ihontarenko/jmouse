package org.jmouse.cad.pcb.drawing;

/**
 * A via's drill and the copper ring's diameter around it, millimetres.
 *
 * @param drill    the hole's diameter
 * @param diameter the copper ring's diameter around it
 */
public record ViaSize(double drill, double diameter) {
}
