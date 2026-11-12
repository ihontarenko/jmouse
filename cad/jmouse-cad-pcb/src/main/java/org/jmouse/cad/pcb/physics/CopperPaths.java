package org.jmouse.cad.pcb.physics;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;
import org.jmouse.cad.pcb.geometry.NetLengths;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The pcb's copper as an electrical network: what two points of it are apart in ohms, how a current between them spreads
 * through the copper, how warm it makes each piece, and which piece is too thin for it.
 *
 * <p>Every straight piece of a track or of copper drawn as lines, every arc and every via barrel between two layers is a
 * resistor, ρ·length / section, at the ambient temperature; a pad, a poured zone and an area of copper drawn filled are
 * taken as perfect conductors — a pad is short and wide, a zone's spreading resistance is not worked out, and the answer
 * says when one is on the way. Copper joins where it touches, as the nets are joined ({@link CopperNets}): a track ending
 * on a pad, branching off another's middle, crossing a via. The network is solved for one ampere (conjugate gradients over
 * the nodal equations), then scaled to the current asked. How much a piece carries for an allowed rise is IPC-2221's
 * ({@link CopperPhysics}); the copper's thickness is the pcb's way of making.</p>
 */
public final class CopperPaths {

    /**
     * A point asked about: where it stands, on which copper layer — empty for any —, and how far off copper it may stand and
     * still be on it. It is on the one piece of copper nearest it — a pad before a track over it —, never on two, so a
     * generous reach cannot join neighbouring nets; on a zone only where nothing else is.
     */
    public record Probe(double x, double y, Optional<String> layer, double reach) {
    }

    /** What a resistor of the network is of. */
    public enum Thing {
        TRACK, ARC, SHAPE, VIA;

        /** The word an answer says it with: {@code track}. */
        public String word() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * One resistor: a straight piece of a track or of copper drawn as lines, an arc, or a via's barrel between two
     * neighbouring layers.
     *
     * @param index      the track's, arc's, shape's or via's place in the pcb
     * @param layer      the copper layer; a barrel's upper one
     * @param length     millimetres along the current
     * @param width      the copper's width across the current, millimetres: a barrel's is its circumference
     * @param thickness  the copper's thickness, millimetres
     * @param resistance ohms, at the ambient temperature
     * @param capacity   amperes it carries within the allowed rise
     */
    public record Branch(Thing thing, int index, String layer, PcbDrawing.Point from, PcbDrawing.Point to, double length, double width,
                         double thickness, double resistance, double capacity) {
    }

    /**
     * What the current asked about does in one branch.
     *
     * @param current     amperes
     * @param power       watts it warms the pcb with
     * @param rise        kelvin it warms by, by IPC-2221
     * @param neededWidth for a track or an arc too thin: the width that would carry the current
     * @param neededVias  for a via too thin: how many like it, side by side, would
     */
    public record Flow(Branch branch, double current, double power, double rise, boolean overloaded, Optional<Double> neededWidth,
                       Optional<Integer> neededVias) {
    }

    /**
     * The answer between two points.
     *
     * @param resistance ohms, at the ambient temperature
     * @param drop       volts lost between the points at the current
     * @param power      watts the copper turns into heat
     * @param flows      the branches the current flows through, the most loaded — current against capacity — first
     * @param pours      the zones it may pass through, taken as perfect conductors: the true resistance is somewhat higher
     */
    public record Path(double resistance, double current, double temperatureRise, double drop, double power, List<Flow> flows, List<Integer> pours) {

        /** The most loaded branch: where the copper should be strengthened first. */
        public Optional<Flow> weakest() {
            return flows.stream().findFirst();
        }
    }

    /** A path between two points, or why there is none, in words. */
    public sealed interface Answer permits Found, Refused {
    }

    public record Found(Path path) implements Answer {
    }

    public record Refused(String refusal) implements Answer {
    }

    /** A straight piece of copper along one layer: a track's or a drawn line's segment, or an arc's end other copper meets it at. */
    private static final class Piece {

