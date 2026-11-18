package org.jmouse.cad.pcb.drawing;

/**
 * Where a part's designator stands in its own frame, and how it is turned.
 *
 * @param x        millimetres right of the part's origin
 * @param y        millimetres below the part's origin
 * @param rotation how far it is turned, degrees
 * @param layer    the layer it is printed on; empty for its footprint's field's
 */
public record Reference(double x, double y, double rotation, String layer) {

    /** A designator standing where its footprint's field says it is printed. */
    public Reference(double x, double y, double rotation) {
        this(x, y, rotation, "");
    }
}
