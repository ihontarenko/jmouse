package org.jmouse.cad.exchange.pcb;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;

import org.jmouse.svg.path.node.CircularArc;

import java.util.List;
import java.util.Optional;

/**
 * One thing drawn on a layer of the pcb — copper, a mask's opening, paste, a silkscreen stroke —, in the pcb's frame:
 * millimetres, Y down. A format draws it its own way: Gerber with an aperture or a region, a solid model as an area.
 */
public sealed interface ArtworkFeature {

    /** What the feature is for, which a format may record beside it. */
    enum Role { CONDUCTOR, SURFACE_PAD, COMPONENT_PAD, VIA_PAD, LEGEND, OPENING }

    Role role();

    /** The net the copper carries, as the pcb's connections give it; null for none — a legend, an opening, unwired copper. */
    String net();

    /** Straight runs between the points, drawn with a round pen as wide as {@code width}. */
    record Stroke(List<Point> points, double width, Role role, String net) implements ArtworkFeature {
    }

    /** An arc of a circle from a point through another to a third, drawn with a round pen; three in a line are straight. */
    record Arc(Point from, Point through, Point to, double width, Role role, String net)
            implements ArtworkFeature {

        /** The circle an arc runs on: its centre, in the pcb's frame, and whether it turns clockwise as the pcb is seen. */
        public record Circle(Point centre, boolean clockwise) {
        }

        /** The circle through its three points; empty for three in a line, which a format draws as a straight run. */
        public Optional<Circle> circle() {
            return CircularArc.through(from.x(), from.y(), through.x(), through.y(), to.x(), to.y()).map(found -> {
                // The pcb's Y grows downwards: a turn that winds positively in its numbers is clockwise to the eye.
                double winding = (through.x() - from.x()) * (to.y() - through.y()) - (through.y() - from.y()) * (to.x() - through.x());
                return new Circle(new Point(found.centerX(), found.centerY()), winding > 0);
            });
        }
    }

    /** A pad where its part stands it, grown all round by {@code growth}. */
    record Pad(Placement placement, FootprintDrawing.Pad pad, double growth, Role role, String net) implements ArtworkFeature {
    }

    /** A filled circle. */
    record Disc(Point centre, double diameter, Role role, String net) implements ArtworkFeature {
    }

    /** A filled area: its outline, less its holes. */
    record Region(List<Point> outline, List<List<Point>> holes, Role role, String net) implements ArtworkFeature {
    }
}
