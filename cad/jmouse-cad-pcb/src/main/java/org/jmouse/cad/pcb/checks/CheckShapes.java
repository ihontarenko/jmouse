package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Hole;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperShapes;
import org.jmouse.geometry.index.Box;

import java.util.List;

/** The boxes and corners the checks measure things by: a placed pad's, a hole's, a marking's. */
final class CheckShapes {

    private CheckShapes() {
    }

    /** The box a placed pad covers: its width and height, turned a quarter where its turn and its part's make one. */
    static Box padBox(CopperNets.PlacedPad pad) {
        double turned = (((pad.pad().rotation() + pad.placement().rotation()) % 180) + 180) % 180;
        double across = turned == 90 ? pad.pad().height() : pad.pad().width();
        double down = turned == 90 ? pad.pad().width() : pad.pad().height();
        return new Box(pad.centre().x() - across / 2, pad.centre().y() - down / 2, pad.centre().x() + across / 2, pad.centre().y() + down / 2);
    }

    /** The box a hole covers, its ring with it. */
    static Box holeBox(Hole hole) {
        return Box.around(hole.x(), hole.y(), CopperShapes.reach(hole));
    }

    /** The box holding some corners. */
    static Box boxOf(List<Point> corners) {
        return new Box(corners.stream().mapToDouble(Point::x).min().orElse(0),
                corners.stream().mapToDouble(Point::y).min().orElse(0),
                corners.stream().mapToDouble(Point::x).max().orElse(0),
                corners.stream().mapToDouble(Point::y).max().orElse(0));
    }

    /** A box's four corners, clockwise from its top left. */
    static List<Point> cornersOf(Box box) {
        return List.of(new Point(box.left(), box.top()), new Point(box.right(), box.top()),
                new Point(box.right(), box.bottom()), new Point(box.left(), box.bottom()));
    }

    /** The middle of a box. */
    static Point middleOf(Box box) {
        return new Point((box.left() + box.right()) / 2, (box.top() + box.bottom()) / 2);
    }
}
