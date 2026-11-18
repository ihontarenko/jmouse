package org.jmouse.cad.pcb.physics;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbLayers;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.Arc;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Zone;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;
import org.jmouse.cad.pcb.geometry.NetLengths;
import org.jmouse.core.graph.DisjointSets;
import org.jmouse.geometry.Segments;
import org.jmouse.geometry.index.Box;
import org.jmouse.geometry.index.BoxIndex;
import org.jmouse.geometry.index.BoxTree;
import org.jmouse.numerics.Halving;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The pcb's copper as a network of resistors between nodes, worked out for some probes: every straight piece of a track or of copper
 * drawn as lines, every arc and every via barrel between two layers a resistor; a pad, a poured zone and an area drawn filled one node
 * each — a perfect conductor. Nodes are joined where copper touches, as the nets are; each probe stands on the nearest copper within
 * its reach.
 */
final class CopperNetwork {

    /** A resistor between two nodes. */
    record Resistor(CopperPaths.Branch branch, int from, int to) {
    }

    /** A filled area of copper drawn as a shape: one conductor. */
    private record Area(int node, String layer, CopperShape shape) {
    }

    private final PcbDrawing                pcb;
    private final CopperNets                nets;
    private final Map<String, Double>       values;
    private final CopperPhysics             physics;
    private final int                       curveSteps;
    private final double                    temperatureRise;
    private final double                    resistivity;
    private final List<String>              layers;
    /** Nodes joined as one conductor, growing as nodes are made. */
    private final DisjointSets              nodes = new DisjointSets();
    private final List<NetworkPiece>        pieces = new ArrayList<>();
    private final List<Resistor>            resistors = new ArrayList<>();
    private final List<Integer>             padNodes = new ArrayList<>();
    private final List<Map<String, Integer>> viaNodes = new ArrayList<>();
    private final List<CopperShape>         viaShapes = new ArrayList<>();
    private final List<Integer>             zoneNodes = new ArrayList<>();
    private final List<Integer>             probeNodes = new ArrayList<>();
    private final List<Area>                areas = new ArrayList<>();
    private final List<CopperPaths.Probe>   probes;
    private final boolean[]                 probeTouches;
    private BoxIndex                        padsNear;
    private BoxIndex                        viasNear;
    private BoxIndex                        piecesNear;

    private CopperNetwork(PcbDrawing pcb, CopperNets nets, Map<String, Double> values, CopperPhysics physics, int curveSteps,
                          double temperatureRise, List<CopperPaths.Probe> probes) {
        this.pcb = pcb;
        this.nets = nets;
        this.values = values;
        this.physics = physics;
        this.curveSteps = curveSteps;
        this.temperatureRise = temperatureRise;
        this.resistivity = physics.resistivityAtAmbient();
        this.layers = pcb.copperLayerIds();
        this.probes = probes;
        this.probeTouches = new boolean[probes.size()];
    }

    /** The network of a pcb's copper, its probes stood on the copper nearest them. */
    static CopperNetwork of(PcbDrawing pcb, CopperNets nets, Map<String, Double> values, CopperPhysics physics, int curveSteps,
                            double temperatureRise, List<CopperPaths.Probe> probes) {
        CopperNetwork network = new CopperNetwork(pcb, nets, values, physics, curveSteps, temperatureRise, probes);
        network.conductorNodes();
        network.trackPieces();
        network.arcPieces();
        network.drawnPieces();
        network.indexed();
        network.meetings();
        network.probesStood();
        network.areasJoined();
        network.zonesJoined();
        network.pieceResistors();
        network.barrelResistors();
        return network;
    }

    DisjointSets nodes() {
        return nodes;
    }

    List<Resistor> resistors() {
        return resistors;
    }

    /** Each probe's node; empty for a probe on no copper. */
    List<Optional<Integer>> probeNodes() {
        List<Optional<Integer>> probed = new ArrayList<>();
        for (int probe = 0; probe < probes.size(); probe++) {
            probed.add(probeTouches[probe] ? Optional.of(probeNodes.get(probe)) : Optional.empty());
        }
        return probed;
    }

    /** Each zone's node, by the zone's place. */
    List<Integer> zoneNodes() {
        return zoneNodes;
    }

