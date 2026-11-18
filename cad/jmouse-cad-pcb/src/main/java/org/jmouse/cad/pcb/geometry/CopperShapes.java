package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Arc;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Shape;
import org.jmouse.cad.pcb.drawing.Hole;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Each thing of a pcb as the copper it is ({@link CopperShape}): a track along its rounded corners, an arc, a pad where its
 * part stands, a pad's hole, a via, copper drawn as a shape. Told once, for the nets, the checks and whatever else
 * measures copper.
 */
public final class CopperShapes {

    private CopperShapes() {
    }

    /** A track's copper: its centre line, its rounded corners followed along their arcs, grown by half its width. */
    public static CopperShape track(Track track, int curveSteps) {
        List<Point> points = new ArrayList<>();
        for (RoundedTrack.Piece piece : RoundedTrack.of(track)) {
            List<Point> along = switch (piece) {
                case RoundedTrack.Run run -> run.points();
                case RoundedTrack.Bend bend -> CopperNets.arcPoints(new Arc(track.layer(), track.width(), bend.from(),
                        bend.through(), bend.to(), track.arrangement(), track.finish()), curveSteps);
            };
            for (Point point : along) {
                if (points.isEmpty() || !points.getLast().equals(point)) {
                    points.add(point);
                }
            }
        }
        return CopperShape.line(points.isEmpty() ? track.points() : points, track.width() / 2);
    }

    /** An arc track's copper: its centre line along the arc — the straight run where its three points stand on one line. */
    public static CopperShape arc(Arc arc, int curveSteps) {
        return CopperShape.line(CopperNets.arcPoints(arc, curveSteps), arc.width() / 2);
    }

    /** A via's copper: its ring, as wide as its diameter. */
    public static CopperShape via(Via via) {
        return CopperShape.circle(via.x(), via.y(), via.diameter() / 2);
    }

    /** How far a hole of the pcb reaches from its centre: its ring's edge, else its drill's. */
    public static double reach(Hole hole) {
        return Math.max(hole.ring(), hole.drill()) / 2;
    }

    /**
     * A placed pad's copper: a circle; an oval as its long axis grown by half its short side; a rounded rectangle as its
     * inner rectangle grown by its corner; any other shape as its rectangle.
     */
    public static CopperShape pad(Placement placement, FootprintDrawing.Pad pad) {
        double halfWidth = pad.width() / 2;
        double halfHeight = pad.height() / 2;
        if (PcbVocabulary.CIRCLE.equals(pad.shape())) {
            Point centre = onPcb(placement, pad, 0, 0);
            return CopperShape.circle(centre.x(), centre.y(), Math.min(halfWidth, halfHeight));
        }
        if (PcbVocabulary.OVAL.equals(pad.shape())) {
            double radius = Math.min(halfWidth, halfHeight);
            Point from = halfWidth >= halfHeight ? onPcb(placement, pad, -(halfWidth - radius), 0) : onPcb(placement, pad, 0, -(halfHeight - radius));
            Point to = halfWidth >= halfHeight ? onPcb(placement, pad, halfWidth - radius, 0) : onPcb(placement, pad, 0, halfHeight - radius);
            return CopperShape.line(List.of(from, to), radius);
        }
        double corner = PcbVocabulary.ROUNDED_RECTANGLE.equals(pad.shape()) ? Math.min(halfWidth, halfHeight) * 2 * pad.roundness() : 0;
        double innerWidth = halfWidth - corner;
        double innerHeight = halfHeight - corner;
        CopperShape inner = CopperShape.ring(List.of(onPcb(placement, pad, -innerWidth, -innerHeight), onPcb(placement, pad, innerWidth, -innerHeight),
                onPcb(placement, pad, innerWidth, innerHeight), onPcb(placement, pad, -innerWidth, innerHeight)));
        return CopperShape.of(inner.pieces(), true, corner);
    }

    /** A pad's hole where its part stands: a round hole, or a slot as its long axis grown by half its width. */
    public static CopperShape hole(Placement placement, FootprintDrawing.Pad pad) {
        Point centre = placement.placed(pad.x(), pad.y());
        double width = pad.drillWidth();
        double height = pad.drillHeight() > 0 ? pad.drillHeight() : pad.drillWidth();
        if (Math.abs(width - height) < 1e-9) {
            return CopperShape.circle(centre.x(), centre.y(), width / 2);
        }
        double turn = Math.toRadians(-pad.rotation());
        double radius = Math.min(width, height) / 2;
        double along = (Math.max(width, height) - Math.min(width, height)) / 2;
        double dx = width > height ? along : 0;
        double dy = width > height ? 0 : along;
        Point from = placement.placed(pad.x() - dx * Math.cos(turn) + dy * Math.sin(turn), pad.y() - dx * Math.sin(turn) - dy * Math.cos(turn));
        Point to = placement.placed(pad.x() + dx * Math.cos(turn) - dy * Math.sin(turn), pad.y() + dx * Math.sin(turn) + dy * Math.cos(turn));
        return CopperShape.line(List.of(from, to), radius);
    }

    /** A piece of copper the pcb draws as a shape on a copper layer — one of its outlines —, with the net it says, if it says one. */
    public record Drawn(int index, int outline, String layer, String net, CopperShape shape) {
    }

    /** Every piece of copper the pcb draws as a shape — one of several outlines is several pieces —, in one order. */
    public static List<Drawn> drawn(PcbDrawing pcb, int curveSteps) {
        Set<String> layers = Set.copyOf(pcb.copperLayerIds());
        List<Drawn> drawn = new ArrayList<>();
        for (int index = 0; index < pcb.shapes().size(); index++) {
            Shape shape = pcb.shapes().get(index);
            if (!layers.contains(shape.layer())) {
                continue;
            }
            List<String> outlines = PathOutlines.subpaths(shape.path());
            for (int outline = 0; outline < outlines.size(); outline++) {
                int at = index;
                int which = outline;
                CopperShape.ofPath(outlines.get(outline), shape.filled(), shape.filled() ? 0 : shape.width() / 2, curveSteps)
                        .ifPresent(copper -> drawn.add(new Drawn(at, which, shape.layer(), shape.net() == null || shape.net().isBlank() ? null : shape.net(), copper)));
            }
        }
        return drawn;
    }

    /** A point of a pad's own frame on the pcb: turned with the pad, then placed with its part. */
    private static Point onPcb(Placement placement, FootprintDrawing.Pad pad, double x, double y) {
        double turn = Math.toRadians(-pad.rotation());
        return placement.placed(pad.x() + x * Math.cos(turn) - y * Math.sin(turn), pad.y() + x * Math.sin(turn) + y * Math.cos(turn));
    }

}
