package org.jmouse.cad.pcb.geometry;

import org.jmouse.svg.path.node.ArcThrough;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Arc;
import org.jmouse.cad.pcb.drawing.Via;
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
            Arc arc = pcb.arcs().get(index);
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
            Via via = pcb.vias().get(index);
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
    public static double trackLength(Track track) {
        double length = 0;
        // Along its copper as it is drawn: each run straight, each rounded corner along its arc.
        for (RoundedTrack.Piece piece : RoundedTrack.of(track)) {
            switch (piece) {
                case RoundedTrack.Run run -> {
                    for (int index = 1; index < run.points().size(); index++) {
                        length += run.points().get(index - 1).distanceTo(run.points().get(index));
                    }
                }
                case RoundedTrack.Bend bend -> length += arcLength(bend.from(), bend.through(), bend.to());
            }
        }
        return length;
    }

    /** An arc's length through three points: the circle through them, the angle swept from the first by the middle to the last. */
    public static double arcLength(Point from, Point through, Point to) {
        return ArcThrough.of(from.x(), from.y(), through.x(), through.y(), to.x(), to.y()).map(ArcThrough::length).orElse(from.distanceTo(to));
    }
}