        final Thing thing;
        final int index;
        final String layer;
        final PcbDrawing.Point from;
        final PcbDrawing.Point to;
        final double width;
        final CopperShape shape;
        final int[] ends;
        /** Whether it is a resistor of its own: a track's segment is; an arc's end only marks where the arc is met. */
        final boolean conducts;
        /** Where along it other copper meets it: the share of its length, and the node there. */
        final List<double[]> taps = new ArrayList<>();

        Piece(Thing thing, int index, String layer, PcbDrawing.Point from, PcbDrawing.Point to, double width, CopperShape shape, int[] ends,
              boolean conducts) {
            this.thing = thing;
            this.index = index;
            this.layer = layer;
            this.from = from;
            this.to = to;
            this.width = width;
            this.shape = shape;
            this.ends = ends;
            this.conducts = conducts;
        }
    }

    /** Nodes joined as one conductor: a union-find that grows as nodes are made. */
    private static final class Nodes {

        private final List<Integer> parents = new ArrayList<>();

        int made() {
            parents.add(parents.size());
            return parents.size() - 1;
        }

        int find(int node) {
            int found = node;
            while (parents.get(found) != found) {
                parents.set(found, parents.get(parents.get(found)));
                found = parents.get(found);
            }
            return found;
        }

        void join(int one, int other) {
            parents.set(find(one), find(other));
        }
    }

    /** A resistor between two nodes. */
    private record Resistor(Branch branch, int from, int to) {
    }

    private final PcbDrawing          pcb;
    private final CopperNets          nets;
    private final Map<String, Double> values;
    private final CopperPhysics       physics;
    private final int                 curveSteps;
    private final double              temperatureRise;
    private final Nodes               nodes = new Nodes();
    private final List<Piece>         pieces = new ArrayList<>();
    private final List<Resistor>      resistors = new ArrayList<>();

    private CopperPaths(PcbDrawing pcb, CopperNets nets, Map<String, Double> values, CopperPhysics physics, int curveSteps, double temperatureRise) {
        this.pcb = pcb;
        this.nets = nets;
        this.values = values;
        this.physics = physics;
        this.curveSteps = curveSteps;
        this.temperatureRise = temperatureRise;
    }

    /**
     * The copper between two points: its resistance, the drop and the heat at a current, and, piece by piece, where that
     * current flows and which piece is too thin for it within the allowed rise. Refused — with words — when a point is on no
     * copper, or when no copper joins them.
     *
     * @param values the values the pcb is held to, by rule word: the copper's, the plating's and the pcb's thicknesses
     */
    public static Answer between(PcbDrawing pcb, CopperNets nets, Map<String, Double> values, Probe from, Probe to, double current,
                                 double temperatureRise, CopperPhysics physics, int curveSteps) {
        return new CopperPaths(pcb, nets, values, physics, curveSteps, temperatureRise).solve(from, to, current);
    }

