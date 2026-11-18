package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Arc;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.geometry.index.Box;
import org.jmouse.geometry.index.BoxIndex;
import org.jmouse.geometry.index.BoxTree;

import org.jmouse.core.graph.DisjointSets;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The nodes of a board's copper ({@link CopperNode}): every pad, every via, and every place tracks and arcs end — what meets there
 * says what it is. Ends are one place where the copper says so: a board read from KiCad has a track's end a few hundredths of a
 * millimetre off its neighbour's, inside both tracks' copper, and that is one joint; an arc's end meets what its copper touches.
 * Made once for a board as it stands, and asked: the node at a point, at a track's end, the nearest one.
 */
public final class CopperNodes {

    /** A track's or an arc's end, before it is known what it meets. */
    private record Loose(OptionalInt track, CopperNode.End end, OptionalInt arc, Point at, String layer, double width) {

        /** How near another end stands to be one place with this one: inside the narrower track, or touching an arc's copper. */
        double reachTo(Loose other) {
            return arc.isPresent() || other.arc.isPresent() ? (width + other.width) / 2 : Math.min(width, other.width) / 2;
        }
    }

    private final List<CopperNode>                         nodes;
    private final Map<CopperNode.TrackEnd, CopperNode>     byTrackEnd;
    private final Map<CopperNode, CopperShape>             padCopper;
    private final BoxIndex                                 index;

    private CopperNodes(List<CopperNode> nodes, Map<CopperNode.TrackEnd, CopperNode> byTrackEnd, Map<CopperNode, CopperShape> padCopper) {
        this.nodes = List.copyOf(nodes);
        this.byTrackEnd = Map.copyOf(byTrackEnd);
        this.padCopper = padCopper;
        for (CopperNode node : this.nodes) {
        }
        this.index = BoxTree.of(this.nodes.stream().map(this::boxOf).toList());
    }

    /** The nodes of a board as it stands. */
    public static CopperNodes of(RoutingWorld world) {
        return new Building(world).built();
    }

    /** Every node, pads first, then vias, then where lines end. */
    public List<CopperNode> all() {
        return nodes;
    }

    /** The node a track's end is at: every end is at one — a free one at its own tip. */
    public CopperNode atEnd(int track, CopperNode.End end) {
        return byTrackEnd.get(new CopperNode.TrackEnd(track, end));
    }

    /** The node on a layer a point stands at — on a pad's copper, within the rest's reach —: the nearest, where several are. */
    public Optional<CopperNode> at(String layer, Point point) {
        return near(layer, point, 0, node -> true).filter(node -> holds(node, point));
    }

    /** The nearest node on a layer a test lets through, within a distance of a point — past its own reach —; empty where none is. */
    public Optional<CopperNode> near(String layer, Point point, double within, Predicate<CopperNode> wanted) {
        return Arrays.stream(index.near(Box.around(point.x(), point.y(), within))).mapToObj(nodes::get)
                .filter(node -> node.on(layer) && wanted.test(node))
                .filter(node -> holds(node, point) || node.at().distanceTo(point) <= node.reach() + within)
                .min(Comparator.comparingDouble((CopperNode node) -> holds(node, point) ? 0 : 1)
                        .thenComparingDouble(node -> node.at().distanceTo(point)));
    }

    /** The box a node takes: a pad's copper's, a square of the rest's reach. */
    public Box boxOf(CopperNode node) {
        CopperShape copper = padCopper.get(node);
        return copper != null ? copper.box() : Box.around(node.at().x(), node.at().y(), node.reach());
    }

    private boolean holds(CopperNode node, Point point) {
        CopperShape copper = padCopper.get(node);
        return copper != null ? copper.covers(point.x(), point.y()) : node.holds(point);
    }


    /** The nodes worked out of a board: pads and vias take the ends on them, the rest are grouped where they stand together. */
    private static final class Building {

        private final RoutingWorld                         world;
        private final PcbDrawing                           pcb;
        private final List<CopperNode>                     nodes = new ArrayList<>();
        private final Map<CopperNode.TrackEnd, CopperNode> byTrackEnd = new HashMap<>();
        private final Map<CopperNode, CopperShape>         padCopper = new IdentityHashMap<>();
        private final Map<Integer, CopperShape>            trackCopper = new HashMap<>();

