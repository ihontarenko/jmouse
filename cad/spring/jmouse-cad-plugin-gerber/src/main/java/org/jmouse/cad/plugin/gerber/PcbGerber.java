package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.gerber.GerberFile;
import org.jmouse.gerber.GerberPoint;
import org.jmouse.gerber.compose.GerberComposer;

import java.util.List;
import java.util.Optional;

/**
 * The pcb's frame as Gerber's, for every file of the set: millimetres, the pcb's origin, Y turned over — the pcb
 * draws Y down, Gerber up —, and a file opened with what it is and who wrote it.
 */
final class PcbGerber {

    /** The program that wrote the files: vendor, name, version. */
    private static final List<String> SOFTWARE = List.of("jMouse", "jMouse CAD", "1");

    private PcbGerber() {
    }

    /** A Gerber file of this function — {@code Copper,L1,Top}, {@code Soldermask,Top} —, dark on clear or clear on dark. */
    static GerberComposer file(String function, boolean positive) {
        return new GerberComposer(new GerberFile(function, positive, SOFTWARE, Optional.empty()));
    }

    /** A point of the pcb as Gerber's. */
    static GerberPoint out(double x, double y) {
        return new GerberPoint(x, -y);
    }

    /** A point of a part's footprint where the part stands, as Gerber's. */
    static GerberPoint placedOut(Placement placement, double x, double y) {
        Point placed = placement.placed(x, y);
        return out(placed.x(), placed.y());
    }
}
