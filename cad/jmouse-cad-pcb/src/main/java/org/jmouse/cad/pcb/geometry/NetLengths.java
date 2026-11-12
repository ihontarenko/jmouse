package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How long each net's copper runs, in millimetres: along its tracks — their corners rounded as they are drawn —, its arcs,
 * its copper drawn as lines, and down its vias' barrels from the one layer each joins to the other. Copper on no net is
 * left out.
 */
public final class NetLengths {

    private NetLengths() {
    }

    /** One net's length: along its copper, how many vias, down their barrels, and the whole. */
    public record Length(double copper, int vias, double barrels) {

        public double total() {
            return copper + barrels;
        }

        Length plusCopper(double more) {
            return new Length(copper + more, vias, barrels);
        }

        Length plusVia(double barrel) {
            return new Length(copper, vias + 1, barrels + barrel);
        }
    }

    /**
     * @param thickness the pcb's thickness, which the barrels are worked out from; 0 while it is not known
     */
    public static Map<String, Length> of(PcbDrawing pcb, CopperNets nets, double thickness) {
        Map<String, Length> lengths = new HashMap<>();
        Length none = new Length(0, 0, 0);
        for (int index = 0; index < pcb.tracks().size(); index++) {
            double length = trackLength(pcb.tracks().get(index));
            nets.trackNets().get(index).ifPresent(net -> lengths.merge(net, none.plusCopper(length), NetLengths::sum));
        }
        for (int index = 0; index < pcb.arcs().size(); index++) {
            PcbDrawing.Arc arc = pcb.arcs().get(index);
            double length = arcLength(arc.from(), arc.through(), arc.to());
            nets.arcNets().get(index).ifPresent(net -> lengths.merge(net, none.plusCopper(length), NetLengths::sum));
        }
        // Copper drawn as lines runs as a track does; a filled area, as a pour, has no length of its own.
        for (int index = 0; index < nets.drawn().size(); index++) {
            CopperShape shape = nets.drawn().get(index).shape();
            if (shape.area()) {
                continue;
            }
            double length = shape.pieces().stream().mapToDouble(piece -> Math.hypot(piece[2] - piece[0], piece[3] - piece[1])).sum();
            nets.drawnNets().get(index).ifPresent(net -> lengths.merge(net, none.plusCopper(length), NetLengths::sum));
        }
        List<String> layers = pcb.copperLayerIds();
        double layerGap = thickness / Math.max(1, layers.size() - 1);
        for (int index = 0; index < pcb.vias().size(); index++) {
            PcbDrawing.Via via = pcb.vias().get(index);
            int from = via.from() == null || via.from().isEmpty() ? 0 : layers.indexOf(via.from());
            int to = via.to() == null || via.to().isEmpty() ? layers.size() - 1 : layers.indexOf(via.to());
            double barrel = layerGap * Math.abs(to - from);
            nets.viaNets().get(index).ifPresent(net -> lengths.merge(net, none.plusVia(barrel), NetLengths::sum));
        }
        return lengths;
    }

    private static Length sum(Length one, Length other) {
        return new Length(one.copper() + other.copper(), one.vias() + other.vias(), one.barrels() + other.barrels());
    }

    /**
     * A track's length along its copper: each rounded corner cuts the two straight ends it replaces, 2·cut, and runs the
     * arc between instead, radius·turn.
     */
    public static double trackLength(PcbDrawing.Track track) {
        List<PcbDrawing.Point> points = track.points();
        double length = 0;
        for (int index = 1; index < points.size(); index++) {
            length += Math.hypot(points.get(index).x() - points.get(index - 1).x(), points.get(index).y() - points.get(index - 1).y());
        }
        if (track.rounding() <= 0) {
            return length;
        }
        for (int index = 1; index < points.size() - 1; index++) {
            PcbDrawing.Point previous = points.get(index - 1);
            PcbDrawing.Point corner = points.get(index);
            PcbDrawing.Point next = points.get(index + 1);
            double inLength = Math.hypot(corner.x() - previous.x(), corner.y() - previous.y());
            double outLength = Math.hypot(next.x() - corner.x(), next.y() - corner.y());
            if (inLength == 0 || outLength == 0) {
                continue;
            }
            double cosine = ((previous.x() - corner.x()) * (next.x() - corner.x()) + (previous.y() - corner.y()) * (next.y() - corner.y()))
                            / (inLength * outLength);
            double between = Math.acos(Math.max(-1, Math.min(1, cosine)));
            if (between > Math.PI - 1e-6 || between < 1e-6) {
                continue;
            }
            double tangent = Math.tan(between / 2);
            double cut = Math.min(track.rounding() / tangent, Math.min(inLength / 2, outLength / 2));
            length += cut * tangent * (Math.PI - between) - 2 * cut;
        }
        return length;
    }

    /** An arc's length through three points: the circle through them, the angle swept from the first by the middle to the last. */
    public static double arcLength(PcbDrawing.Point from, PcbDrawing.Point through, PcbDrawing.Point to) {
        double ax = from.x();
        double ay = from.y();
        double bx = through.x();
        double by = through.y();
        double cx = to.x();
        double cy = to.y();
        double determinant = 2 * (ax * (by - cy) + bx * (cy - ay) + cx * (ay - by));
        if (Math.abs(determinant) < 1e-12) {
            return Math.hypot(cx - ax, cy - ay);
        }
        double ux = ((ax * ax + ay * ay) * (by - cy) + (bx * bx + by * by) * (cy - ay) + (cx * cx + cy * cy) * (ay - by)) / determinant;
        double uy = ((ax * ax + ay * ay) * (cx - bx) + (bx * bx + by * by) * (ax - cx) + (cx * cx + cy * cy) * (bx - ax)) / determinant;
        double radius = Math.hypot(ax - ux, ay - uy);
        double start = Math.atan2(ay - uy, ax - ux);
        double sweep = turn(start, Math.atan2(cy - uy, cx - ux));
        // The middle point decides which way round: outside the counter-clockwise sweep, the arc is the other way.
        double swept = turn(start, Math.atan2(by - uy, bx - ux)) <= sweep ? sweep : 2 * Math.PI - sweep;
        return radius * swept;
    }

    private static double turn(double start, double end) {
        double turned = (end - start) % (2 * Math.PI);
        return turned < 0 ? turned + 2 * Math.PI : turned;
    }
}
