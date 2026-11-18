package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Zone;
import org.jmouse.geometry.index.BoxIndex;
import org.jmouse.geometry.index.BoxTree;
import org.jmouse.geometry.index.Box;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * The nets of a pcb's copper worked out: its pads placed, its runs of copper, every two pieces that touch joined — a 0 Ω link's pads
 * through the part, a via in a pad without a track —, the zones poured over what they take, and each group's nets.
 */
final class CopperNetsBuilding {

    private CopperNetsBuilding() {
    }

    /** A run of copper along one layer: its copper, and the points a pour takes it in at. */
    private record Run(String layer, CopperShape shape, List<Point> points) {
    }

    /**
     * @param footprints each seated part's footprint, by designator
     * @param partNets   each part's pads and the net each is on, by designator, then pad number, as the circuit wires them
     * @param outlines   how a pad's outline is drawn as a polygon
     * @param curveSteps how many straight pieces a curve is followed in
     */
    static CopperNets of(PcbDrawing pcb, Map<String, FootprintDrawing> footprints,
                                Map<String, Map<String, String>> partNets, PadOutline outlines, int curveSteps) {
        List<CopperNets.PlacedPad> pads = new ArrayList<>();
        List<Optional<String>> padNets = new ArrayList<>();
        for (Placement placement : pcb.placements()) {
            FootprintDrawing footprint = footprints.get(placement.designator());
            if (footprint == null) {
                continue;
            }
            Map<String, String> nets = partNets.getOrDefault(placement.designator(), Map.of());
            for (FootprintDrawing.Pad pad : footprint.pads()) {
                if (pad.isUnplated()) {
                    continue;
                }
                pads.add(new CopperNets.PlacedPad(placement.designator(), pad, placement, placement.placed(pad.x(), pad.y()),
                        CopperNets.polygon(outlines.placed(placement, pad, 0)), CopperShapes.pad(placement, pad)));
                padNets.add(Optional.ofNullable(nets.get(pad.number())));
            }
        }
        List<CopperShapes.Drawn> drawn = CopperShapes.drawn(pcb, curveSteps);
        List<Run> runs = new ArrayList<>();
        pcb.tracks().forEach(track -> runs.add(new Run(track.layer(), CopperShapes.track(track, curveSteps), track.points())));
        pcb.arcs().forEach(arc -> runs.add(new Run(arc.layer(), CopperShapes.arc(arc, curveSteps), List.of(arc.from(), arc.to()))));
        drawn.forEach(piece -> runs.add(new Run(piece.layer(), piece.shape(), piece.shape().pieces().stream()
                .map(segment -> new Point(segment[0], segment[1])).toList())));
        int trackBase = pads.size();
        int arcBase = trackBase + pcb.tracks().size();
        int drawnBase = arcBase + pcb.arcs().size();
        int viaBase = drawnBase + drawn.size();
        int zoneBase = viaBase + pcb.vias().size();
        CopperNets.Bases bases = new CopperNets.Bases(trackBase, arcBase, drawnBase, viaBase, zoneBase, zoneBase + pcb.zones().size());
        Joining joining = new Joining(bases.length());

        // The pads of a 0 Ω link are one piece of copper, through the part.
        Map<String, Integer> firstOfLink = new HashMap<>();
        Set<String> links = new LinkedHashSet<>();
        pcb.placements().stream().filter(placement -> PcbVocabulary.ROLE_LINK.equals(placement.role())).forEach(placement -> links.add(placement.designator()));
        for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
            String designator = pads.get(padIndex).designator();
            if (links.contains(designator)) {
                Integer first = firstOfLink.putIfAbsent(designator, padIndex);
                if (first != null) {
                    joining.join(padIndex, first);
                }
            }
        }
        List<CopperShape> vias = pcb.vias().stream().map(CopperShapes::via).toList();
        // What a piece may touch is found among the pieces whose boxes come near its own — never every piece of the pcb asked of every
        // other —, in the order the whole list would give it, so the pieces join as they always did.
        BoxIndex padsNear = BoxTree.of(pads.stream().map(pad -> pad.copper().box()).toList());
        BoxIndex runsNear = BoxTree.of(runs.stream().map(run -> run.shape().box()).toList());
        BoxIndex viasNear = BoxTree.of(vias.stream().map(CopperShape::box).toList());
        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            Run run = runs.get(runIndex);
            Box box = run.shape().box();
            for (int padIndex : padsNear.near(box, CopperNets.JOIN_TOLERANCE)) {
                CopperNets.PlacedPad pad = pads.get(padIndex);
                if (pad.on(run.layer()) && CopperNets.touching(run.shape(), pad.copper())) {
                    joining.join(trackBase + runIndex, padIndex);
                }
            }
            for (int otherIndex : runsNear.near(box, CopperNets.JOIN_TOLERANCE)) {
                if (otherIndex <= runIndex) {
                    continue;
                }
                Run other = runs.get(otherIndex);
                if (other.layer().equals(run.layer()) && CopperNets.touching(run.shape(), other.shape())) {
                    joining.join(trackBase + runIndex, trackBase + otherIndex);
                }
            }
            for (int viaIndex : viasNear.near(box, CopperNets.JOIN_TOLERANCE)) {
                if (pcb.reaches(pcb.vias().get(viaIndex), run.layer()) && CopperNets.touching(run.shape(), vias.get(viaIndex))) {
                    joining.join(trackBase + runIndex, viaBase + viaIndex);
                }
            }
        }
        // A via in a pad needs no track to it: their copper is one.
        for (int viaIndex = 0; viaIndex < vias.size(); viaIndex++) {
            Via via = pcb.vias().get(viaIndex);
            for (int padIndex : padsNear.near(vias.get(viaIndex).box(), CopperNets.JOIN_TOLERANCE)) {
                CopperNets.PlacedPad pad = pads.get(padIndex);
                boolean sharesLayer = pcb.copperLayerIds().stream().anyMatch(layer -> pad.on(layer) && pcb.reaches(via, layer));
                if (sharesLayer && CopperNets.touching(vias.get(viaIndex), pad.copper())) {
                    joining.join(viaBase + viaIndex, padIndex);
                }
            }
        }
        pour(pcb, pads, padNets, runs, drawn, bases, joining, curveSteps);

        int[] roots = joining.groups();
        Map<Integer, Set<String>> netsOfGroup = new LinkedHashMap<>();
        for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
            int root = roots[padIndex];
            padNets.get(padIndex).ifPresent(net -> netsOfGroup.computeIfAbsent(root, ignored -> new LinkedHashSet<>()).add(net));
        }
        // A zone names its net, so the piece it pours is on that net even where no pad of it is inside yet.
        for (int zoneIndex = 0; zoneIndex < pcb.zones().size(); zoneIndex++) {
            netsOfGroup.computeIfAbsent(roots[zoneBase + zoneIndex], ignored -> new LinkedHashSet<>()).add(pcb.zones().get(zoneIndex).net());
        }
        // A piece drawn on copper may say its net, as a zone does.
        for (int drawnIndex = 0; drawnIndex < drawn.size(); drawnIndex++) {
            String net = drawn.get(drawnIndex).net();
            if (net != null) {
                netsOfGroup.computeIfAbsent(roots[drawnBase + drawnIndex], ignored -> new LinkedHashSet<>()).add(net);
            }
        }
        List<List<Point>> reachedAt = new ArrayList<>();
        pads.forEach(pad -> reachedAt.add(List.of(pad.centre())));
        pcb.tracks().forEach(track -> reachedAt.add(List.of(track.points().getFirst(), track.points().getLast())));
        pcb.arcs().forEach(arc -> reachedAt.add(List.of(arc.from(), arc.to())));
        drawn.forEach(piece -> reachedAt.add(List.of()));
        pcb.vias().forEach(via -> reachedAt.add(List.of(new Point(via.x(), via.y()))));
        pcb.zones().forEach(zone -> reachedAt.add(List.of()));
        return new CopperNets(pads, padNets, drawn, roots, bases, netsOfGroup, joining, pcb.zones().stream().map(Zone::net).toList(),
                reachedAt);
    }

    /**
     * Each zone joined to what it pours over, in the order they are poured: the higher priority first; of two alike, the one
     * drawn first. A piece a zone takes is on the zone's net from then on, so a later zone of another net leaves it alone.
     */
    private static void pour(PcbDrawing pcb, List<CopperNets.PlacedPad> pads, List<Optional<String>> padNets, List<Run> runs,
                             List<CopperShapes.Drawn> drawn, CopperNets.Bases bases, Joining joining, int curveSteps) {
        if (pcb.zones().isEmpty()) {
            return;
        }
        // The nets each piece already has from its pads and what it says itself, before any zone joins it.
        Map<Integer, Set<String>> netsOfRoot = new HashMap<>();
        for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
            int at = padIndex;
            padNets.get(padIndex).ifPresent(net -> netsOfRoot.computeIfAbsent(joining.groupOf(at), ignored -> new LinkedHashSet<>()).add(net));
        }
        for (int drawnIndex = 0; drawnIndex < drawn.size(); drawnIndex++) {
            String net = drawn.get(drawnIndex).net();
            if (net != null) {
                netsOfRoot.computeIfAbsent(joining.groupOf(bases.drawn() + drawnIndex), ignored -> new LinkedHashSet<>()).add(net);
            }
        }
        List<Integer> order = IntStream.range(0, pcb.zones().size()).boxed()
                .sorted(Comparator.<Integer>comparingInt(index -> -pcb.zones().get(index).priority()).thenComparingInt(index -> index)).toList();
        // What a zone holds stands within its box: the pads, runs and vias there are all it asks, in their own order.
        BoxIndex padsNear = BoxTree.of(pads.stream().map(pad -> pad.copper().box()).toList());
        BoxIndex runsNear = BoxTree.of(runs.stream().map(run -> run.shape().box()).toList());
        BoxIndex viasNear = BoxTree.of(pcb.vias().stream().map(via -> CopperShapes.via(via).box()).toList());
        for (int zoneIndex : order) {
            Zone zone = pcb.zones().get(zoneIndex);
            Optional<CopperShape> area = CopperShape.ofPath(zone.path(), true, 0, curveSteps);
            if (area.isEmpty()) {
                continue;
            }
            int zoneAt = bases.zone() + zoneIndex;
            Box held = area.get().box();
            for (int padIndex : padsNear.near(held, 0)) {
                CopperNets.PlacedPad pad = pads.get(padIndex);
                if (padNets.get(padIndex).filter(zone.net()::equals).isPresent() && pad.on(zone.layer())
                    && area.get().encloses(pad.centre().x(), pad.centre().y())) {
                    pourInto(joining, netsOfRoot, padIndex, zoneAt, zone.net());
                }
            }
            for (int runIndex : runsNear.near(held, 0)) {
                Run run = runs.get(runIndex);
                int at = bases.track() + runIndex;
                if (run.layer().equals(zone.layer()) && takes(joining, netsOfRoot, at, zone.net())
                    && run.points().stream().anyMatch(point -> area.get().encloses(point.x(), point.y()))) {
                    pourInto(joining, netsOfRoot, at, zoneAt, zone.net());
                }
            }
            for (int viaIndex : viasNear.near(held, 0)) {
                Via via = pcb.vias().get(viaIndex);
                int at = bases.via() + viaIndex;
                if (pcb.reaches(via, zone.layer()) && takes(joining, netsOfRoot, at, zone.net()) && area.get().encloses(via.x(), via.y())) {
                    pourInto(joining, netsOfRoot, at, zoneAt, zone.net());
                }
            }
        }
    }

    /** Whether a zone of a net may take a piece: one on no net yet, or on that net alone. */
    private static boolean takes(Joining joining, Map<Integer, Set<String>> netsOfRoot, int index, String net) {
        Set<String> nets = netsOfRoot.get(joining.groupOf(index));
        return nets == null || (nets.size() == 1 && nets.contains(net));
    }

    private static void pourInto(Joining joining, Map<Integer, Set<String>> netsOfRoot, int index, int zoneAt, String net) {
        Set<String> nets = new LinkedHashSet<>(netsOfRoot.getOrDefault(joining.groupOf(index), Set.of()));
        nets.addAll(netsOfRoot.getOrDefault(joining.groupOf(zoneAt), Set.of()));
        nets.add(net);
        joining.join(index, zoneAt);
        netsOfRoot.put(joining.groupOf(index), nets);
    }

}
