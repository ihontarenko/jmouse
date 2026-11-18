package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.Point;

/**
 * A measure drawn on the pcb: the distance between two points.
 *
 * @param from   where it is measured from
 * @param to     where it is measured to
 * @param offset how far its line is drawn aside of the two points, millimetres
 */
public record Dimension(Point from, Point to, double offset) {
}
