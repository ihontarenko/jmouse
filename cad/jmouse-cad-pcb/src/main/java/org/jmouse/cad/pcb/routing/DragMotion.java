package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.locationtech.jts.geom.Envelope;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The parts a drag moved, from where they stood to where they are let go, as one motion: where each stands any share of the way
 * along — moved straight, turned the shorter way round —, put on the other side at once if it was. Nothing in it is the hand's way
 * there: a drag is worked out from where it began to where it is, every frame, so a hand that wandered leaves nothing behind.
 */
final class DragMotion {

    private final Map<String, PcbDrawing.Placement> from;
    private final Map<String, PcbDrawing.Placement> to;

    private DragMotion(Map<String, PcbDrawing.Placement> from, Map<String, PcbDrawing.Placement> to) {
        this.from = from;
        this.to = to;
    }

    /** No part moving: what joints slide in, the parts where they were let go. */
    static DragMotion still() {
        return new DragMotion(Map.of(), Map.of());
    }

    /** The motion of every part that stands otherwise in {@code moved} than in {@code before}. */
    static DragMotion of(PcbDrawing before, PcbDrawing moved) {
        Map<String, PcbDrawing.Placement> standing = new LinkedHashMap<>();
        before.placements().forEach(placement -> standing.putIfAbsent(placement.designator(), placement));
        Map<String, PcbDrawing.Placement> from = new LinkedHashMap<>();
        Map<String, PcbDrawing.Placement> to = new LinkedHashMap<>();
        for (PcbDrawing.Placement now : moved.placements()) {
            PcbDrawing.Placement was = standing.get(now.designator());
            if (was != null && !was.equals(now) && !to.containsKey(now.designator())) {
                from.put(now.designator(), was);
                to.put(now.designator(), now);
            }
        }
        return new DragMotion(from, to);
    }

    boolean moves(String designator) {
        return to.containsKey(designator);
    }

    /** Where a moved part stood before the drag. */
    PcbDrawing.Placement from(String designator) {
        return from.get(designator);
    }

    /** Where a moved part stands at the end of the drag. */
    PcbDrawing.Placement to(String designator) {
        return to.get(designator);
    }

    /** Where a moved part stands a share of the way along: 0 where it stood, 1 where it is let go. */
    PcbDrawing.Placement at(String designator, double share) {
        PcbDrawing.Placement was = from.get(designator);
        PcbDrawing.Placement now = to.get(designator);
        if (share <= 0) {
            return was;
        }
        if (share >= 1) {
            return now;
        }
        double turn = ((now.rotation() - was.rotation()) % 360 + 540) % 360 - 180;
        return new PcbDrawing.Placement(now.designator(), was.x() + (now.x() - was.x()) * share, was.y() + (now.y() - was.y()) * share,
                was.rotation() + turn * share, now.side(), now.reference(), now.shown(), now.footprint(), now.nets(), now.arrangement(), now.role());
    }

    /** Where a point of the pcb that stood on a moved part — as it stood before the drag — is a share of the way along. */
    Point carried(String designator, Point point, double share) {
        return from.get(designator).carriedTo(at(designator, share), point);
    }

    /** Where a point of the pcb that stands on a moved part as it is let go is a share of the way along. */
    Point brought(String designator, Point point, double share) {
        return to.get(designator).carriedTo(at(designator, share), point);
    }

    /** The region a drag works in: where the moved parts' pads stood and where they are let go, and as far round as the reach. */
    Envelope region(List<CopperNets.PlacedPad> pads, double reach) {
        Envelope region = new Envelope();
        for (CopperNets.PlacedPad pad : pads) {
            if (moves(pad.designator())) {
                region.expandToInclude(pad.shape().getEnvelopeInternal());
                region.expandToInclude(Plane.carried(pad.shape(), from(pad.designator()), to(pad.designator())).getEnvelopeInternal());
            }
        }
        region.expandBy(reach);
        return region;
    }

    /**
     * The farthest any copper of the moved parts goes, at most: its pads' corners, each moved straight and turned round its part's
     * middle. The steps a drag is worked out in are as many as keep each one shorter than what a band keeps clear by.
     */
    double farthest(List<CopperNets.PlacedPad> pads) {
        double farthest = 0;
        for (CopperNets.PlacedPad pad : pads) {
            Optional<PcbDrawing.Placement> was = Optional.ofNullable(from.get(pad.designator()));
            if (was.isEmpty()) {
                continue;
            }
            PcbDrawing.Placement now = to.get(pad.designator());
            double turn = Math.toRadians(Math.abs(((now.rotation() - was.get().rotation()) % 360 + 540) % 360 - 180));
            for (double[] piece : pad.copper().pieces()) {
                for (int end = 0; end < 2; end++) {
                    Point corner = new Point(piece[2 * end], piece[2 * end + 1]);
                    double reach = Math.hypot(corner.x() - was.get().x(), corner.y() - was.get().y()) + pad.copper().radius();
                    farthest = Math.max(farthest, Lines.lengthBetween(corner, was.get().carriedTo(now, corner)) + turn * reach);
                }
            }
        }
        return farthest;
    }
}
