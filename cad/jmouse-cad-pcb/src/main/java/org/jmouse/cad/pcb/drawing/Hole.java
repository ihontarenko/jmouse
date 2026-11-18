package org.jmouse.cad.pcb.drawing;

/**
 * A hole drilled through the pcb that is no part's.
 *
 * @param x     where it stands, millimetres from the left
 * @param y     where it stands, millimetres down
 * @param drill its diameter, millimetres
 * @param ring  the copper ring's diameter round it, millimetres; 0: a bare hole
 */
public record Hole(double x, double y, double drill, double ring) {
}