    /** A node for every pad, every layer a via reaches, every zone and every probe. */
    private void conductorNodes() {
        nets.pads().forEach(pad -> padNodes.add(nodes.add()));
        for (Via via : pcb.vias()) {
            Map<String, Integer> reached = new LinkedHashMap<>();
            layers.stream().filter(layer -> pcb.reaches(via, layer)).forEach(layer -> reached.put(layer, nodes.add()));
            viaNodes.add(reached);
            viaShapes.add(CopperShape.circle(via.x(), via.y(), via.diameter() / 2));
        }
        pcb.zones().forEach(zone -> zoneNodes.add(nodes.add()));
        probes.forEach(probe -> probeNodes.add(nodes.add()));
    }

    /** Each track's segments, its corners the nodes between them. */
    private void trackPieces() {
        for (int index = 0; index < pcb.tracks().size(); index++) {
            Track track = pcb.tracks().get(index);
            int[] corners = new int[track.points().size()];
            for (int corner = 0; corner < corners.length; corner++) {
                corners[corner] = nodes.add();
            }
            for (int segment = 1; segment < track.points().size(); segment++) {
                Point from = track.points().get(segment - 1);
                Point to = track.points().get(segment);
                pieces.add(new NetworkPiece(CopperPaths.Thing.TRACK, index, track.layer(), from, to, track.width(),
                        CopperShape.line(List.of(from, to), track.width() / 2), new int[] {corners[segment - 1], corners[segment]}, true));
            }
        }
    }

    /** Each arc one resistor from end to end, its ends pieces other copper meets it at. */
    private void arcPieces() {
        for (int index = 0; index < pcb.arcs().size(); index++) {
            Arc arc = pcb.arcs().get(index);
            int[] ends = {nodes.add(), nodes.add()};
            List<Point> points = List.of(arc.from(), arc.to());
            for (int end = 0; end < 2; end++) {
                Point point = points.get(end);
                pieces.add(new NetworkPiece(CopperPaths.Thing.ARC, index, arc.layer(), point, point, arc.width(),
                        CopperShape.circle(point.x(), point.y(), arc.width() / 2), new int[] {ends[end], ends[end]}, false));
            }
            double length = NetLengths.arcLength(arc.from(), arc.through(), arc.to());
            resistors.add(new Resistor(branch(CopperPaths.Thing.ARC, index, arc.layer(), arc.from(), arc.to(), length, arc.width(),
                    thicknessOn(arc.layer()), PcbLayers.isInnerCopper(arc.layer())), ends[0], ends[1]));
        }
    }

    /** Copper drawn as a shape: its lines, segment by segment, as a track's are; a filled area, one conductor, as a pour is. */
    private void drawnPieces() {
        for (CopperShapes.Drawn drawn : CopperShapes.drawn(pcb, curveSteps)) {
            if (drawn.shape().area()) {
                areas.add(new Area(nodes.add(), drawn.layer(), drawn.shape()));
                continue;
            }
            double width = drawn.shape().radius() * 2;
            Point lastPoint = null;
            int lastNode = -1;
            for (double[] segment : drawn.shape().pieces()) {
                Point from = new Point(segment[0], segment[1]);
                Point to = new Point(segment[2], segment[3]);
                int start = lastPoint != null && lastPoint.distanceTo(from) <= CopperNets.JOIN_TOLERANCE ? lastNode : nodes.add();
                int end = nodes.add();
                pieces.add(new NetworkPiece(CopperPaths.Thing.SHAPE, drawn.index(), drawn.layer(), from, to, width,
                        CopperShape.line(List.of(from, to), width / 2), new int[] {start, end}, true));
                lastPoint = to;
                lastNode = end;
            }
        }
    }

    /**
     * Pads, vias and pieces by their boxes: what a piece of copper meets is found among what stands near its box — never every pad,
     * via and piece asked of every piece —, in the order the whole lists would give it, so the nodes join as they always did.
     */
    private void indexed() {
        padsNear = BoxTree.of(nets.pads().stream().map(placed -> placed.copper().box()).toList());
        viasNear = BoxTree.of(viaShapes.stream().map(CopperShape::box).toList());
        piecesNear = BoxTree.of(pieces.stream().map(piece -> piece.shape.box()).toList());
    }