    private Answer solve(Probe from, Probe to, double current) {
        Network network = network(List.of(from, to));
        if (network.probes().get(0).isEmpty()) {
            return new Refused("The first point is on no copper.");
        }
        if (network.probes().get(1).isEmpty()) {
            return new Refused("The second point is on no copper.");
        }
        int sourceRoot = nodes.find(network.probes().get(0).get());
        int sinkRoot = nodes.find(network.probes().get(1).get());
        Map<Integer, List<Integer>> links = new HashMap<>();
        for (Resistor resistor : resistors) {
            int one = nodes.find(resistor.from());
            int other = nodes.find(resistor.to());
            if (one != other) {
                links.computeIfAbsent(one, ignored -> new ArrayList<>()).add(other);
                links.computeIfAbsent(other, ignored -> new ArrayList<>()).add(one);
            }
        }
        Set<Integer> reached = reachedFrom(sourceRoot, links);
        if (!reached.contains(sinkRoot)) {
            return new Refused("No copper joins the two points.");
        }
        List<Integer> pours = new ArrayList<>();
        for (int zone = 0; zone < network.zoneNodes().size(); zone++) {
            if (reached.contains(nodes.find(network.zoneNodes().get(zone)))) {
                pours.add(zone);
            }
        }
        if (sourceRoot == sinkRoot) {
            return new Found(new Path(0, current, temperatureRise, 0, 0, List.of(), pours));
        }
        Map<Integer, Integer> numbers = new LinkedHashMap<>();
        reached.forEach(root -> numbers.put(root, numbers.size()));
        List<Resistor> inside = resistors.stream().filter(resistor -> numbers.containsKey(nodes.find(resistor.from()))
                && nodes.find(resistor.from()) != nodes.find(resistor.to())).toList();
        double[] solved = potentials(numbers.size(), inside, numbers, numbers.get(sourceRoot), numbers.get(sinkRoot));
        double resistance = solved[numbers.get(sourceRoot)];
        List<Flow> flows = new ArrayList<>();
        for (Resistor resistor : inside) {
            Branch branch = resistor.branch();
            double share = Math.abs(solved[numbers.get(nodes.find(resistor.from()))] - solved[numbers.get(nodes.find(resistor.to()))]) / branch.resistance();
            if (share < physics.carrying()) {
                continue;
            }
            double flowing = share * current;
            double section = branch.width() * branch.thickness();
            boolean inner = branch.thing() != Thing.VIA && PcbDrawing.isInnerCopper(branch.layer());
            boolean overloaded = flowing > branch.capacity();
            flows.add(new Flow(branch, flowing, flowing * flowing * branch.resistance(), physics.riseOf(flowing, section, inner), overloaded,
                    overloaded && branch.thing() != Thing.VIA ? Optional.of(physics.sectionFor(flowing, temperatureRise, inner) / branch.thickness()) : Optional.empty(),
                    overloaded && branch.thing() == Thing.VIA ? Optional.of((int) Math.ceil(flowing / branch.capacity())) : Optional.empty()));
        }
        flows.sort(Comparator.comparingDouble((Flow flow) -> flow.current() / flow.branch().capacity()).reversed());
        return new Found(new Path(resistance, current, temperatureRise, current * resistance, current * current * resistance, flows, pours));
    }

    /** The probes among the nodes — empty for one on no copper —, and each zone's node. */
    private record Network(List<Optional<Integer>> probes, List<Integer> zoneNodes) {
    }

