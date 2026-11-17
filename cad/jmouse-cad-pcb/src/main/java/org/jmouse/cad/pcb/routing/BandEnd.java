package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

/**
 * What holds one end of a band: a point that stays where it is — a pad the drag did not move, a joint —, or a pad of a moved part,
 * which takes the end with it to the same spot of itself.
 */
sealed interface BandEnd {

    /** Where the end is a share of the way along the drag. */
    Point at(DragMotion motion, double share);

    /** Where the end stood before the drag. */
    Point was();

    /** An end held where it stands. */
    record Held(Point was) implements BandEnd {

        @Override
        public Point at(DragMotion motion, double share) {
            return was;
        }
    }

    /** An end at a joint that slides, going straight from where it stood to where it rests. */
    record Sliding(Point was, Point to) implements BandEnd {

        @Override
        public Point at(DragMotion motion, double share) {
            return share >= 1 ? to : new Point(was.x() + (to.x() - was.x()) * share, was.y() + (to.y() - was.y()) * share);
        }
    }

    /** An end on a pad of a moved part, going with it. */
    record OnMovedPad(Point was, String designator) implements BandEnd {

        @Override
        public Point at(DragMotion motion, double share) {
            return Lines.settled(motion.carried(designator, was, share));
        }
    }
}