    /** Where copper meets copper: a piece meeting pads, vias, other pieces. */
    private void meetings() {
        double touch = CopperNets.JOIN_TOLERANCE;
        for (int pieceIndex = 0; pieceIndex < pieces.size(); pieceIndex++) {
            NetworkPiece piece = pieces.get(pieceIndex);
            for (int pad : padsNear.near(piece.shape.box(), touch)) {
                CopperNets.PlacedPad placed = nets.pads().get(pad);
                if (placed.on(piece.layer) && CopperNets.touching(piece.shape, placed.copper())) {
                    nodes.join(nodeAlong(piece, piece.shareNearest(placed.centre())), padNodes.get(pad));
                }
            }
            for (int via : viasNear.near(piece.shape.box(), touch)) {
                Integer node = viaNodes.get(via).get(piece.layer);
                if (node != null && CopperNets.touching(piece.shape, viaShapes.get(via))) {
                    Via placed = pcb.vias().get(via);
                    nodes.join(nodeAlong(piece, piece.shareNearest(new Point(placed.x(), placed.y()))), node);
                }
            }
            for (int otherIndex : piecesNear.near(piece.shape.box(), touch)) {
                if (otherIndex <= pieceIndex) {
                    continue;
                }
                NetworkPiece other = pieces.get(otherIndex);
                if (other.layer.equals(piece.layer) && !(other.thing == piece.thing && other.index == piece.index)
                    && CopperNets.touching(piece.shape, other.shape)) {
                    Segments.Shares along = piece.nearestShares(other);
                    nodes.join(nodeAlong(piece, along.first()), nodeAlong(other, along.second()));
                }
            }
        }
    }

    /** Each probe stood on the nearest copper within its reach — a pad, a via, a piece —: nothing farther is offered. */
    private void probesStood() {
        for (int probeIndex = 0; probeIndex < probes.size(); probeIndex++) {
            CopperPaths.Probe probe = probes.get(probeIndex);
            Point point = new Point(probe.x(), probe.y());
            CopperShape at = CopperShape.circle(probe.x(), probe.y(), 0);
            NearestCopper nearest = new NearestCopper(probe.reach());
            for (int pad : padsNear.near(at.box(), probe.reach())) {
                CopperNets.PlacedPad placed = nets.pads().get(pad);
                if (probe.layer().isEmpty() || placed.on(probe.layer().get())) {
                    int padNode = padNodes.get(pad);
                    nearest.offer(placed.copper().encloses(probe.x(), probe.y()) ? 0 : at.gapTo(placed.copper()).gap(), () -> padNode);
                }
            }
            for (int via : viasNear.near(at.box(), probe.reach())) {
                Map<String, Integer> reached = viaNodes.get(via);
                Integer node = probe.layer().isEmpty() ? reached.values().stream().findFirst().orElse(null) : reached.get(probe.layer().get());
                if (node != null) {
                    Via placed = pcb.vias().get(via);
                    nearest.offer(new Point(placed.x(), placed.y()).distanceTo(point) - placed.diameter() / 2, () -> node);
                }
            }
            for (int pieceIndex : piecesNear.near(at.box(), probe.reach())) {
                NetworkPiece piece = pieces.get(pieceIndex);
                if (probe.layer().isEmpty() || probe.layer().get().equals(piece.layer)) {
                    double along = piece.shareNearest(point);
                    nearest.offer(piece.at(along).distanceTo(point) - piece.width / 2, () -> nodeAlong(piece, along));
                }
            }
            int stood = probeIndex;
            nearest.node().ifPresent(node -> {
                nodes.join(probeNodes.get(stood), node.getAsInt());
                probeTouches[stood] = true;
            });
        }
    }