    private Network network(List<Probe> probes) {
        double resistivity = physics.resistivityAtAmbient();
        List<String> layers = pcb.copperLayerIds();
        List<Integer> padNodes = new ArrayList<>();
        nets.pads().forEach(pad -> padNodes.add(nodes.made()));
        List<Map<String, Integer>> viaNodes = new ArrayList<>();
        List<CopperShape> viaShapes = new ArrayList<>();
        for (PcbDrawing.Via via : pcb.vias()) {
            Map<String, Integer> reached = new LinkedHashMap<>();
            layers.stream().filter(layer -> pcb.reaches(via, layer)).forEach(layer -> reached.put(layer, nodes.made()));
            viaNodes.add(reached);
            viaShapes.add(CopperShape.circle(via.x(), via.y(), via.diameter() / 2));
        }
        List<Integer> zoneNodes = new ArrayList<>();
        pcb.zones().forEach(zone -> zoneNodes.add(nodes.made()));
        List<Integer> probeNodes = new ArrayList<>();
        probes.forEach(probe -> probeNodes.add(nodes.made()));
        boolean[] probeTouches = new boolean[probes.size()];

        for (int index = 0; index < pcb.tracks().size(); index++) {
            PcbDrawing.Track track = pcb.tracks().get(index);
            int[] corners = new int[track.points().size()];
            for (int corner = 0; corner < corners.length; corner++) {
                corners[corner] = nodes.made();
            }
            for (int segment = 1; segment < track.points().size(); segment++) {
                PcbDrawing.Point from = track.points().get(segment - 1);
                PcbDrawing.Point to = track.points().get(segment);
                pieces.add(new Piece(Thing.TRACK, index, track.layer(), from, to, track.width(), CopperShape.line(List.of(from, to), track.width() / 2),
                        new int[] {corners[segment - 1], corners[segment]}, true));
            }
        }
        for (int index = 0; index < pcb.arcs().size(); index++) {
            PcbDrawing.Arc arc = pcb.arcs().get(index);
            int[] ends = {nodes.made(), nodes.made()};
            List<PcbDrawing.Point> points = List.of(arc.from(), arc.to());
            for (int end = 0; end < 2; end++) {
                PcbDrawing.Point point = points.get(end);
                pieces.add(new Piece(Thing.ARC, index, arc.layer(), point, point, arc.width(), CopperShape.circle(point.x(), point.y(), arc.width() / 2),
                        new int[] {ends[end], ends[end]}, false));
            }
            double length = NetLengths.arcLength(arc.from(), arc.through(), arc.to());
            double thickness = thicknessOn(arc.layer());
            addResistor(new Branch(Thing.ARC, index, arc.layer(), arc.from(), arc.to(), length, arc.width(), thickness,
                    resistivity * length / (arc.width() * thickness),
                    physics.capacityOf(arc.width() * thickness, temperatureRise, PcbDrawing.isInnerCopper(arc.layer()))), ends[0], ends[1]);
        }

        // Copper drawn as a shape: its lines, segment by segment, as a track's are; a filled area, one conductor, as a pour is.
        List<Area> areas = new ArrayList<>();
        for (CopperShapes.Drawn drawn : CopperShapes.drawn(pcb, curveSteps)) {
            if (drawn.shape().area()) {
                areas.add(new Area(nodes.made(), drawn.layer(), drawn.shape()));
                continue;
            }
            double width = drawn.shape().radius() * 2;
            PcbDrawing.Point lastPoint = null;
            int lastNode = -1;
            for (double[] segment : drawn.shape().pieces()) {
                PcbDrawing.Point from = new PcbDrawing.Point(segment[0], segment[1]);
                PcbDrawing.Point to = new PcbDrawing.Point(segment[2], segment[3]);
                int start = lastPoint != null && distance(lastPoint, from) <= CopperNets.JOIN_TOLERANCE ? lastNode : nodes.made();
                int end = nodes.made();
                pieces.add(new Piece(Thing.SHAPE, drawn.index(), drawn.layer(), from, to, width, CopperShape.line(List.of(from, to), width / 2),
                        new int[] {start, end}, true));
                lastPoint = to;
                lastNode = end;
            }
        }

        // Where copper meets copper: pads, vias, other pieces.
        for (int pieceIndex = 0; pieceIndex < pieces.size(); pieceIndex++) {
            Piece piece = pieces.get(pieceIndex);
            for (int pad = 0; pad < nets.pads().size(); pad++) {
                CopperNets.PlacedPad placed = nets.pads().get(pad);
                if (placed.on(piece.layer) && touching(piece.shape, placed.copper())) {
                    nodes.join(nodeAlong(piece, alongNearest(piece.from, piece.to, placed.centre())), padNodes.get(pad));
                }
            }
            for (int via = 0; via < pcb.vias().size(); via++) {
                Integer node = viaNodes.get(via).get(piece.layer);
                if (node != null && touching(piece.shape, viaShapes.get(via))) {
                    PcbDrawing.Via placed = pcb.vias().get(via);
                    nodes.join(nodeAlong(piece, alongNearest(piece.from, piece.to, new PcbDrawing.Point(placed.x(), placed.y()))), node);
                }
            }
            for (int otherIndex = pieceIndex + 1; otherIndex < pieces.size(); otherIndex++) {
                Piece other = pieces.get(otherIndex);
                if (other.layer.equals(piece.layer) && !(other.thing == piece.thing && other.index == piece.index) && touching(piece.shape, other.shape)) {
                    double[] along = nearestAlongBoth(piece, other);
                    nodes.join(nodeAlong(piece, along[0]), nodeAlong(other, along[1]));
                }
            }
        }
        for (int probeIndex = 0; probeIndex < probes.size(); probeIndex++) {
            Probe probe = probes.get(probeIndex);
            PcbDrawing.Point point = new PcbDrawing.Point(probe.x(), probe.y());
            CopperShape at = CopperShape.circle(probe.x(), probe.y(), 0);
            Nearest nearest = new Nearest(probe.reach());
            for (int pad = 0; pad < nets.pads().size(); pad++) {
                CopperNets.PlacedPad placed = nets.pads().get(pad);
                if (probe.layer().isEmpty() || placed.on(probe.layer().get())) {
                    int padNode = padNodes.get(pad);
                    nearest.offer(placed.copper().encloses(probe.x(), probe.y()) ? 0 : at.gapTo(placed.copper()).gap(), () -> padNode);
                }
            }
            for (int via = 0; via < pcb.vias().size(); via++) {
                Map<String, Integer> reached = viaNodes.get(via);
                Integer node = probe.layer().isEmpty() ? reached.values().stream().findFirst().orElse(null) : reached.get(probe.layer().get());
                if (node != null) {
                    PcbDrawing.Via placed = pcb.vias().get(via);
                    nearest.offer(distance(new PcbDrawing.Point(placed.x(), placed.y()), point) - placed.diameter() / 2, () -> node);
                }
            }
            for (Piece piece : pieces) {
                if (probe.layer().isEmpty() || probe.layer().get().equals(piece.layer)) {
                    double along = alongNearest(piece.from, piece.to, point);
                    nearest.offer(distance(pointAlong(piece.from, piece.to, along), point) - piece.width / 2, () -> nodeAlong(piece, along));
                }
            }
            if (nearest.node != null) {
                nodes.join(probeNodes.get(probeIndex), nearest.node.getAsInt());
                probeTouches[probeIndex] = true;
            }
        }

        // A filled area is one conductor with whatever copper touches it on its layer.
        for (Area area : areas) {
            for (int pad = 0; pad < nets.pads().size(); pad++) {
                CopperNets.PlacedPad placed = nets.pads().get(pad);
                if (placed.on(area.layer()) && (touching(area.shape(), placed.copper()) || area.shape().encloses(placed.centre().x(), placed.centre().y()))) {
                    nodes.join(padNodes.get(pad), area.node());
                }
            }
            for (int via = 0; via < pcb.vias().size(); via++) {
                Integer node = viaNodes.get(via).get(area.layer());
                PcbDrawing.Via placed = pcb.vias().get(via);
                if (node != null && (touching(area.shape(), viaShapes.get(via)) || area.shape().encloses(placed.x(), placed.y()))) {
                    nodes.join(node, area.node());
                }
            }
            for (Piece piece : pieces) {
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
                } else if (touching(area.shape(), piece.shape)) {
                    nodes.join(nodeAlong(piece, alongNearest(piece.from, piece.to, piece.shape.gapTo(area.shape()).at())), area.node());
                }
            }
            for (int probeIndex = 0; probeIndex < probes.size(); probeIndex++) {
                Probe probe = probes.get(probeIndex);
                if (!probeTouches[probeIndex] && (probe.layer().isEmpty() || probe.layer().get().equals(area.layer()))
                        && area.shape().encloses(probe.x(), probe.y())) {
                    nodes.join(probeNodes.get(probeIndex), area.node());
                    probeTouches[probeIndex] = true;
                }
            }
        }