        Building(RoutingWorld world) {
            this.world = world;
            this.pcb = world.pcb();
        }

        CopperNodes built() {
            List<Loose> loose = looseEnds();
            loose = padsTaking(loose);
            loose = viasTaking(loose);
            meetingsOf(loose);
            return new CopperNodes(nodes, byTrackEnd, padCopper);
        }

        private List<Loose> looseEnds() {
            List<Loose> ends = new ArrayList<>();
            for (int index = 0; index < pcb.tracks().size(); index++) {
                Track track = pcb.tracks().get(index);
                ends.add(new Loose(OptionalInt.of(index), CopperNode.End.FIRST, OptionalInt.empty(), track.points().getFirst(), track.layer(), track.width()));
                ends.add(new Loose(OptionalInt.of(index), CopperNode.End.LAST, OptionalInt.empty(), track.points().getLast(), track.layer(), track.width()));
            }
            for (int index = 0; index < pcb.arcs().size(); index++) {
                Arc arc = pcb.arcs().get(index);
                ends.add(new Loose(OptionalInt.empty(), CopperNode.End.FIRST, OptionalInt.of(index), arc.from(), arc.layer(), arc.width()));
                ends.add(new Loose(OptionalInt.empty(), CopperNode.End.LAST, OptionalInt.of(index), arc.to(), arc.layer(), arc.width()));
            }
            return ends;
        }

        /** Every pad a node, with the ends standing on its copper; the ends left. */
        private List<Loose> padsTaking(List<Loose> loose) {
            List<CopperNets.PlacedPad> pads = world.nets().pads();
            List<String> copperLayers = pcb.copperLayerIds();
            Map<Integer, List<Loose>> taken = new LinkedHashMap<>();
            List<Loose> left = new ArrayList<>();
            BoxIndex padIndex = BoxTree.of(pads.stream().map(pad -> pad.copper().box()).toList());
            for (Loose end : loose) {
                Optional<Integer> pad = Arrays.stream(padIndex.near(Box.point(end.at().x(), end.at().y()))).boxed()
                        .filter(index -> pads.get(index).on(end.layer()) && pads.get(index).copper().covers(end.at().x(), end.at().y()))
                        .findFirst();
                pad.ifPresentOrElse(index -> taken.computeIfAbsent(index, ignored -> new ArrayList<>()).add(end), () -> left.add(end));
            }
            for (int index = 0; index < pads.size(); index++) {
                CopperNets.PlacedPad pad = pads.get(index);
                Box bounds = pad.copper().box();
                Set<String> layers = new HashSet<>();
                copperLayers.stream().filter(pad::on).forEach(layers::add);
                CopperNode node = node(CopperNodeKind.PAD, pad.centre(), Math.min(bounds.width(), bounds.height()) / 2, layers,
                        OptionalInt.of(index), taken.getOrDefault(index, List.of()), List.of());
                padCopper.put(node, pad.copper());
            }
            return left;
        }

        /** Every via a node, with the ends within it on any layer it joins; the ends left. */
        private List<Loose> viasTaking(List<Loose> loose) {
            List<String> copperLayers = pcb.copperLayerIds();
            // The ends indexed by where they stand: a via asks those within its own box, never every end of the pcb; an end one via
            // took is taken from the next.
            BoxIndex endIndex = BoxTree.of(loose.stream().map(end -> Box.point(end.at().x(), end.at().y())).toList());
            boolean[] taken = new boolean[loose.size()];
            for (int index = 0; index < pcb.vias().size(); index++) {
                Via via = pcb.vias().get(index);
                Point middle = new Point(via.x(), via.y());
                double radius = via.diameter() / 2;
                Set<String> layers = Set.copyOf(via.layersOf(copperLayers));
                int[] positions = endIndex.near(Box.around(via.x(), via.y(), radius));
                List<Loose> within = new ArrayList<>();
                for (int position : positions) {
                    Loose end = loose.get(position);
                    if (!taken[position] && layers.contains(end.layer()) && end.at().distanceTo(middle) <= radius) {
                        taken[position] = true;
                        within.add(end);
                    }
                }
                node(CopperNodeKind.VIA, middle, radius, layers, OptionalInt.of(index), within, List.of());
            }
            List<Loose> left = new ArrayList<>();
            for (int position = 0; position < loose.size(); position++) {
                if (!taken[position]) {
                    left.add(loose.get(position));
                }
            }
            return left;
        }

