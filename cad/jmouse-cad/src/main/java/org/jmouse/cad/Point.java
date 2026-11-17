package org.jmouse.cad;

/**
 * A place on a sheet or a pcb, in its units.
 *
 * @param x across, in its drawing's units
 * @param y down, in its drawing's units
 */
public record Point(double x, double y) {
}