        // A zone joins, as it is poured, the pads of its net inside it, and the runs and vias inside it on its net or on none.
        for (int zoneIndex = 0; zoneIndex < pcb.zones().size(); zoneIndex++) {
            PcbDrawing.Zone zone = pcb.zones().get(zoneIndex);
            Optional<CopperShape> outline = CopperShape.ofPath(zone.path(), true, 0, curveSteps);
            if (outline.isEmpty()) {
                continue;
            }
            CopperShape area = outline.get();
            int zoneNode = zoneNodes.get(zoneIndex);
            for (int pad = 0; pad < nets.pads().size(); pad++) {
                CopperNets.PlacedPad placed = nets.pads().get(pad);
                if (nets.padNets().get(pad).equals(Optional.of(zone.net())) && placed.on(zone.layer()) && area.encloses(placed.centre().x(), placed.centre().y())) {
                    nodes.join(padNodes.get(pad), zoneNode);
                }
            }
            for (Piece piece : pieces) {
                Optional<String> net = switch (piece.thing) {
                    case TRACK -> nets.trackNets().get(piece.index);
                    case ARC -> nets.arcNets().get(piece.index);
                    case SHAPE -> Optional.ofNullable(pcb.shapes().get(piece.index).net()).filter(name -> !name.isBlank());
                    case VIA -> Optional.empty();
                };
                if (piece.layer.equals(zone.layer()) && (net.isEmpty() || net.get().equals(zone.net()))) {
                    for (int end = 0; end < 2; end++) {
                        PcbDrawing.Point point = end == 0 ? piece.from : piece.to;
                        if (area.encloses(point.x(), point.y())) {
                            nodes.join(piece.ends[end], zoneNode);
                        }
                    }
                }
            }
            for (int via = 0; via < pcb.vias().size(); via++) {
                Integer node = viaNodes.get(via).get(zone.layer());
                Optional<String> net = nets.viaNets().get(via);
                PcbDrawing.Via placed = pcb.vias().get(via);
                if (node != null && (net.isEmpty() || net.get().equals(zone.net())) && area.encloses(placed.x(), placed.y())) {
                    nodes.join(node, zoneNode);
                }
            }
            for (int probeIndex = 0; probeIndex < probes.size(); probeIndex++) {
                Probe probe = probes.get(probeIndex);
                if (!probeTouches[probeIndex] && (probe.layer().isEmpty() || probe.layer().get().equals(zone.layer())) && area.encloses(probe.x(), probe.y())) {
                    nodes.join(probeNodes.get(probeIndex), zoneNode);
                    probeTouches[probeIndex] = true;
                }
            }
        }

