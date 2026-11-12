package org.jmouse.cad.altium;

import org.jmouse.altium.AltiumSchematic.Component;
import org.jmouse.altium.AltiumSchematic.Point;
import org.jmouse.cad.exchange.imported.SymbolCanvas;

/**
 * Altium's sheet in our drawing's terms. Altium measures in ten mils with Y upwards: a unit is 0.254 mm, four drawing
 * units, so its 100-mil grid is two of our 20-unit steps. A part's drawing is stored where it stands on the sheet —
 * mirrored about its own vertical, then turned counter-clockwise about its location —; a symbol is that undone.
 */
final class AltiumGeometry {

    /** What a unit of Altium's — ten mils — is in millimetres. */
    private static final double MILLIMETRES_PER_UNIT = 0.254;

    /** What a unit of Altium's is in drawing units. */
    static final double UNITS = MILLIMETRES_PER_UNIT * SymbolCanvas.UNITS_PER_MILLIMETRE;

    private AltiumGeometry() {
    }

    /** A point of the sheet in the component's own frame: its turn undone about its location, then its mirror. */
    static Point local(Component component, Point onSheet) {
        double radians = Math.toRadians(-component.orientation().degrees());
        double x = onSheet.x() - component.location().x();
        double y = onSheet.y() - component.location().y();
        double turnedX = x * Math.cos(radians) - y * Math.sin(radians);
        double turnedY = x * Math.sin(radians) + y * Math.cos(radians);
        return new Point(component.mirrored() ? -turnedX : turnedX, turnedY);
    }

    /** The jMX turn of a part Altium turns so: jMX turns clockwise on a sheet whose Y goes down, so the complement. */
    static double sheetRotation(Component component) {
        return (360 - component.orientation().degrees()) % 360;
    }
}
