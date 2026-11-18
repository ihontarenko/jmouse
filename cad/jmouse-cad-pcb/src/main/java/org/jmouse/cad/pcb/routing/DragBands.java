package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.jmouse.geometry.Areas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The lines a drag makes bands of: every line with an end on a moved pad — the part pulls it —, and every line the moved parts'
 * copper comes up against on its way — the part pushes it. A line held where it stands, one the editor moved itself, and a track
 * standing wholly on the part are no band.
 */
final class DragBands {

    private DragBands() {
    }

    static List<Band> of(RoutingWorld before, PcbDrawing carried, DragMotion motion, List<RigidCopper.MovedPad> pads, Set<Integer> whole) {
        PcbDrawing was = before.pcb();
        Map<String, Geometry> swept = new HashMap<>();
        Envelope region = motion.region(pads.stream().map(RigidCopper.MovedPad::pad).toList(), before.settings().banding().reach());
        Set<Integer> taken = new HashSet<>(whole);
        List<Band> bands = new ArrayList<>();
        for (int index = 0; index < was.tracks().size(); index++) {
            if (taken.contains(index)) {
                continue;
            }
            JoinedTracks.JoinedLine line = JoinedTracks.joinedLine(before, index);
            List<Integer> members = line.members().stream().map(JoinedTracks.Member::index).toList();
            taken.addAll(members);
            boolean fixed = members.stream().anyMatch(member -> !was.tracks().get(member).equals(carried.tracks().get(member))
                                                                || RoutingEngine.held(was, member));
            if (fixed) {
                continue;
            }
            Track track = was.tracks().get(index);
            Optional<String> net = before.netOfTrack(index);
            double width = members.stream().mapToDouble(member -> was.tracks().get(member).width()).max().orElse(track.width());
            BandEnd start = endAt(pads, motion, track.layer(), line.points().getFirst(), net);
            BandEnd end = endAt(pads, motion, track.layer(), line.points().getLast(), net);
            boolean pulled = start instanceof BandEnd.OnMovedPad || end instanceof BandEnd.OnMovedPad;
            Geometry sweep = swept.computeIfAbsent(track.layer(), layer -> sweptOn(layer, pads, motion));
            double reach = width / 2 + before.clearanceOf(net) + widestClearance(before, pads);
            if (pulled || !sweep.isEmpty() && sweep.isWithinDistance(Plane.FACTORY.createLineString(
                    Plane.coordinatesOf(line.points()).toArray(Coordinate[]::new)), reach)) {
                bands.add(cutBy(region, line, track.layer(), width, net, start, end));
            }
        }
        return bands;
    }

    /**
     * A band of a line, the stretch of it the region the drag works in reaches its own: the corners round it hold the band
     * ({@link RegionCut}); where the line's own end does, it keeps what holds it.
     */
    private static Band cutBy(Envelope region, JoinedTracks.JoinedLine line, String layer, double width, Optional<String> net, BandEnd start,
                              BandEnd end) {
        return Band.of(line, layer, width, net, RegionCut.of(Polylines.cornersOf(line.points()), region, start, end));
    }

    private static BandEnd endAt(List<RigidCopper.MovedPad> pads, DragMotion motion, String layer, Point point, Optional<String> net) {
        return RigidCopper.padUnder(pads, motion, layer, point, net)
                .<BandEnd>map(pad -> new BandEnd.OnMovedPad(point, pad.pad().designator()))
                .orElseGet(() -> new BandEnd.Held(point));
    }

    /**
     * Where the moved parts' pads on a layer pass on their way: each pad's hull from where it stood to where it stands, over the
     * part of the way it has copper on the layer — a part put on the other side leaves the layer at once.
     */
    private static Geometry sweptOn(String layer, List<RigidCopper.MovedPad> pads, DragMotion motion) {
        List<Geometry> hulls = new ArrayList<>();
        for (RigidCopper.MovedPad moved : pads) {
            String designator = moved.pad().designator();
            Geometry shape = moved.pad().shape();
            List<Geometry> passed = new ArrayList<>();
            if (moved.pad().on(layer)) {
                passed.add(shape);
            }
            if (moved.pad().pad().hasCopperOn(layer, motion.to(designator))) {
                passed.add(Plane.carried(shape, motion.from(designator), motion.at(designator, 0.5)));
                passed.add(Plane.carried(shape, motion.from(designator), motion.to(designator)));
            }
            if (!passed.isEmpty()) {
                hulls.add(Areas.unionOf(passed).convexHull());
            }
        }
        return hulls.isEmpty() ? Plane.FACTORY.createPolygon() : Areas.unionOf(hulls);
    }

    /** The widest gap any moved pad's net keeps. */
    private static double widestClearance(RoutingWorld before, List<RigidCopper.MovedPad> pads) {
        return pads.stream().mapToDouble(pad -> before.clearanceOf(pad.net())).max().orElse(0);
    }
}