    /** A filled area is one conductor with whatever copper touches it on its layer; a probe on no copper yet inside it stands on it. */
    private void areasJoined() {
        double touch = CopperNets.JOIN_TOLERANCE;
        for (Area area : areas) {
            // What an area touches or holds stands within its box.
            Box reach = area.shape().box();
            for (int pad : padsNear.near(reach, touch)) {
                CopperNets.PlacedPad placed = nets.pads().get(pad);
                if (placed.on(area.layer()) && (CopperNets.touching(area.shape(), placed.copper())
                                                || area.shape().encloses(placed.centre().x(), placed.centre().y()))) {
                    nodes.join(padNodes.get(pad), area.node());
                }
            }
            for (int via : viasNear.near(reach, touch)) {
                Integer node = viaNodes.get(via).get(area.layer());
                Via placed = pcb.vias().get(via);
                if (node != null && (CopperNets.touching(area.shape(), viaShapes.get(via)) || area.shape().encloses(placed.x(), placed.y()))) {
                    nodes.join(node, area.node());
                }
            }
            for (int pieceIndex : piecesNear.near(reach, touch)) {
                NetworkPiece piece = pieces.get(pieceIndex);
                if (!piece.layer.equals(area.layer())) {
                    continue;
                }
                boolean fromInside = area.shape().encloses(piece.from.x(), piece.from.y());
                boolean toInside = area.shape().encloses(piece.to.x(), piece.to.y());
                if (fromInside != toInside) {
                    // In at its edge: the stretch inside is the area's copper, and costs nothing.
                    nodes.join(nodeAlong(piece, edgeAlong(piece, area.shape(), fromInside)), area.node());
                } else if (fromInside) {
                    nodes.join(piece.ends[0], area.node());
                } else if (CopperNets.touching(area.shape(), piece.shape)) {
                    nodes.join(nodeAlong(piece, piece.shareNearest(piece.shape.gapTo(area.shape()).at())), area.node());
                }
            }
            standUnplacedProbes(area.layer(), area.shape(), area.node());
        }
    }

    /** A zone joins, as it is poured, the pads of its net inside it, and the runs and vias inside it on its net or on none. */
    private void zonesJoined() {
        for (int zoneIndex = 0; zoneIndex < pcb.zones().size(); zoneIndex++) {
            Zone zone = pcb.zones().get(zoneIndex);
            Optional<CopperShape> outline = CopperShape.ofPath(zone.path(), true, 0, curveSteps);
            if (outline.isEmpty()) {
                continue;
            }
            CopperShape area = outline.get();
            int zoneNode = zoneNodes.get(zoneIndex);
            // What a zone holds stands within its box.
            for (int pad : padsNear.near(area.box(), 0)) {
                CopperNets.PlacedPad placed = nets.pads().get(pad);
                if (nets.padNets().get(pad).equals(Optional.of(zone.net())) && placed.on(zone.layer())
                    && area.encloses(placed.centre().x(), placed.centre().y())) {
                    nodes.join(padNodes.get(pad), zoneNode);
                }
            }
            for (int pieceIndex : piecesNear.near(area.box(), 0)) {
                NetworkPiece piece = pieces.get(pieceIndex);
                Optional<String> net = netOf(piece);
                if (piece.layer.equals(zone.layer()) && (net.isEmpty() || net.get().equals(zone.net()))) {
                    for (int end = 0; end < 2; end++) {
                        Point point = end == 0 ? piece.from : piece.to;
                        if (area.encloses(point.x(), point.y())) {
                            nodes.join(piece.ends[end], zoneNode);
                        }
                    }
                }
            }
            for (int via : viasNear.near(area.box(), 0)) {
                Integer node = viaNodes.get(via).get(zone.layer());
                Optional<String> net = nets.viaNets().get(via);
                Via placed = pcb.vias().get(via);
                if (node != null && (net.isEmpty() || net.get().equals(zone.net())) && area.encloses(placed.x(), placed.y())) {
                    nodes.join(node, zoneNode);
                }
            }
            standUnplacedProbes(zone.layer(), area, zoneNode);
        }
    }

    /** The probes on no copper yet that stand inside a conductor on its layer, stood on it. */
    private void standUnplacedProbes(String layer, CopperShape conductor, int node) {
        for (int probeIndex = 0; probeIndex < probes.size(); probeIndex++) {
            CopperPaths.Probe probe = probes.get(probeIndex);
            if (!probeTouches[probeIndex] && (probe.layer().isEmpty() || probe.layer().get().equals(layer))
                && conductor.encloses(probe.x(), probe.y())) {
                nodes.join(probeNodes.get(probeIndex), node);
                probeTouches[probeIndex] = true;
            }
        }
    }

    /** The net a piece's copper is on; empty for one on none. */
    private Optional<String> netOf(NetworkPiece piece) {
        return switch (piece.thing) {
            case TRACK -> nets.trackNets().get(piece.index);
            case ARC -> nets.arcNets().get(piece.index);
            case SHAPE -> Optional.ofNullable(pcb.shapes().get(piece.index).net()).filter(name -> !name.isBlank());
            case VIA -> Optional.empty();
        };
    }

