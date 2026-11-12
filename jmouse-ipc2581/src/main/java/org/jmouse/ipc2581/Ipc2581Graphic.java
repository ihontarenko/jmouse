package org.jmouse.ipc2581;

import java.util.List;

/**
 * Something drawn on a layer where it is — a track, a silkscreen stroke, a poured area —, as a layer's features hold it:
 * a line drawn with a round pen, or a filled area.
 */
public sealed interface Ipc2581Graphic {

    record Line(Ipc2581Point from, Ipc2581Point to, double width) implements Ipc2581Graphic {
    }

    /** An arc of a circle round {@code centre}, from one point to another, turning clockwise or not. */
    record Arc(Ipc2581Point from, Ipc2581Point to, Ipc2581Point centre, boolean clockwise, double width) implements Ipc2581Graphic {
    }

    /** Straight runs through the points, one pen all along. */
    record Polyline(List<Ipc2581Point> points, double width) implements Ipc2581Graphic {

        public Polyline {
            if (points.size() < 2) {
                throw new IllegalArgumentException("A polyline runs through two points at least");
            }
            points = List.copyOf(points);
        }
    }

    /** A filled area. */
    record Area(Ipc2581Polygon polygon) implements Ipc2581Graphic {
    }
}