        /** The ends left grouped where they stand together, layer by layer, each group a node of what meets there. */
        private void meetingsOf(List<Loose> loose) {
            BoxIndex endIndex = BoxTree.of(loose.stream().map(end -> Box.point(end.at().x(), end.at().y())).toList());
            double widest = loose.stream().mapToDouble(Loose::width).max().orElse(0);
            DisjointSets meetingAt = new DisjointSets(loose.size());
            for (int position = 0; position < loose.size(); position++) {
                Loose end = loose.get(position);
                for (int other : endIndex.near(Box.around(end.at().x(), end.at().y(), widest))) {
                    Loose candidate = loose.get(other);
                    if (other != position && candidate.layer().equals(end.layer())
                        && candidate.at().distanceTo(end.at()) <= end.reachTo(candidate)) {
                        meetingAt.join(other, position);
                    }
                }
            }
            Map<Integer, List<Loose>> groups = new LinkedHashMap<>();
            for (int position = 0; position < loose.size(); position++) {
                groups.computeIfAbsent(meetingAt.rootOf(position), ignored -> new ArrayList<>()).add(loose.get(position));
            }
            BoxIndex trackIndex = trackIndex();
            groups.values().forEach(meeting -> meetingNode(meeting, trackIndex));
        }


        private void meetingNode(List<Loose> meeting, BoxIndex trackIndex) {
            Loose first = meeting.getFirst();
            Set<Integer> ending = new HashSet<>();
            meeting.forEach(end -> end.track().ifPresent(ending::add));
            List<Integer> through = throughAt(first.at(), first.layer(), ending, trackIndex);
            long tracks = meeting.stream().filter(end -> end.track().isPresent()).count();
            long arcs = meeting.size() - tracks;
            CopperNodeKind kind;
            if (!through.isEmpty() || tracks + arcs >= 3) {
                kind = CopperNodeKind.JUNCTION;
            } else if (arcs > 0) {
                kind = CopperNodeKind.ARC_END;
            } else if (tracks == 2) {
                kind = CopperNodeKind.JOINT;
            } else {
                kind = CopperNodeKind.TRACK_TIP;
            }
            double reach = meeting.stream().mapToDouble(Loose::width).min().orElse(first.width()) / 2;
            OptionalInt item = kind == CopperNodeKind.TRACK_TIP ? first.track() : OptionalInt.empty();
            node(kind, first.at(), reach, Set.of(first.layer()), item, meeting, through);
        }

        /** The tracks on a layer whose copper a point stands on, away from their ends: those it is a T on. */
        private List<Integer> throughAt(Point point, String layer, Set<Integer> ending, BoxIndex trackIndex) {
            return Arrays.stream(trackIndex.near(Box.point(point.x(), point.y()))).boxed()
                    .filter(index -> !ending.contains(index) && pcb.tracks().get(index).layer().equals(layer))
                    .filter(index -> trackCopper.computeIfAbsent(index, ignored -> CopperShapes.track(pcb.tracks().get(index), world.board().checks().curveSteps()))
                            .covers(point.x(), point.y()))
                    .toList();
        }

        private BoxIndex trackIndex() {
            return BoxTree.of(pcb.tracks().stream().map(Building::boxOfTrack).toList());
        }

        /** The box a track's copper takes: its points', grown half its width. */
        private static Box boxOfTrack(Track track) {
            Box points = track.points().stream().map(point -> Box.point(point.x(), point.y())).reduce(Box::joined).orElse(Box.point(0, 0));
            return points.grown(track.width() / 2);
        }

        private CopperNode node(CopperNodeKind kind, Point at, double reach, Set<String> layers, OptionalInt item, List<Loose> meeting,
                                List<Integer> through) {
            List<CopperNode.TrackEnd> ends = meeting.stream().filter(end -> end.track().isPresent())
                    .map(end -> new CopperNode.TrackEnd(end.track().getAsInt(), end.end())).toList();
            List<Integer> arcs = meeting.stream().filter(end -> end.arc().isPresent()).map(end -> end.arc().getAsInt()).distinct().toList();
            CopperNode node = new CopperNode(kind, at, reach, layers, item, ends, arcs, through);
            nodes.add(node);
            ends.forEach(end -> byTrackEnd.put(end, node));
            return node;
        }
    }
}