    /** Each piece that conducts, cut where other copper meets it, a chain of resistors. */
    private void pieceResistors() {
        for (NetworkPiece piece : pieces) {
            if (!piece.conducts) {
                continue;
            }
            double thickness = thicknessOn(piece.layer);
            List<double[]> stops = new ArrayList<>();
            stops.add(new double[] {0, piece.ends[0]});
            piece.taps.stream().sorted(Comparator.comparingDouble(tap -> tap[0])).forEach(stops::add);
            stops.add(new double[] {1, piece.ends[1]});
            for (int stop = 1; stop < stops.size(); stop++) {
                Point from = piece.at(stops.get(stop - 1)[0]);
                Point to = piece.at(stops.get(stop)[0]);
                double length = from.distanceTo(to);
                int fromNode = (int) stops.get(stop - 1)[1];
                int toNode = (int) stops.get(stop)[1];
                if (length <= CopperNets.JOIN_TOLERANCE) {
                    nodes.join(fromNode, toNode);
                    continue;
                }
                resistors.add(new Resistor(branch(piece.thing, piece.index, piece.layer, from, to, length, piece.width, thickness,
                        PcbLayers.isInnerCopper(piece.layer)), fromNode, toNode));
            }
        }
    }

    /** Each via's barrel, from one layer it reaches to the next. */
    private void barrelResistors() {
        double layerGap = rule(PcbVocabulary.RULE_PCB_THICKNESS) / Math.max(1, layers.size() - 1);
        double plating = rule(PcbVocabulary.RULE_PLATING_THICKNESS);
        for (int via = 0; via < pcb.vias().size(); via++) {
            Via placed = pcb.vias().get(via);
            List<Map.Entry<String, Integer>> reached = new ArrayList<>(viaNodes.get(via).entrySet());
            double circumference = Math.PI * (placed.drill() + plating);
            Point at = new Point(placed.x(), placed.y());
            for (int layer = 1; layer < reached.size(); layer++) {
                double length = layerGap * (layers.indexOf(reached.get(layer).getKey()) - layers.indexOf(reached.get(layer - 1).getKey()));
                resistors.add(new Resistor(branch(CopperPaths.Thing.VIA, via, reached.get(layer - 1).getKey(), at, at, length, circumference,
                        plating, false), reached.get(layer - 1).getValue(), reached.get(layer).getValue()));
            }
        }
    }

    /** A branch of copper this long, wide and thick: its resistance at the ambient temperature, and what it carries within the rise. */
    private CopperPaths.Branch branch(CopperPaths.Thing thing, int index, String layer, Point from, Point to, double length, double width,
                                      double thickness, boolean inner) {
        return new CopperPaths.Branch(thing, index, layer, from, to, length, width, thickness, resistivity * length / (width * thickness),
                physics.capacityOf(width * thickness, temperatureRise, inner));
    }

    /** A value the pcb's way of making must say: refused, with words, where it does not. */
    private double rule(String word) {
        Double value = values.get(word);
        if (value == null) {
            throw new IllegalArgumentException("The pcb's way of making says no " + word + ": its copper cannot be measured.");
        }
        return value;
    }

    /** The copper's thickness on a layer, by the pcb's way of making. */
    private double thicknessOn(String layer) {
        return rule(PcbLayers.isInnerCopper(layer) ? PcbVocabulary.RULE_INNER_COPPER_THICKNESS : PcbVocabulary.RULE_COPPER_THICKNESS);
    }

    /** The node where other copper meets a piece: an end's when it meets there, a tap's along it otherwise. */
    private int nodeAlong(NetworkPiece piece, double along) {
        double length = piece.length();
        if (length * along <= CopperNets.JOIN_TOLERANCE) {
            return piece.ends[0];
        }
        if (length * (1 - along) <= CopperNets.JOIN_TOLERANCE) {
            return piece.ends[1];
        }
        for (double[] tap : piece.taps) {
            if (Math.abs(tap[0] - along) * length <= CopperNets.JOIN_TOLERANCE) {
                return (int) tap[1];
            }
        }
        int node = nodes.add();
        piece.taps.add(new double[] {along, node});
        return node;
    }

    /** How far along a piece, one of whose ends is inside an area and the other not, it crosses the area's edge: found by halving. */
    private double edgeAlong(NetworkPiece piece, CopperShape area, boolean fromInside) {
        return Halving.bracket(fromInside ? 0 : 1, fromInside ? 1 : 0, physics.edgeHalvings(), along -> {
            Point point = piece.at(along);
            return area.encloses(point.x(), point.y());
        }).middle();
    }
}
