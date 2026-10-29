package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.gerber.GerberPoint;
import org.jmouse.gerber.GerberShape;
import org.jmouse.gerber.compose.GerberComposer;
import org.jmouse.svg.path.node.CircularArc;
import org.jmouse.cad.board.geometry.Teardrops;
import org.jmouse.cad.board.geometry.ZonePours;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * One copper layer as Gerber X2: its zones as poured, the pads with copper on it, a footprint's own copper drawing, the
 * tracks — a rounded corner drawn as the arc it is —, the board's arcs, the vias that reach it, the rings of plated
 * mounting holes, and the drops its tracks are widened with.
 */
final class CopperLayer {

    private static final String CONDUCTOR = "Conductor";

    /** A corner sharper than this, or straighter, is left as it is drawn. */
    private static final double STRAIGHT = 1e-6;

    private final PadShapes pads;
    private final int       curveSteps;

    CopperLayer(PadShapes pads, int curveSteps) {
        this.pads = pads;
        this.curveSteps = curveSteps;
    }

    /**
     * The layer's file: {@code Copper,L1,Top} for the first of the stack, its zones as poured first.
     *
     * @param pours every zone of the board as poured, by its place in the board's list
     * @param drops the drops its tracks are widened with where they enter copper
     */
    String write(BoardFabrication fabrication, String layer, int position, List<Optional<Geometry>> pours,
                 List<Teardrops.Drop> drops) {
        List<String> stack = fabrication.board().copperLayerIds();
        String side = layer.equals(BoardDrawing.TOP) ? "Top" : layer.equals(BoardDrawing.BOTTOM) ? "Bot" : "Inr";
        GerberComposer copper = BoardGerber.file("Copper,L" + (position + 1) + "," + side, true);
        for (int index = 0; index < pours.size(); index++) {
            if (fabrication.board().zones().get(index).layer().equals(layer)) {
                pours.get(index).ifPresent(pour -> pour(copper, pour));
            }
        }
        for (BoardFabrication.SeatedPart part : fabrication.parts()) {
            FootprintDrawing drawing = part.drawing().orElse(null);
            if (drawing == null) {
                continue;
            }
            for (FootprintDrawing.Pad pad : drawing.pads()) {
                if (pad.hasCopperOn(layer, part.placement())) {
                    pads.draw(copper, part.placement(), pad, 0, pad.isSurface() ? "SMDPad,CuDef" : "ComponentPad");
                }
            }
            // A footprint's copper drawing — a logo, a shield — is copper of its side as it is drawn.
            for (FootprintDrawing.Outline outline : drawing.outlines()) {
                if ("copper".equals(outline.layer()) && part.placement().side().equals(layer)) {
                    BoardGerber.outline(copper, part.placement(), outline, outline.width(), CONDUCTOR, curveSteps);
                }
            }
        }
        BoardDrawing board = fabrication.board();
        for (BoardDrawing.Track track : board.tracks()) {
            if (track.layer().equals(layer)) {
                track(copper, track);
            }
        }
        for (BoardDrawing.Arc arc : board.arcs()) {
            if (arc.layer().equals(layer)) {
                arc(copper, arc);
            }
        }
        for (BoardDrawing.Via via : board.vias()) {
            if (board.reaches(via, layer)) {
                copper.flash(copper.aperture(new GerberShape.Circle(via.diameter()), "ViaPad"), BoardGerber.out(via.x(), via.y()));
            }
        }
        // A plated mounting hole's ring, on every copper layer of the stack.
        for (BoardDrawing.Hole hole : board.holes()) {
            if (hole.ring() > 0 && stack.contains(layer)) {
                copper.flash(copper.aperture(new GerberShape.Circle(hole.ring()), "ComponentPad"), BoardGerber.out(hole.x(), hole.y()));
            }
        }
        for (Teardrops.Drop drop : drops) {
            if (drop.layer().equals(layer)) {
                copper.region(List.of(drop.outline().stream().map(point -> BoardGerber.out(point.x(), point.y())).toList()));
            }
        }
        return copper.document().toSource();
    }

    /** A zone's copper: each polygon's outline dark, its holes cleared, the largest first so nothing in a hole is cleared away. */
    private static void pour(GerberComposer composer, Geometry pour) {
        for (Polygon polygon : ZonePours.polygonsOf(pour)) {
            composer.region(List.of(ring(polygon.getExteriorRing())));
            if (polygon.getNumInteriorRing() > 0) {
                composer.polarity(false);
                for (int hole = 0; hole < polygon.getNumInteriorRing(); hole++) {
                    composer.region(List.of(ring(polygon.getInteriorRingN(hole))));
                }
                composer.polarity(true);
            }
        }
    }

    private static List<GerberPoint> ring(LineString ring) {
        return Arrays.stream(ring.getCoordinates()).map(point -> BoardGerber.out(point.x, point.y)).toList();
    }


