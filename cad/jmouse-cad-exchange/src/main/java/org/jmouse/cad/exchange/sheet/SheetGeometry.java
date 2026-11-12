package org.jmouse.cad.exchange.sheet;

import org.jmouse.cad.Point;
import org.jmouse.cad.circuit.Placement;

import java.util.List;

/**
 * The measures a schematic sheet is drawn to, as the sheet editor draws it ({@code SchematicDocument.ts}): its grid, how far
 * apart the pins of a box stand, how long a pin's lead is, how wide a pin's name is written — and how a point of a part's
 * own drawing lands on the sheet, mirrored, turned and moved with the part.
 */
public final class SheetGeometry {

    /** The sheet's grid step, in its units. */
    public static final double GRID = 20;

    /** Pins of a box stand two grid steps apart, so a wire can leave every one of them on its own grid line. */
    public static final double BOX_PITCH = GRID * 2;

    /** A pin leaves its box through a lead one grid step long; the wire starts at the lead's end. */
    public static final double BOX_LEAD = GRID;

    private SheetGeometry() {
    }

    /** How wide a pin's name is written inside a box: 9 px technical type. */
    public static double labelWidth(String text) {
        return text.length() * 5.5 + 6;
    }

    /** The widest of these names, as written inside a box; nothing is no width. */
    public static double widest(List<String> names) {
        return names.stream().mapToDouble(SheetGeometry::labelWidth).max().orElse(0);
    }

    /** Where the {@code index}-th of a row of pins stands from the row's middle, {@code pitch} apart. */
    public static double along(List<String> row, int index, double pitch) {
        return (index - (row.size() - 1) / 2.0) * pitch;
    }

    public static double roundUp(double value, double step) {
        return Math.ceil(value / step) * step;
    }

    /** A value on the sheet's grid. */
    public static double snap(double value) {
        return Math.round(value / GRID) * GRID;
    }

    /** A point of a part's own drawing as the part turns it: mirrored left to right first, then turned clockwise. */
    public static Point turned(Point point, double rotation, boolean mirrored) {
        double radians = Math.toRadians(rotation);
        double localX = mirrored ? -point.x() : point.x();
        return new Point(localX * Math.cos(radians) - point.y() * Math.sin(radians),
                localX * Math.sin(radians) + point.y() * Math.cos(radians));
    }

    /** A point of a part's own drawing on the sheet: turned with the part, then moved to where it stands. */
    public static Point onSheet(Placement.PlacedPart item, Point point) {
        return onSheet(item.at(), item.rotation(), item.mirrored(), point);
    }

    /** A point of a drawing standing at {@code at}, turned and mirrored so, on the sheet: a part's, or a mark's. */
    public static Point onSheet(Point at, double rotation, boolean mirrored, Point point) {
        Point turned = turned(point, rotation, mirrored);
        return new Point(at.x() + turned.x(), at.y() + turned.y());
    }
}