        // Each track segment, cut where other copper meets it, is a chain of resistors.
        for (Piece piece : pieces) {
            if (!piece.conducts) {
                continue;
            }
            double thickness = thicknessOn(piece.layer);
            List<double[]> stops = new ArrayList<>();
            stops.add(new double[] {0, piece.ends[0]});
            piece.taps.stream().sorted(Comparator.comparingDouble(tap -> tap[0])).forEach(stops::add);
            stops.add(new double[] {1, piece.ends[1]});
            for (int stop = 1; stop < stops.size(); stop++) {
                PcbDrawing.Point from = pointAlong(piece.from, piece.to, stops.get(stop - 1)[0]);
                PcbDrawing.Point to = pointAlong(piece.from, piece.to, stops.get(stop)[0]);
                double length = distance(from, to);
                int fromNode = (int) stops.get(stop - 1)[1];
                int toNode = (int) stops.get(stop)[1];
                if (length <= CopperNets.JOIN_TOLERANCE) {
                    nodes.join(fromNode, toNode);
                    continue;
                }
                addResistor(new Branch(piece.thing, piece.index, piece.layer, from, to, length, piece.width, thickness,
                        resistivity * length / (piece.width * thickness),
                        physics.capacityOf(piece.width * thickness, temperatureRise, PcbDrawing.isInnerCopper(piece.layer))), fromNode, toNode);
            }
        }

        // Each via's barrel, from one layer it reaches to the next.
        double layerGap = rule(PcbVocabulary.RULE_PCB_THICKNESS) / Math.max(1, layers.size() - 1);
        double plating = rule(PcbVocabulary.RULE_PLATING_THICKNESS);
        for (int via = 0; via < pcb.vias().size(); via++) {
            PcbDrawing.Via placed = pcb.vias().get(via);
            List<Map.Entry<String, Integer>> reached = new ArrayList<>(viaNodes.get(via).entrySet());
            double circumference = Math.PI * (placed.drill() + plating);
            PcbDrawing.Point at = new PcbDrawing.Point(placed.x(), placed.y());
            for (int layer = 1; layer < reached.size(); layer++) {
                double length = layerGap * (layers.indexOf(reached.get(layer).getKey()) - layers.indexOf(reached.get(layer - 1).getKey()));
                addResistor(new Branch(Thing.VIA, via, reached.get(layer - 1).getKey(), at, at, length, circumference, plating,
                        resistivity * length / (circumference * plating), physics.capacityOf(circumference * plating, temperatureRise, false)),
                        reached.get(layer - 1).getValue(), reached.get(layer).getValue());
            }
        }