    /**
     * A track: straight between its points, each corner rounded by its {@code rounding} — the arc cut back from the
     * corner as far as the turn allows, never past half a leg —, drawn as a Gerber arc.
     */
    private static void track(GerberComposer composer, BoardDrawing.Track track) {
        List<BoardDrawing.Point> points = track.points();
        if (points.size() < 2) {
            return;
        }
        int pen = composer.aperture(new GerberShape.Circle(track.width()), CONDUCTOR);
        List<GerberPoint> run = new ArrayList<>();
        run.add(BoardGerber.out(points.getFirst().x(), points.getFirst().y()));
        for (int index = 1; index < points.size() - 1; index++) {
            Optional<Corner> rounded = corner(points.get(index - 1), points.get(index), points.get(index + 1), track.rounding());
            if (rounded.isEmpty()) {
                run.add(BoardGerber.out(points.get(index).x(), points.get(index).y()));
                continue;
            }
            Corner corner = rounded.get();
            run.add(corner.from());
            composer.line(pen, run);
            composer.arc(pen, corner.from(), corner.to(), corner.centre(), corner.clockwise());
            run = new ArrayList<>(List.of(corner.to()));
        }
        run.add(BoardGerber.out(points.getLast().x(), points.getLast().y()));
        composer.line(pen, run);
    }

    /** A rounded corner as Gerber draws it: where the arc leaves the way in, where it joins the way out, about what. */
    private record Corner(GerberPoint from, GerberPoint to, GerberPoint centre, boolean clockwise) {
    }

    private static Optional<Corner> corner(BoardDrawing.Point previous, BoardDrawing.Point corner, BoardDrawing.Point next,
                                           double rounding) {
        double inLength = Math.hypot(corner.x() - previous.x(), corner.y() - previous.y());
        double outLength = Math.hypot(next.x() - corner.x(), next.y() - corner.y());
        if (rounding <= 0 || inLength == 0 || outLength == 0) {
            return Optional.empty();
        }
        double[] inward = {(previous.x() - corner.x()) / inLength, (previous.y() - corner.y()) / inLength};
        double[] outward = {(next.x() - corner.x()) / outLength, (next.y() - corner.y()) / outLength};
        double between = Math.acos(Math.max(-1, Math.min(1, inward[0] * outward[0] + inward[1] * outward[1])));
        if (between > Math.PI - STRAIGHT || between < STRAIGHT) {
            return Optional.empty();
        }
        double tangent = Math.tan(between / 2);
        double cut = Math.min(rounding / tangent, Math.min(inLength / 2, outLength / 2));
        double radius = cut * tangent;
        double[] start = {corner.x() + inward[0] * cut, corner.y() + inward[1] * cut};
        double[] end = {corner.x() + outward[0] * cut, corner.y() + outward[1] * cut};
        // The centre stands off the way in by the radius, on the side the track turns to: the one the end is that far from.
        double[] left = {start[0] + inward[1] * radius, start[1] - inward[0] * radius};
        double[] right = {start[0] - inward[1] * radius, start[1] + inward[0] * radius};
        double[] centre = Math.abs(Math.hypot(end[0] - left[0], end[1] - left[1]) - radius)
                <= Math.abs(Math.hypot(end[0] - right[0], end[1] - right[1]) - radius) ? left : right;
        GerberPoint from = BoardGerber.out(start[0], start[1]);
        GerberPoint to = BoardGerber.out(end[0], end[1]);
        GerberPoint about = BoardGerber.out(centre[0], centre[1]);
        return Optional.of(new Corner(from, to, about, turn(from, about, to) < 0));
    }

    /** An arc of the board through its three points; three in a line are a straight track. */
    private static void arc(GerberComposer composer, BoardDrawing.Arc arc) {
        int pen = composer.aperture(new GerberShape.Circle(arc.width()), CONDUCTOR);
        GerberPoint from = BoardGerber.out(arc.from().x(), arc.from().y());
        GerberPoint through = BoardGerber.out(arc.through().x(), arc.through().y());
        GerberPoint to = BoardGerber.out(arc.to().x(), arc.to().y());
        Optional<CircularArc> circle = CircularArc.through(from.x(), from.y(), through.x(), through.y(), to.x(), to.y());
        if (circle.isEmpty()) {
            composer.line(pen, List.of(from, to));
            return;
        }
        GerberPoint centre = new GerberPoint(circle.get().centerX(), circle.get().centerY());
        // Clockwise when the three points turn right as Gerber sees them.
        double winding = (through.x() - from.x()) * (to.y() - through.y()) - (through.y() - from.y()) * (to.x() - through.x());
        composer.arc(pen, from, to, centre, winding < 0);
    }

    /** Which way round a centre a pen goes from one point to another: negative clockwise, as Gerber's Y runs up. */
    private static double turn(GerberPoint from, GerberPoint centre, GerberPoint to) {
        return (from.x() - centre.x()) * (to.y() - centre.y()) - (from.y() - centre.y()) * (to.x() - centre.x());
    }
}
