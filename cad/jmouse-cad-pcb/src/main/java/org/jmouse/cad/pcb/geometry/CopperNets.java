package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.svg.path.node.CircularArc;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.operation.buffer.BufferParameters;

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
    public record PlacedPad(String designator, FootprintDrawing.Pad pad, PcbDrawing.Placement placement,
                            PcbDrawing.Point centre, Geometry shape, CopperShape copper) {

        /** Whether it has copper on a layer. */
        public boolean on(String layer) {
            return pad.hasCopperOn(layer, placement);
        }
    }

    /** Copper that joins pads of different nets: the nets it joins, and the tracks, arcs and vias that do it. */
    public record Short(List<String> nets, List<Integer> tracks, List<Integer> arcs, List<Integer> vias) {
    }

    /** A connection not laid yet: two pads of one net that no copper joins, the nearest of their pieces. */
    public record Unlaid(String net, PcbDrawing.Point from, PcbDrawing.Point to) {
    }

    /** A run of copper along one layer: its copper, and the points a pour takes it in at. */
    private record Run(String layer, CopperShape shape, List<PcbDrawing.Point> points) {
    }

    private final List<PlacedPad>            pads;
    private final List<Optional<String>>     padNets;
    private final List<CopperShapes.Drawn>   drawn;
    private final int[]                      roots;
    private final Bases                      bases;
    private final Map<Integer, Set<String>>  netsOfGroup;

    /** Where each kind starts in the one index space the joining runs over. */
    private record Bases(int track, int arc, int drawn, int via, int zone, int length) {
    }

    private CopperNets(List<PlacedPad> pads, List<Optional<String>> padNets, List<CopperShapes.Drawn> drawn, int[] roots, Bases bases,
                       Map<Integer, Set<String>> netsOfGroup) {
        this.pads = List.copyOf(pads);
        this.padNets = List.copyOf(padNets);
        this.drawn = List.copyOf(drawn);
        this.roots = roots;
        this.bases = bases;
        this.netsOfGroup = netsOfGroup;
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
        for (PcbDrawing.Placement placement : pcb.placements()) {
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
                .map(segment -> new PcbDrawing.Point(segment[0], segment[1])).toList())));
        int trackBase = pads.size();
        int arcBase = trackBase + pcb.tracks().size();
        int drawnBase = arcBase + pcb.arcs().size();
        int viaBase = drawnBase + drawn.size();
        int zoneBase = viaBase + pcb.vias().size();
        Bases bases = new Bases(trackBase, arcBase, drawnBase, viaBase, zoneBase, zoneBase + pcb.zones().size());
        int[] parents = IntStream.range(0, bases.length()).toArray();

        // The pads of a 0 Ω link are one piece of copper, through the part.
        Map<String, Integer> firstOfLink = new HashMap<>();
        Set<String> links = new LinkedHashSet<>();
        pcb.placements().stream().filter(placement -> PcbVocabulary.ROLE_LINK.equals(placement.role())).forEach(placement -> links.add(placement.designator()));
        for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
            String designator = pads.get(padIndex).designator();
            if (links.contains(designator)) {
                Integer first = firstOfLink.putIfAbsent(designator, padIndex);
                if (first != null) {
                    join(parents, padIndex, first);
                }
            }
        }
        List<CopperShape> vias = pcb.vias().stream().map(CopperShapes::via).toList();
        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            Run run = runs.get(runIndex);
            for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
                PlacedPad pad = pads.get(padIndex);
                if (pad.on(run.layer()) && touching(run.shape(), pad.copper())) {
                    join(parents, trackBase + runIndex, padIndex);
                }
            }
            for (int otherIndex = runIndex + 1; otherIndex < runs.size(); otherIndex++) {
                Run other = runs.get(otherIndex);
                if (other.layer().equals(run.layer()) && touching(run.shape(), other.shape())) {
                    join(parents, trackBase + runIndex, trackBase + otherIndex);
                }
            }
            for (int viaIndex = 0; viaIndex < vias.size(); viaIndex++) {
                if (pcb.reaches(pcb.vias().get(viaIndex), run.layer()) && touching(run.shape(), vias.get(viaIndex))) {
                    join(parents, trackBase + runIndex, viaBase + viaIndex);
                }
            }
        }
        // A via in a pad needs no track to it: their copper is one.
        for (int viaIndex = 0; viaIndex < vias.size(); viaIndex++) {
            PcbDrawing.Via via = pcb.vias().get(viaIndex);
            for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
                PlacedPad pad = pads.get(padIndex);
                boolean sharesLayer = pcb.copperLayerIds().stream().anyMatch(layer -> pad.on(layer) && pcb.reaches(via, layer));
                if (sharesLayer && touching(vias.get(viaIndex), pad.copper())) {
                    join(parents, viaBase + viaIndex, padIndex);
                }
            }
        }
        pour(pcb, pads, padNets, runs, drawn, bases, parents, curveSteps);

        int[] roots = new int[bases.length()];
        for (int index = 0; index < roots.length; index++) {
            roots[index] = find(parents, index);
        }
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
        return new CopperNets(pads, padNets, drawn, roots, bases, netsOfGroup);
    }

    /**
     * Each zone joined to what it pours over, in the order they are poured: the higher priority first; of two alike, the one
     * drawn first. A piece a zone takes is on the zone's net from then on, so a later zone of another net leaves it alone.
     */
    private static void pour(PcbDrawing pcb, List<PlacedPad> pads, List<Optional<String>> padNets, List<Run> runs,
                             List<CopperShapes.Drawn> drawn, Bases bases, int[] parents, int curveSteps) {
        if (pcb.zones().isEmpty()) {
            return;
        }
        // The nets each piece already has from its pads and what it says itself, before any zone joins it.
        Map<Integer, Set<String>> netsOfRoot = new HashMap<>();
        for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
            int at = padIndex;
            padNets.get(padIndex).ifPresent(net -> netsOfRoot.computeIfAbsent(find(parents, at), ignored -> new LinkedHashSet<>()).add(net));
        }
        for (int drawnIndex = 0; drawnIndex < drawn.size(); drawnIndex++) {
            String net = drawn.get(drawnIndex).net();
            if (net != null) {
                netsOfRoot.computeIfAbsent(find(parents, bases.drawn() + drawnIndex), ignored -> new LinkedHashSet<>()).add(net);
            }
        }
        List<Integer> order = IntStream.range(0, pcb.zones().size()).boxed()
                .sorted(Comparator.<Integer>comparingInt(index -> -pcb.zones().get(index).priority()).thenComparingInt(index -> index)).toList();
        for (int zoneIndex : order) {
            PcbDrawing.Zone zone = pcb.zones().get(zoneIndex);
            Optional<CopperShape> area = CopperShape.ofPath(zone.path(), true, 0, curveSteps);
            if (area.isEmpty()) {
                continue;
            }
            int zoneAt = bases.zone() + zoneIndex;
            for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
                PlacedPad pad = pads.get(padIndex);
                if (padNets.get(padIndex).filter(zone.net()::equals).isPresent() && pad.on(zone.layer())
                    && area.get().encloses(pad.centre().x(), pad.centre().y())) {
                    pourInto(parents, netsOfRoot, padIndex, zoneAt, zone.net());
                }
            }
            for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
                Run run = runs.get(runIndex);
                int at = bases.track() + runIndex;
                if (run.layer().equals(zone.layer()) && takes(parents, netsOfRoot, at, zone.net())
                    && run.points().stream().anyMatch(point -> area.get().encloses(point.x(), point.y()))) {
                    pourInto(parents, netsOfRoot, at, zoneAt, zone.net());
                }
            }
            for (int viaIndex = 0; viaIndex < pcb.vias().size(); viaIndex++) {
                PcbDrawing.Via via = pcb.vias().get(viaIndex);
                int at = bases.via() + viaIndex;
                if (pcb.reaches(via, zone.layer()) && takes(parents, netsOfRoot, at, zone.net()) && area.get().encloses(via.x(), via.y())) {
                    pourInto(parents, netsOfRoot, at, zoneAt, zone.net());
                }
            }
        }
    }

    /** Whether a zone of a net may take a piece: one on no net yet, or on that net alone. */
    private static boolean takes(int[] parents, Map<Integer, Set<String>> netsOfRoot, int index, String net) {
        Set<String> nets = netsOfRoot.get(find(parents, index));
        return nets == null || (nets.size() == 1 && nets.contains(net));
    }

    private static void pourInto(int[] parents, Map<Integer, Set<String>> netsOfRoot, int index, int zoneAt, String net) {
        Set<String> nets = new LinkedHashSet<>(netsOfRoot.getOrDefault(find(parents, index), Set.of()));
        nets.addAll(netsOfRoot.getOrDefault(find(parents, zoneAt), Set.of()));
        nets.add(net);
        join(parents, index, zoneAt);
        netsOfRoot.put(find(parents, index), nets);
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
        return netsOf(bases.track(), bases.arc() - bases.track());
    }

    public List<Optional<String>> arcNets() {
        return netsOf(bases.arc(), bases.drawn() - bases.arc());
    }

    /** Each piece of copper drawn as a shape, in the order of {@link #drawn()}. */
    public List<Optional<String>> drawnNets() {
        return netsOf(bases.drawn(), bases.via() - bases.drawn());
    }

    public List<Optional<String>> viaNets() {
        return netsOf(bases.via(), bases.zone() - bases.via());
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
                        inGroup(bases.arc(), bases.drawn() - bases.arc(), root), inGroup(bases.via(), bases.zone() - bases.via(), root)));
            }
        });
        return shorts;
    }

    /**
     * The connections not laid yet: for each net, the shortest lines joining the pieces copper has not joined — a minimum
     * spanning tree over the pieces, each reached through its nearest pad.
     */
    public List<Unlaid> unlaid() {
        Map<String, List<Integer>> byNet = new LinkedHashMap<>();
        for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
            int at = padIndex;
            padNets.get(padIndex).ifPresent(net -> byNet.computeIfAbsent(net, ignored -> new ArrayList<>()).add(at));
        }
        List<Unlaid> lines = new ArrayList<>();
        byNet.forEach((net, members) -> {
            Map<Integer, List<Integer>> pieces = new LinkedHashMap<>();
            members.forEach(member -> pieces.computeIfAbsent(roots[member], ignored -> new ArrayList<>()).add(member));
            List<List<Integer>> remaining = new ArrayList<>(pieces.values());
            if (remaining.size() < 2) {
                return;
            }
            List<Integer> reached = new ArrayList<>(remaining.removeFirst());
            while (!remaining.isEmpty()) {
                double best = Double.POSITIVE_INFINITY;
                int bestPiece = 0;
                int from = 0;
                int to = 0;
                for (int inside : reached) {
                    for (int pieceIndex = 0; pieceIndex < remaining.size(); pieceIndex++) {
                        for (int outside : remaining.get(pieceIndex)) {
                            double distance = distance(pads.get(inside).centre(), pads.get(outside).centre());
                            if (distance < best) {
                                best = distance;
                                bestPiece = pieceIndex;
                                from = inside;
                                to = outside;
                            }
                        }
                    }
                }
                lines.add(new Unlaid(net, pads.get(from).centre(), pads.get(to).centre()));
                reached.addAll(remaining.remove(bestPiece));
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
        return nets;
    }

    private static double distance(PcbDrawing.Point one, PcbDrawing.Point other) {
        return Math.hypot(one.x() - other.x(), one.y() - other.y());
    }

    /** Whether two pieces of copper touch or overlap: one conductor, wherever along them they meet. */
    static boolean touching(CopperShape first, CopperShape second) {
        return first.within(second, JOIN_TOLERANCE) && first.gapTo(second).gap() <= JOIN_TOLERANCE;
    }

    private static int find(int[] parents, int index) {
        int root = index;
        while (parents[root] != root) {
            parents[root] = parents[parents[root]];
            root = parents[root];
        }
        return root;
    }

    private static void join(int[] parents, int one, int other) {
        parents[find(parents, one)] = find(parents, other);
    }

    /** A closed outline as a polygon. */
    static Geometry polygon(List<PcbDrawing.Point> outline) {
        if (outline.size() < 3) {
            return GEOMETRY.createPolygon();
        }
        List<Coordinate> coordinates = new ArrayList<>(outline.stream().map(point -> new Coordinate(point.x(), point.y())).toList());
        coordinates.add(coordinates.getFirst());
        return GEOMETRY.createPolygon(coordinates.toArray(Coordinate[]::new));
    }

    /** A line of copper: its points grown by half its width, round at its ends and its corners. */
    static Geometry line(List<PcbDrawing.Point> points, double width) {
        Coordinate[] coordinates = points.stream().map(point -> new Coordinate(point.x(), point.y())).toArray(Coordinate[]::new);
        Geometry centre = coordinates.length == 1 ? GEOMETRY.createPoint(coordinates[0]) : GEOMETRY.createLineString(coordinates);
        return centre.buffer(width / 2, BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_ROUND);
    }

    /** The points an arc of the pcb passes, from its start through its middle point to its end. */
    static List<PcbDrawing.Point> arcPoints(PcbDrawing.Arc arc, int steps) {
        PcbDrawing.Point from = arc.from();
        PcbDrawing.Point through = arc.through();
        PcbDrawing.Point to = arc.to();
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
        List<PcbDrawing.Point> points = new ArrayList<>();
        for (int step = 0; step <= steps; step++) {
            double angle = start + sweep * step / steps;
            points.add(new PcbDrawing.Point(centreX + radius * Math.cos(angle), centreY + radius * Math.sin(angle)));
        }
        return points;
    }

    /** An angle brought into {@code [0, 2π)}. */
    private static double normalised(double angle) {
        double turned = angle % (2 * Math.PI);
        return turned < 0 ? turned + 2 * Math.PI : turned;
    }
}