        List<Optional<Integer>> probed = new ArrayList<>();
        for (int probeIndex = 0; probeIndex < probes.size(); probeIndex++) {
            probed.add(probeTouches[probeIndex] ? Optional.of(probeNodes.get(probeIndex)) : Optional.empty());
        }
        return new Network(probed, zoneNodes);
    }

    /** A filled area of copper drawn as a shape: one conductor. */
    private record Area(int node, String layer, CopperShape shape) {
    }

    /** The nearest copper a probe may stand on, within its reach. */
    private static final class Nearest {

        private final double reach;
        private double gap = Double.POSITIVE_INFINITY;
        private IntSupplier node;

        Nearest(double reach) {
            this.reach = reach;
        }

        void offer(double offered, IntSupplier offeredNode) {
            if (offered <= reach && (node == null || offered < gap)) {
                gap = Math.max(0, offered);
                node = offeredNode;
            }
        }
    }

    private void addResistor(Branch branch, int from, int to) {
        resistors.add(new Resistor(branch, from, to));
    }

    private double rule(String word) {
        Double value = values.get(word);
        if (value == null) {
            throw new IllegalArgumentException("The pcb's way of making says no " + word + ": its copper cannot be measured.");
        }
        return value;
    }

    /** The copper's thickness on a layer, by the pcb's way of making. */
    private double thicknessOn(String layer) {
        return rule(PcbDrawing.isInnerCopper(layer) ? PcbVocabulary.RULE_INNER_COPPER_THICKNESS : PcbVocabulary.RULE_COPPER_THICKNESS);
    }

    /** The node where other copper meets a piece: an end's when it meets there, a tap's along it otherwise. */
    private int nodeAlong(Piece piece, double along) {
        double length = distance(piece.from, piece.to);
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
        int node = nodes.made();
        piece.taps.add(new double[] {along, node});
        return node;
    }

    /** How far along a piece, one of whose ends is inside an area and the other not, it crosses the area's edge: found by halving. */
    private double edgeAlong(Piece piece, CopperShape area, boolean fromInside) {
        double inside = fromInside ? 0 : 1;
        double outside = fromInside ? 1 : 0;
        for (int halving = 0; halving < physics.edgeHalvings(); halving++) {
            double middle = (inside + outside) / 2;
            PcbDrawing.Point point = pointAlong(piece.from, piece.to, middle);
            if (area.encloses(point.x(), point.y())) {
                inside = middle;
            } else {
                outside = middle;
            }
        }
        return (inside + outside) / 2;
    }

    /**
     * The node potentials for one ampere into {@code source} and out of {@code sink}: conjugate gradients on the nodal
     * equations with the sink held at zero, preconditioned by each node's own conductance.
     */
    private double[] potentials(int count, List<Resistor> inside, Map<Integer, Integer> numbers, int source, int sink) {
        double[] diagonal = new double[count];
        List<List<double[]>> neighbours = new ArrayList<>();
        for (int node = 0; node < count; node++) {
            neighbours.add(new ArrayList<>());
        }
        for (Resistor resistor : inside) {
            int from = numbers.get(nodes.find(resistor.from()));
            int to = numbers.get(nodes.find(resistor.to()));
            double conductance = 1 / resistor.branch().resistance();
            diagonal[from] += conductance;
            diagonal[to] += conductance;
            neighbours.get(from).add(new double[] {to, conductance});
            neighbours.get(to).add(new double[] {from, conductance});
        }
        double[] solution = new double[count];
        double[] residual = new double[count];
        residual[source] = 1;
        double[] preconditioned = preconditioned(residual, diagonal, sink);
        double[] direction = preconditioned.clone();
        double[] product = new double[count];
        double agreement = dot(residual, preconditioned);
        CopperPhysics.Solver solver = physics.solver();
        int most = solver.iterationsPerNode() * count + solver.iterations();
        for (int iteration = 0; iteration < most && Math.sqrt(dot(residual, residual)) > solver.tolerance(); iteration++) {
            for (int node = 0; node < count; node++) {
                if (node == sink) {
                    product[node] = 0;
                    continue;
                }
                double sum = diagonal[node] * direction[node];
                for (double[] neighbour : neighbours.get(node)) {
                    if ((int) neighbour[0] != sink) {
                        sum -= neighbour[1] * direction[(int) neighbour[0]];
                    }
                }
                product[node] = sum;
            }
            double step = agreement / dot(direction, product);
            for (int node = 0; node < count; node++) {
                solution[node] += step * direction[node];
                residual[node] -= step * product[node];
            }
            preconditioned = preconditioned(residual, diagonal, sink);
            double next = dot(residual, preconditioned);
            for (int node = 0; node < count; node++) {
                direction[node] = preconditioned[node] + next / agreement * direction[node];
            }
            agreement = next;
        }
        return solution;
    }

