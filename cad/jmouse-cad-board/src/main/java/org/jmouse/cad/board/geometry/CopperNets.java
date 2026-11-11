package org.jmouse.cad.board.geometry;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.svg.path.node.CircularArc;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.operation.buffer.BufferParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Which copper of a board is on which net. The circuit says which pads share a net; the board says what copper joins
 * them: a track or an arc joins every pad its copper touches on a layer the pad has copper on, and every run it touches on
 * its own layer — a track laid through a pad, a branch off the middle of another —; a via joins every run that touches it.
 * A piece of joined copper is on the one net its pads are on; on none when it reaches no pad, or pads of two nets — a short.
 *
 * <p>Every piece of copper is a polygon — a track its line grown by half its width, round at the ends —, so "touches" is
 * one question for every pair: are they no further apart than {@link #JOIN_TOLERANCE}.</p>
 */
public final class CopperNets {

    /** How far apart two pieces of copper may stand and still be taken as one, in millimetres. */
    public static final double JOIN_TOLERANCE = 0.01;

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    /** A part's pad where its part stands: its outline on the board and its centre. */
    public record PlacedPad(String designator, FootprintDrawing.Pad pad, BoardDrawing.Placement placement,
                            BoardDrawing.Point centre, Geometry shape) {
    }

    /** A run of copper along one layer: a track or an arc. */
    private record Run(String layer, Geometry shape) {
    }

    private final List<PlacedPad>        pads;
    private final List<Optional<String>> padNets;
    private final List<Optional<String>> trackNets;
    private final List<Optional<String>> arcNets;
    private final List<Optional<String>> viaNets;

    private CopperNets(List<PlacedPad> pads, List<Optional<String>> padNets, List<Optional<String>> trackNets,
                       List<Optional<String>> arcNets, List<Optional<String>> viaNets) {
        this.pads = List.copyOf(pads);
        this.padNets = List.copyOf(padNets);
        this.trackNets = List.copyOf(trackNets);
        this.arcNets = List.copyOf(arcNets);
        this.viaNets = List.copyOf(viaNets);
    }

    /**
     * @param footprints each seated part's footprint, by designator
     * @param partNets   each part's pads and the net each is on, by designator, then pad number, as the circuit wires them
     * @param outlines   how a pad's outline is drawn
     * @param curveSteps how many straight pieces an arc is drawn with
     */
    public static CopperNets of(BoardDrawing board, Map<String, FootprintDrawing> footprints,
                                Map<String, Map<String, String>> partNets, PadOutline outlines, int curveSteps) {
        List<PlacedPad> pads = new ArrayList<>();
        List<Optional<String>> padNets = new ArrayList<>();
        for (BoardDrawing.Placement placement : board.placements()) {
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
                        polygon(outlines.placed(placement, pad, 0))));
                padNets.add(Optional.ofNullable(nets.get(pad.number())));
            }
        }
        List<Run> runs = new ArrayList<>();
        for (BoardDrawing.Track track : board.tracks()) {
            runs.add(new Run(track.layer(), line(track.points(), track.width())));
        }
        for (BoardDrawing.Arc arc : board.arcs()) {
            runs.add(new Run(arc.layer(), line(arcPoints(arc, curveSteps), arc.width())));
        }
        List<Geometry> vias = board.vias().stream()
                .map(via -> GEOMETRY.createPoint(new Coordinate(via.x(), via.y())).buffer(via.diameter() / 2)).toList();

        // One index space: pads, then runs, then vias.
        int runBase = pads.size();
        int viaBase = runBase + runs.size();
        int[] parents = new int[viaBase + vias.size()];
        for (int index = 0; index < parents.length; index++) {
            parents[index] = index;
        }
        for (int runIndex = 0; runIndex < runs.size(); runIndex++) {
            Run run = runs.get(runIndex);
            for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
                PlacedPad pad = pads.get(padIndex);
                if (pad.pad().hasCopperOn(run.layer(), pad.placement()) && touching(run.shape(), pad.shape())) {
                    join(parents, runBase + runIndex, padIndex);
                }
            }
            for (int otherIndex = runIndex + 1; otherIndex < runs.size(); otherIndex++) {
                Run other = runs.get(otherIndex);
                if (other.layer().equals(run.layer()) && touching(run.shape(), other.shape())) {
                    join(parents, runBase + runIndex, runBase + otherIndex);
                }
            }
            for (int viaIndex = 0; viaIndex < vias.size(); viaIndex++) {
                if (board.reaches(board.vias().get(viaIndex), run.layer()) && touching(run.shape(), vias.get(viaIndex))) {
                    join(parents, runBase + runIndex, viaBase + viaIndex);
                }
            }
        }

        Map<Integer, Set<String>> netsOfGroup = new HashMap<>();
        for (int padIndex = 0; padIndex < pads.size(); padIndex++) {
            int group = find(parents, padIndex);
            padNets.get(padIndex).ifPresent(net -> netsOfGroup.computeIfAbsent(group, ignored -> new HashSet<>()).add(net));
        }
        return new CopperNets(pads, padNets,
                netsOf(parents, netsOfGroup, runBase, board.tracks().size()),
                netsOf(parents, netsOfGroup, runBase + board.tracks().size(), board.arcs().size()),
                netsOf(parents, netsOfGroup, viaBase, vias.size()));
    }

    /** Every pad with copper, where its part stands it. */
    public List<PlacedPad> pads() {
        return pads;
    }

    /** Each pad's net as the circuit wires it, in the order of {@link #pads()}; empty for a pad it does not wire. */
    public List<Optional<String>> padNets() {
        return padNets;
    }

    /** Each track's net, by the board's order; empty for a track joined to no pad, or to pads of two nets. */
    public List<Optional<String>> trackNets() {
        return trackNets;
    }

    public List<Optional<String>> arcNets() {
        return arcNets;
    }

    public List<Optional<String>> viaNets() {
        return viaNets;
    }

    private static List<Optional<String>> netsOf(int[] parents, Map<Integer, Set<String>> netsOfGroup, int base, int count) {
        List<Optional<String>> nets = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Set<String> group = netsOfGroup.getOrDefault(find(parents, base + index), Set.of());
            nets.add(group.size() == 1 ? Optional.of(group.iterator().next()) : Optional.empty());
        }
        return nets;
    }

    private static boolean touching(Geometry first, Geometry second) {
        return first.getEnvelopeInternal().distance(second.getEnvelopeInternal()) <= JOIN_TOLERANCE
                && first.isWithinDistance(second, JOIN_TOLERANCE);
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
    static Geometry polygon(List<BoardDrawing.Point> outline) {
        if (outline.size() < 3) {
            return GEOMETRY.createPolygon();
        }
        List<Coordinate> coordinates = new ArrayList<>(outline.stream().map(point -> new Coordinate(point.x(), point.y())).toList());
        coordinates.add(coordinates.getFirst());
        return GEOMETRY.createPolygon(coordinates.toArray(Coordinate[]::new));
    }

    /** A line of copper: its points grown by half its width, round at its ends and its corners. */
    static Geometry line(List<BoardDrawing.Point> points, double width) {
        Coordinate[] coordinates = points.stream().map(point -> new Coordinate(point.x(), point.y())).toArray(Coordinate[]::new);
        Geometry centre = coordinates.length == 1 ? GEOMETRY.createPoint(coordinates[0]) : GEOMETRY.createLineString(coordinates);
        return centre.buffer(width / 2, BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_ROUND);
    }

    /** The points an arc of the board passes, from its start through its middle point to its end. */
    static List<BoardDrawing.Point> arcPoints(BoardDrawing.Arc arc, int steps) {
        BoardDrawing.Point from = arc.from();
        BoardDrawing.Point through = arc.through();
        BoardDrawing.Point to = arc.to();
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
        List<BoardDrawing.Point> points = new ArrayList<>();
        for (int step = 0; step <= steps; step++) {
            double angle = start + sweep * step / steps;
            points.add(new BoardDrawing.Point(centreX + radius * Math.cos(angle), centreY + radius * Math.sin(angle)));
        }
        return points;
    }

    /** An angle brought into {@code [0, 2π)}. */
    private static double normalised(double angle) {
        double turned = angle % (2 * Math.PI);
        return turned < 0 ? turned + 2 * Math.PI : turned;
    }
}
