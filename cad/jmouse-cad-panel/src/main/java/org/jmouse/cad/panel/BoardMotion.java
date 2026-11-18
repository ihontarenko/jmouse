package org.jmouse.cad.panel;

import org.jmouse.cad.Point;
import org.jmouse.svg.path.node.PathData;
import org.jmouse.svg.path.node.PathMotion;
import org.jmouse.svg.path.parser.PathParser;

/**
 * How one copy of a board is carried to where it stands on a panel: turned about the origin of its drawing as a part is
 * turned on a pcb — counter-clockwise as the pcb is seen —, then moved.
 *
 * @param rotation degrees
 */
record BoardMotion(double rotation, double acrossX, double acrossY) {

    private static final PathParser PATHS = new PathParser();

    Point point(Point point) {
        // The drawing's Y grows downwards: what is seen as counter-clockwise runs against the frame's own angle.
        double radians = Math.toRadians(-rotation);
        // A board turns a quarter at a time: these are whole numbers, and must stay so.
        double cosine = Math.rint(Math.cos(radians));
        double sine = Math.rint(Math.sin(radians));
        return new Point(point.x() * cosine - point.y() * sine + acrossX, point.x() * sine + point.y() * cosine + acrossY);
    }

    String path(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        PathData parsed = PATHS.parse(path);
        return PathMotion.of(parsed, Math.toRadians(-rotation), acrossX, acrossY).toSource();
    }
}