    private static double[] preconditioned(double[] residual, double[] diagonal, int sink) {
        double[] scaled = new double[residual.length];
        for (int node = 0; node < residual.length; node++) {
            scaled[node] = node == sink || diagonal[node] == 0 ? 0 : residual[node] / diagonal[node];
        }
        return scaled;
    }

    private static double dot(double[] first, double[] second) {
        double sum = 0;
        for (int index = 0; index < first.length; index++) {
            sum += first[index] * second[index];
        }
        return sum;
    }

    /** The nodes copper reaches from one, by the branches between roots. */
    private static Set<Integer> reachedFrom(int start, Map<Integer, List<Integer>> links) {
        Set<Integer> reached = new LinkedHashSet<>(List.of(start));
        Deque<Integer> waiting = new ArrayDeque<>(List.of(start));
        while (!waiting.isEmpty()) {
            for (int next : links.getOrDefault(waiting.pop(), List.of())) {
                if (reached.add(next)) {
                    waiting.push(next);
                }
            }
        }
        return reached;
    }

    private static boolean touching(CopperShape first, CopperShape second) {
        return first.within(second, CopperNets.JOIN_TOLERANCE) && first.gapTo(second).gap() <= CopperNets.JOIN_TOLERANCE;
    }

    private static double distance(PcbDrawing.Point from, PcbDrawing.Point to) {
        return Math.hypot(to.x() - from.x(), to.y() - from.y());
    }

    private static PcbDrawing.Point pointAlong(PcbDrawing.Point from, PcbDrawing.Point to, double along) {
        return new PcbDrawing.Point(from.x() + (to.x() - from.x()) * along, from.y() + (to.y() - from.y()) * along);
    }

    /** The share along a piece of the point on it nearest to a point. */
    private static double alongNearest(PcbDrawing.Point from, PcbDrawing.Point to, PcbDrawing.Point point) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double squared = dx * dx + dy * dy;
        if (squared == 0) {
            return 0;
        }
        return Math.min(1, Math.max(0, ((point.x() - from.x()) * dx + (point.y() - from.y()) * dy) / squared));
    }

    /** The shares along two pieces of their nearest points: the closest points of two segments. */
    private static double[] nearestAlongBoth(Piece first, Piece second) {
        double ux = first.to.x() - first.from.x();
        double uy = first.to.y() - first.from.y();
        double vx = second.to.x() - second.from.x();
        double vy = second.to.y() - second.from.y();
        double wx = first.from.x() - second.from.x();
        double wy = first.from.y() - second.from.y();
        double a = ux * ux + uy * uy;
        double b = ux * vx + uy * vy;
        double c = vx * vx + vy * vy;
        double d = ux * wx + uy * wy;
        double e = vx * wx + vy * wy;
        if (a == 0 && c == 0) {
            return new double[] {0, 0};
        }
        if (a == 0) {
            return new double[] {0, clamp(e / c)};
        }
        if (c == 0) {
            return new double[] {clamp(-d / a), 0};
        }
        double denominator = a * c - b * b;
        double s = denominator > 1e-12 ? clamp((b * e - c * d) / denominator) : 0;
        double t = (b * s + e) / c;
        if (t < 0) {
            t = 0;
            s = clamp(-d / a);
        } else if (t > 1) {
            t = 1;
            s = clamp((b - d) / a);
        }
        return new double[] {s, t};
    }

    private static double clamp(double share) {
        return Math.min(1, Math.max(0, share));
    }
}
