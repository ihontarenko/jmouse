package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperNets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The copper a moved part takes with it as it is, not as a band: a track standing wholly on its pads goes whole, and an arc goes
 * with the ends of it that stood on them — wholly on the part, it goes whole; held by one end, it keeps its bulge. A pad no longer
 * on the copper's layer — the part turned over — takes nothing.
 */
final class RigidCopper {

    private RigidCopper() {
    }

    /** One pad of a moved part, as it stood, and its net. */
    record MovedPad(CopperNets.PlacedPad pad, Optional<String> net) {

        /** Whether a point of copper of a net on a layer stood on it. */
        boolean holds(String layer, Point point, Optional<String> copperNet) {
            boolean ofIt = copperNet.isEmpty() || net.isEmpty() || net.equals(copperNet);
            return ofIt && pad.on(layer) && pad.copper().covers(point.x(), point.y());
        }
    }

    /** The pads of the moved parts, as they stood before the drag. */
    static List<MovedPad> movedPads(RoutingWorld before, DragMotion motion) {
        List<CopperNets.PlacedPad> pads = before.nets().pads();
        List<MovedPad> moved = new ArrayList<>();
        for (int index = 0; index < pads.size(); index++) {
            if (motion.moves(pads.get(index).designator())) {
                moved.add(new MovedPad(pads.get(index), before.nets().padNets().get(index)));
            }
        }
        return moved;
    }

    /** The moved pad a point of copper stood on, if it still has copper on the layer where the part now stands. */
    static Optional<MovedPad> padUnder(List<MovedPad> pads, DragMotion motion, String layer, Point point, Optional<String> net) {
        return pads.stream()
                .filter(pad -> pad.holds(layer, point, net))
                .filter(pad -> pad.pad().pad().hasCopperOn(layer, motion.to(pad.pad().designator())))
                .findFirst();
    }

    /** The tracks standing wholly on moved pads, by their places, each with the part it goes with: they go whole. */
    static Map<Integer, String> wholeTracks(RoutingWorld before, PcbDrawing moved, List<MovedPad> pads, DragMotion motion) {
        Map<Integer, String> whole = new LinkedHashMap<>();
        List<PcbDrawing.Track> was = before.pcb().tracks();
        for (int index = 0; index < was.size(); index++) {
            PcbDrawing.Track track = was.get(index);
            Optional<String> net = before.netOfTrack(index);
            if (track.equals(moved.tracks().get(index)) && !moved.drawnGroups().holds(track.arrangement())
                && track.points().stream().allMatch(point -> padUnder(pads, motion, track.layer(), point, net).isPresent())) {
                whole.put(index, padUnder(pads, motion, track.layer(), track.points().getFirst(), net).orElseThrow().pad().designator());
            }
        }
        return whole;
    }

    /** The layout as the editor moved it, with the whole tracks and the arcs its moved parts take with them. */
    static PcbDrawing carried(RoutingWorld before, PcbDrawing moved, List<MovedPad> pads, DragMotion motion) {
        List<PcbDrawing.Track> tracks = new ArrayList<>(moved.tracks());
        for (int index : wholeTracks(before, moved, pads, motion).keySet()) {
            PcbDrawing.Track track = tracks.get(index);
            Optional<String> net = before.netOfTrack(index);
            tracks.set(index, track.withPoints(track.points().stream()
                    .map(point -> carriedPoint(pads, motion, track.layer(), point, net).orElse(point)).toList()));
        }
        return moved.withTracks(tracks).withArcs(carriedArcs(before, moved, pads, motion));
    }

    private static Optional<Point> carriedPoint(List<MovedPad> pads, DragMotion motion, String layer, Point point, Optional<String> net) {
        return padUnder(pads, motion, layer, point, net).map(pad -> Lines.settled(motion.carried(pad.pad().designator(), point, 1)));
    }

    /** The arcs as the moved pads leave them: an end that stood on one goes with it, the bulge half as far as both ends together. */
    private static List<PcbDrawing.Arc> carriedArcs(RoutingWorld before, PcbDrawing moved, List<MovedPad> pads, DragMotion motion) {
        List<PcbDrawing.Arc> was = before.pcb().arcs();
        List<PcbDrawing.Arc> arcs = new ArrayList<>(moved.arcs());
        for (int index = 0; index < arcs.size() && index < was.size(); index++) {
            PcbDrawing.Arc arc = arcs.get(index);
            if (!arc.equals(was.get(index)) || moved.drawnGroups().holds(arc.arrangement())) {
                continue;
            }
            Optional<String> net = before.nets().arcNets().get(index);
            Point from = carriedPoint(pads, motion, arc.layer(), arc.from(), net).orElse(arc.from());
            Point to = carriedPoint(pads, motion, arc.layer(), arc.to(), net).orElse(arc.to());
            if (from.equals(arc.from()) && to.equals(arc.to())) {
                continue;
            }
            double dx = (from.x() - arc.from().x() + to.x() - arc.to().x()) / 2;
            double dy = (from.y() - arc.from().y() + to.y() - arc.to().y()) / 2;
            Point through = Lines.settled(new Point(arc.through().x() + dx, arc.through().y() + dy));
            arcs.set(index, new PcbDrawing.Arc(arc.layer(), arc.width(), from, through, to, arc.arrangement(), arc.finish()));
        }
        return arcs;
    }
}
