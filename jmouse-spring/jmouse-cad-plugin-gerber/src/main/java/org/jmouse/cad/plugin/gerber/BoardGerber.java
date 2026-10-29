package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.gerber.GerberFile;
import org.jmouse.gerber.GerberPoint;
import org.jmouse.gerber.GerberShape;
import org.jmouse.gerber.compose.GerberComposer;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;

import java.util.List;
import java.util.Optional;

/**
 * The board's frame as Gerber's, for every file of the set: millimetres, the board's origin, Y turned over — the board
 * draws Y down, Gerber up —, and a file opened with what it is and who wrote it.
 */
final class BoardGerber {

    /** The program that wrote the files: vendor, name, version. */
    private static final List<String> SOFTWARE = List.of("jMouse", "jMouse CAD", "1");

    private static final PathParser PATHS = new PathParser();

    private BoardGerber() {
    }

    /** A Gerber file of this function — {@code Copper,L1,Top}, {@code Soldermask,Top} —, dark on clear or clear on dark. */
    static GerberComposer file(String function, boolean positive) {
        return new GerberComposer(new GerberFile(function, positive, SOFTWARE, Optional.empty()));
    }

    /** A point of the board as Gerber's. */
    static GerberPoint out(double x, double y) {
        return new GerberPoint(x, -y);
    }

    /** A point of a part's footprint where the part stands, as Gerber's. */
    static GerberPoint placedOut(BoardDrawing.Placement placement, double x, double y) {
        BoardDrawing.Point placed = placement.placed(x, y);
        return out(placed.x(), placed.y());
    }

    /**
     * A footprint's drawing — a silkscreen line, a copper logo — placed with its part: filled as an area, or stroked
     * with a round pen of {@code width}.
     *
     * @param purpose the aperture's function: {@code Conductor}, {@code Other,Legend}
     */
    static void outline(GerberComposer composer, BoardDrawing.Placement placement, FootprintDrawing.Outline outline, double width,
                        String purpose, int steps) {
        int pen = outline.filled() ? 0 : composer.aperture(new GerberShape.Circle(width), purpose);
        for (List<double[]> run : runs(outline.path(), steps)) {
            List<GerberPoint> placed = run.stream().map(point -> placedOut(placement, point[0], point[1])).toList();
            if (outline.filled()) {
                composer.region(List.of(placed));
            } else {
                composer.line(pen, placed);
            }
        }
    }

    /** The runs a path draws, each of at least two points, a curve in {@code steps} straight pieces. */
    static List<List<double[]>> runs(String path, int steps) {
        if (path == null || path.isBlank()) {
            return List.of();
        }
        return PathPolylines.of(PATHS.parse(path), steps).stream().filter(run -> run.size() > 1).toList();
    }
}
