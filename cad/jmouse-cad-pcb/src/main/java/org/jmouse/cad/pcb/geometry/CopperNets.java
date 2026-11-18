package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Arc;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Zone;
import org.jmouse.geometry.index.BoxIndex;
import org.jmouse.geometry.index.BoxTree;

import org.jmouse.geometry.index.Box;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.svg.path.node.CircularArc;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.operation.buffer.BufferParameters;

import java.util.ArrayList;
import java.util.Arrays;
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
 * Which copper of a pcb is on which net, and what is still missing between them. The circuit says which pads share a net;
 * the pcb says what copper joins them:
 * <ul>
 *   <li>a track, an arc or copper drawn as a shape joins every pad its copper touches on a layer the pad has copper on,
 *   and every run it touches on its own layer — a track laid through a pad, a branch off the middle of another —;</li>
 *   <li>a via joins every run that touches it on a layer it reaches, and every pad it stands in;</li>
 *   <li>the two pads of a 0 Ω link are one piece of copper, through the part;</li>
 *   <li>a zone is poured last, as a pcb house pours it, the higher priority first: it joins the pads of its own net inside
 *   it, and the runs and vias inside it that are on its net or on none yet — a stitching via —, while copper of any other
 *   net is cleared around.</li>
 * </ul>
 * A piece of joined copper is on the one net its pads, its zone and its drawn copper say; on none when they say two — a
 * short. What is left between a net's pieces is a connection not laid yet.
 *
 * <p>Copper is measured as {@link CopperShape}s, as the editor measures it, so "touches" is one question for every pair:
 * are they no further apart than {@link #JOIN_TOLERANCE}.</p>
 */
public final class CopperNets {

    /** How far apart two pieces of copper may stand and still be taken as one, in millimetres. */
    public static final double JOIN_TOLERANCE = 0.01;

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    /**
     * A part's pad where its part stands: its outline on the pcb as a polygon — what a pour clears round —, its copper as
     * the checks measure it, and its centre.
     */
    public record PlacedPad(String designator, FootprintDrawing.Pad pad, Placement placement,
                            Point centre, Geometry shape, CopperShape copper) {

        /** Whether it has copper on a layer. */
        public boolean on(String layer) {
            return pad.hasCopperOn(layer, placement);
        }
    }

    /**
     * Copper that joins pads of different nets: the nets it joins, the tracks, arcs and vias of the copper joined, and where the
     * nets meet — for each two of them the chain of copper joining them, not all the copper the short spreads to.
     */
    public record Short(List<String> nets, List<Integer> tracks, List<Integer> arcs, List<Integer> vias, List<Joined> joins) {
    }

    /** What a piece of copper is to the joining: a pad, a track, an arc, a piece drawn, a via, a zone's pour. */
    public enum MemberKind {
        PAD, TRACK, ARC, DRAWN, VIA, ZONE
    }

    /** A piece of copper: its kind, and its place among its kind. */
    public record Member(MemberKind kind, int index) {
    }

    /**
     * Where two nets meet: the copper joining them, from a piece that names the first — a pad, a zone — through each piece that
     * touches the next to one that names the second.
     */
    public record Joined(String firstNet, String secondNet, List<Member> chain) {
    }

    /** A connection not laid yet: two pads of one net that no copper joins, the nearest of their pieces. */
    public record Unlaid(String net, Point from, Point to) {
    }

    /** A run of copper along one layer: its copper, and the points a pour takes it in at. */
    private record Run(String layer, CopperShape shape, List<Point> points) {
    }

    private final List<PlacedPad>            pads;
    private final List<Optional<String>>     padNets;
    private final List<CopperShapes.Drawn>   drawn;
    private final int[]                      roots;
    private final Bases                      bases;
    private final Map<Integer, Set<String>>  netsOfGroup;
    /**
     * Each kind's nets, by its order, worked out once: a piece's net is asked for piece by piece wherever the pcb is walked, and a
     * list made again at every asking made every walk the square of the pcb.
     */
    private final List<Optional<String>>     trackNets;
    private final List<Optional<String>>     arcNets;
    private final List<Optional<String>>     drawnNets;
    private final List<Optional<String>>     viaNets;
    private final Joining                    joining;
    /** The net each zone names, by its place. */
    private final List<String>               zoneNets;
    /** Where each piece of copper may be reached, by its place among all of them: a pad's middle, a track's or an arc's ends, a via's middle. */
    private final List<List<Point>> reachedAt;

    /** Where each kind starts in the one index space the joining runs over. */
    private record Bases(int track, int arc, int drawn, int via, int zone, int length) {
    }

    private CopperNets(List<PlacedPad> pads, List<Optional<String>> padNets, List<CopperShapes.Drawn> drawn, int[] roots, Bases bases,
                       Map<Integer, Set<String>> netsOfGroup, Joining joining, List<String> zoneNets, List<List<Point>> reachedAt) {
        this.reachedAt = List.copyOf(reachedAt);
        this.joining = joining;
        this.zoneNets = List.copyOf(zoneNets);
        this.pads = List.copyOf(pads);
        this.padNets = List.copyOf(padNets);
        this.drawn = List.copyOf(drawn);
        this.roots = roots;
        this.bases = bases;
        this.netsOfGroup = netsOfGroup;
        this.trackNets = netsOf(bases.track(), bases.arc() - bases.track());
        this.arcNets = netsOf(bases.arc(), bases.drawn() - bases.arc());
        this.drawnNets = netsOf(bases.drawn(), bases.via() - bases.drawn());
        this.viaNets = netsOf(bases.via(), bases.zone() - bases.via());
    }

    /**
     * @param footprints each seated part's footprint, by designator
     * @param partNets   each part's pads and the net each is on, by designator, then pad number, as the circuit wires them
     * @param outlines   how a pad's outline is drawn as a polygon
     * @param curveSteps how many straight pieces a curve is followed in
     */
    public static CopperNets of(PcbDrawing pcb, Map<String, FootprintDrawing> footprints,
                                Map<String, Map<String, String>> partNets, PadOutline outlines, int curveSteps) {
        List<PlacedPad> pads = new ArrayList<>();
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
                pads.add(new PlacedPad(placement.designator(), pad, placement, placement.placed(pad.x(), pad.y()),
                        polygon(outlines.placed(placement, pad, 0)), CopperShapes.pad(placement, pad)));
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
        Bases bases = new Bases(trackBase, arcBase, drawnBase, viaBase, zoneBase, zoneBase + pcb.zones().size());
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
            for (int padIndex : padsNear.near(box, JOIN_TOLERANCE)) {
                PlacedPad pad = pads.get(padIndex);
                if (pad.on(run.layer()) && touching(run.shape(), pad.copper())) {
                    joining.join(trackBase + runIndex, padIndex);
                }
            }
            for (int otherIndex : runsNear.near(box, JOIN_TOLERANCE)) {
                if (otherIndex <= runIndex) {
                    continue;
                }
                Run other = runs.get(otherIndex);
                if (other.layer().equals(run.layer()) && touching(run.shape(), other.shape())) {
                    joining.join(trackBase + runIndex, trackBase + otherIndex);
                }
            }
            for (int viaIndex : viasNear.near(box, JOIN_TOLERANCE)) {
                if (pcb.reaches(pcb.vias().get(viaIndex), run.layer()) && touching(run.shape(), vias.get(viaIndex))) {
                    joining.join(trackBase + runIndex, viaBase + viaIndex);
                }
            }
        }
        // A via in a pad needs no track to it: their copper is one.
        for (int viaIndex = 0; viaIndex < vias.size(); viaIndex++) {
            Via via = pcb.vias().get(viaIndex);
            for (int padIndex : padsNear.near(vias.get(viaIndex).box(), JOIN_TOLERANCE)) {
                PlacedPad pad = pads.get(padIndex);
                boolean sharesLayer = pcb.copperLayerIds().stream().anyMatch(layer -> pad.on(layer) && pcb.reaches(via, layer));
                if (sharesLayer && touching(vias.get(viaIndex), pad.copper())) {
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
    private static void pour(PcbDrawing pcb, List<PlacedPad> pads, List<Optional<String>> padNets, List<Run> runs,
                             List<CopperShapes.Drawn> drawn, Bases bases, Joining joining, int curveSteps) {
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
                PlacedPad pad = pads.get(padIndex);
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

    /** Every pad with copper, where its part stands it. */
    public List<PlacedPad> pads() {
        return pads;
    }

    /** Each pad's net as the circuit wires it, in the order of {@link #pads()}; empty for a pad it does not wire. */
    public List<Optional<String>> padNets() {
        return padNets;
    }

    /** Each track's net, by the pcb's order; empty for a track joined to no net, or to two. */
    public List<Optional<String>> trackNets() {
        return trackNets;
    }

    public List<Optional<String>> arcNets() {
        return arcNets;
    }

    /** Each piece of copper drawn as a shape, in the order of {@link #drawn()}. */
    public List<Optional<String>> drawnNets() {
        return drawnNets;
    }

    public List<Optional<String>> viaNets() {
        return viaNets;
    }

    /** The copper the pcb draws as shapes, as its nets are counted. */
    public List<CopperShapes.Drawn> drawn() {
        return drawn;
    }

    /** Which piece of joined copper each pad is in: two of one number are joined. */
    public int padGroup(int pad) {
        return roots[pad];
    }

    public int trackGroup(int track) {
        return roots[bases.track() + track];
    }

    public int arcGroup(int arc) {
        return roots[bases.arc() + arc];
    }

    public int drawnGroup(int drawnPiece) {
        return roots[bases.drawn() + drawnPiece];
    }

    public int viaGroup(int via) {
        return roots[bases.via() + via];
    }

    /** Copper joining nets the circuit keeps apart: each piece that reaches two, with the tracks, arcs and vias in it. */
    public List<Short> shorts() {
        List<Short> shorts = new ArrayList<>();
        netsOfGroup.forEach((root, nets) -> {
            if (nets.size() > 1) {
                shorts.add(new Short(List.copyOf(nets), inGroup(bases.track(), bases.arc() - bases.track(), root),
                        inGroup(bases.arc(), bases.drawn() - bases.arc(), root), inGroup(bases.via(), bases.zone() - bases.via(), root),
                        joining.chainsIn(root, roots, this::netNamedBy).stream()
                                .map(chain -> new Joined(chain.firstNet(), chain.secondNet(), chain.pieces().stream().map(this::memberOf).toList()))
                                .toList()));
            }
        });
        return shorts;
    }

    /**
     * The connections not laid yet: for each net, the shortest lines joining the pieces copper has not joined — a minimum
     * spanning tree over the pieces, each reached where it is nearest: a pad's middle, or the end of a track or an arc, or a via of
     * it. A line cut back from a pad is reached at its tip, where that is nearer than the pad it ran from.
     */
    public List<Unlaid> unlaid() {
        Map<String, Set<Integer>> rootsByNet = new LinkedHashMap<>();
        for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
            int root = roots[padIndex];
            padNets.get(padIndex).ifPresent(net -> rootsByNet.computeIfAbsent(net, ignored -> new LinkedHashSet<>()).add(root));
        }
        Map<Integer, List<Point>> reachedByRoot = new HashMap<>();
        for (int member = 0; member < reachedAt.size(); member++) {
            reachedByRoot.computeIfAbsent(roots[member], ignored -> new ArrayList<>()).addAll(reachedAt.get(member));
        }
        List<Unlaid> lines = new ArrayList<>();
        rootsByNet.forEach((net, pieceRoots) -> {
            List<List<Point>> remaining = new ArrayList<>(pieceRoots.stream().map(reachedByRoot::get).toList());
            if (remaining.size() < 2) {
                return;
            }
            // Grown one piece at a time from the first, by the nearest piece not yet reached (Prim's way): each piece keeps the
            // nearest point it has to what is reached, and only the points a step adds are measured against it — never every
            // point reached against every point left again at every step.
            List<Point> first = remaining.removeFirst();
            int count = remaining.size();
            double[] best = new double[count];
            Point[] from = new Point[count];
            Point[] to = new Point[count];
            boolean[] joined = new boolean[count];
            Arrays.fill(best, Double.POSITIVE_INFINITY);
            List<Point> added = first;
            for (int step = 0; step < count; step++) {
                for (int pieceIndex = 0; pieceIndex < count; pieceIndex++) {
                    if (joined[pieceIndex]) {
                        continue;
                    }
                    for (Point inside : added) {
                        for (Point outside : remaining.get(pieceIndex)) {
                            double distance = distance(inside, outside);
                            if (distance < best[pieceIndex]) {
                                best[pieceIndex] = distance;
                                from[pieceIndex] = inside;
                                to[pieceIndex] = outside;
                            }
                        }
                    }
                }
                int nearest = -1;
                for (int pieceIndex = 0; pieceIndex < count; pieceIndex++) {
                    if (!joined[pieceIndex] && (nearest < 0 || best[pieceIndex] < best[nearest])) {
                        nearest = pieceIndex;
                    }
                }
                joined[nearest] = true;
                lines.add(new Unlaid(net, from[nearest], to[nearest]));
                added = remaining.get(nearest);
            }
        });
        return lines;
    }

    private List<Integer> inGroup(int base, int count, int root) {
        return IntStream.range(0, count).filter(index -> roots[base + index] == root).boxed().toList();
    }

    private List<Optional<String>> netsOf(int base, int count) {
        List<Optional<String>> nets = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Set<String> group = netsOfGroup.getOrDefault(roots[base + index], Set.of());
            nets.add(group.size() == 1 ? Optional.of(group.iterator().next()) : Optional.empty());
        }
        return List.copyOf(nets);
    }

    private static double distance(Point one, Point other) {
        return Math.hypot(one.x() - other.x(), one.y() - other.y());
    }

    /** Whether two pieces of copper touch or overlap: one conductor, wherever along them they meet. */
    public static boolean touching(CopperShape first, CopperShape second) {
        return first.within(second, JOIN_TOLERANCE) && first.comesWithin(second, JOIN_TOLERANCE);
    }

    /** The net a piece names itself, by its place in the joining: a pad's of the circuit, a zone's, a drawn piece's. */
    private Optional<String> netNamedBy(int piece) {
        if (piece < bases.track()) {
            return padNets.get(piece);
        }
        if (piece >= bases.drawn() && piece < bases.via()) {
            return Optional.ofNullable(drawn.get(piece - bases.drawn()).net());
        }
        if (piece >= bases.zone()) {
            return Optional.of(zoneNets.get(piece - bases.zone()));
        }
        return Optional.empty();
    }

    /** A piece by its place in the joining. */
    private Member memberOf(int piece) {
        if (piece < bases.track()) {
            return new Member(MemberKind.PAD, piece);
        }
        if (piece < bases.arc()) {
            return new Member(MemberKind.TRACK, piece - bases.track());
        }
        if (piece < bases.drawn()) {
            return new Member(MemberKind.ARC, piece - bases.arc());
        }
        if (piece < bases.via()) {
            return new Member(MemberKind.DRAWN, piece - bases.drawn());
        }
        if (piece < bases.zone()) {
            return new Member(MemberKind.VIA, piece - bases.via());
        }
        return new Member(MemberKind.ZONE, piece - bases.zone());
    }

    /** A closed outline as a polygon. */
    static Geometry polygon(List<Point> outline) {
        if (outline.size() < 3) {
            return GEOMETRY.createPolygon();
        }
        List<Coordinate> coordinates = new ArrayList<>(outline.stream().map(point -> new Coordinate(point.x(), point.y())).toList());
        coordinates.add(coordinates.getFirst());
        return GEOMETRY.createPolygon(coordinates.toArray(Coordinate[]::new));
    }

    /** A line of copper: its points grown by half its width, round at its ends and its corners. */
    static Geometry line(List<Point> points, double width) {
        Coordinate[] coordinates = points.stream().map(point -> new Coordinate(point.x(), point.y())).toArray(Coordinate[]::new);
        Geometry centre = coordinates.length == 1 ? GEOMETRY.createPoint(coordinates[0]) : GEOMETRY.createLineString(coordinates);
        return centre.buffer(width / 2, BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_ROUND);
    }

    /** The points an arc of the pcb passes, from its start through its middle point to its end. */
    static List<Point> arcPoints(Arc arc, int steps) {
        Point from = arc.from();
        Point through = arc.through();
        Point to = arc.to();
        Optional<CircularArc> circle = CircularArc.through(from.x(), from.y(), through.x(), through.y(), to.x(), to.y());
        if (circle.isEmpty()) {
            return List.of(from, to);
        }
        double centreX = circle.get().centerX();
        double centreY = circle.get().centerY();
        double radius = circle.get().radius();
        double start = Math.atan2(from.y() - centreY, from.x() - centreX);
        double middle = Math.atan2(through.y() - centreY, through.x() - centreX);
        double end = Math.atan2(to.y() - centreY, to.x() - centreX);
        double sweep = normalised(end - start);
        // The way round that passes the middle point: the other way when going the short way would miss it.
        if (normalised(middle - start) > sweep) {
            sweep -= 2 * Math.PI;
        }
        List<Point> points = new ArrayList<>();
        for (int step = 0; step <= steps; step++) {
            double angle = start + sweep * step / steps;
            points.add(new Point(centreX + radius * Math.cos(angle), centreY + radius * Math.sin(angle)));
        }
        return points;
    }

    /** An angle brought into {@code [0, 2π)}. */
    private static double normalised(double angle) {
        double turned = angle % (2 * Math.PI);
        return turned < 0 ? turned + 2 * Math.PI : turned;
    }
}
