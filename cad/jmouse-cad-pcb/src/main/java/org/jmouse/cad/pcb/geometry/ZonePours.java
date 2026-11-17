package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.drawing.NetRules;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.jmouse.geometry.Areas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The pcb's zones poured as a pcb house pours them: the area drawn, inside the pcb's edge less its clearance and
 * less every cut-out, less every keepout on its layer; every piece of copper of another net — whatever it is: a pad, a track, a via,
 * copper drawn as a shape, a teardrop — cut back by the gap the two keep, a zone of higher priority poured first and kept clear of too; a pad of its own net joined across a thermal gap by four spokes,
 * so it can be soldered; nothing left narrower than copper can be made; every island that joins fewer than two things of its
 * net taken away.
 */
public final class ZonePours {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    private static final PathParser PATHS = new PathParser();

    /** The rule words a pour is made by: its gaps, its thermal reliefs, the narrowest copper it may leave. */
    private static final String EDGE_CLEARANCE      = "edge-clearance";
    private static final String THERMAL_GAP         = "thermal-gap";
    private static final String THERMAL_SPOKE_WIDTH = "thermal-spoke-width";
    private static final String LEAST_WIDTH         = "least-track-width";

    private final PcbDrawing        pcb;
    private final CopperNets          nets;
    /** Every piece of the pcb's copper but its pours: what a zone is poured round. */
    private final List<CopperPieces.Piece> copper;
    private final Map<String, Double> rules;
    private final int                 curveSteps;
    private final AreaOffsets         offsets;
    private final double              arcError;
    private final NetRules            netRules;
    private final PourMemory          memory;

    /**
     * @param rules    the values the pcb is held to, by rule word — {@code clearance}, {@code thermal-gap} …
     * @param arcError how far a round offset followed in chords may stand off the true curve; every gap is grown by it
     */
    public ZonePours(PcbDrawing pcb, CopperNets nets, List<CopperPieces.Piece> copper, Map<String, Double> rules, int curveSteps, double arcError) {
        this(pcb, nets, copper, rules, curveSteps, arcError, PourMemory.none());
    }

    /** @param memory what each zone was last poured from: a zone is poured again only where a change reached it */
    public ZonePours(PcbDrawing pcb, CopperNets nets, List<CopperPieces.Piece> copper, Map<String, Double> rules, int curveSteps, double arcError,
                     PourMemory memory) {
        this.memory = memory;
        this.pcb = pcb;
        this.nets = nets;
        this.copper = List.copyOf(copper);
        this.rules = Map.copyOf(rules);
        this.curveSteps = curveSteps;
        this.offsets = new AreaOffsets(arcError);
        this.arcError = arcError;
        this.netRules = new NetRules(pcb, rules);
    }

    /** Every zone poured, by its place in the pcb's list; empty where nothing of it is left. */
    public List<Optional<Geometry>> pour() {
        List<Optional<Geometry>> pours = new ArrayList<>(pcb.zones().stream().map(zone -> Optional.<Geometry>empty()).toList());
        Optional<Geometry> pcbArea = pcbArea();
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < pcb.zones().size(); index++) {
            order.add(index);
        }
        // The higher priority first; of two alike, the smaller — a zone drawn inside another is what is asked for there —; of two
        // of one size, the one drawn first.
        List<Double> sizes = pcb.zones().stream().map(zone -> area(zone.path()).getArea()).toList();
        order.sort(Comparator.comparingInt((Integer index) -> -pcb.zones().get(index).priority())
                .thenComparingDouble(sizes::get).thenComparingInt(index -> index));
        for (int index : order) {
            PcbDrawing.Zone zone = pcb.zones().get(index);
            Geometry poured = poured(zone, pcbArea, pours);
            pours.set(index, poured.isEmpty() ? Optional.empty() : Optional.of(poured));
        }
        return pours;
    }

    /** What a zone's area is made of beyond the zone: where any of it differs, the zone is poured all over again. */
    private record AreaInputs(String outline, double edgeClearance, List<PcbDrawing.Keepout> keepouts, int curveSteps, double arcError) {
    }

    /** A zone poured before this one, of another net on its layer, kept clear of by its gap. */
    private record EarlierPour(Geometry pour, double gap) {
    }

    private Geometry poured(PcbDrawing.Zone zone, Optional<Geometry> pcbArea, List<Optional<Geometry>> pours) {
        Geometry area = area(zone.path());
        if (pcbArea.isPresent()) {
            area = Areas.common(area, pcbArea.get());
        }
        List<PcbDrawing.Keepout> keepouts = new ArrayList<>();
        for (PcbDrawing.Keepout keepout : pcb.keepouts()) {
            if (keepout.layers().contains(zone.layer())) {
                keepouts.add(keepout);
                area = Areas.difference(area, area(keepout.path()));
            }
        }
        double zoneGap = netRules.zoneGapOf(zone);
        List<PourMemory.Piece> pieces = new ArrayList<>();
        List<Anchor> anchors = new ArrayList<>();
        for (CopperPieces.Piece piece : copper) {
            if (piece.onLayer().test(zone.layer())) {
                meet(piece, zone, zoneGap, pieces, anchors);
            }
        }
        for (int index = 0; index < pours.size(); index++) {
            PcbDrawing.Zone earlier = pcb.zones().get(index);
            if (pours.get(index).isPresent() && earlier.layer().equals(zone.layer()) && !earlier.net().equals(zone.net())) {
                Geometry pour = pours.get(index).get();
                double gap = Math.max(zoneGap, clearanceOf(Optional.of(earlier.net())));
                pieces.add(new PourMemory.Piece(PourMemory.Role.OTHER, new EarlierPour(pour, gap), reachOf(pour.getEnvelopeInternal(), gap),
                        () -> grown(pour, gap)));
            }
        }
        double narrowest = rule(LEAST_WIDTH) / 2;
        // A change reaches as far as the pour is shrunk and grown back again, each offset as far off as its chords may stand.
        double changeReaches = 2 * (narrowest + arcError) + 1e-6;
        PourMemory.Frame frame = new PourMemory.Frame(zone, new AreaInputs(pcb.outline(), rule(EDGE_CLEARANCE), keepouts, curveSteps, arcError),
                changeReaches);
        Geometry opened = memory.opened(frame, area, pieces, (within, near) -> {
            // Only what reaches the area's box is united: copper whose box misses it cannot change what the area keeps.
            Envelope reach = within.getEnvelopeInternal();
            Geometry open = Areas.difference(within, Areas.unionOf(PourMemory.copperOf(near, PourMemory.Role.OTHER, reach), GEOMETRY));
            Geometry filled = Areas.union(Areas.difference(open, Areas.unionOf(PourMemory.copperOf(near, PourMemory.Role.THERMAL, reach), GEOMETRY)),
                    Areas.common(Areas.unionOf(PourMemory.copperOf(near, PourMemory.Role.SPOKES, reach), GEOMETRY), open));
            // Nothing narrower than copper can be made: shrunk by at least half the least width, then grown back by exactly it,
            // so what is left never reaches past what was open — a gap kept above stays kept.
            return grownBack(grown(filled, -narrowest), narrowest);
        });
        return withoutIslands(opened, anchors);
    }

    /** A box grown by a gap and by how far a round offset followed in chords may stand off: as far as a piece's copper reaches. */
    private Envelope reachOf(Envelope box, double gap) {
        Envelope reach = new Envelope(box);
        reach.expandBy(gap + arcError);
        return reach;
    }

    /**
     * How a zone meets a piece of copper on its layer, whatever the piece is: of the zone's net, joined as its kind joins — across a
     * thermal gap by spokes, or solid, its points what the pour must reach —; of no net, stitching the zone or kept clear of, as its kind
     * says; of another net, kept clear of by the gap the two keep.
     */
    private void meet(CopperPieces.Piece piece, PcbDrawing.Zone zone, double zoneGap, List<PourMemory.Piece> pieces, List<Anchor> anchors) {
        String item = piece.kind() + ":" + piece.index();
        boolean ownNet = piece.net().equals(Optional.of(zone.net()));
        boolean stitches = piece.net().isEmpty() && piece.kind().netless() == CopperPieces.NetlessCopper.STITCHES;
        if (!ownNet && !stitches) {
            double gap = Math.max(zoneGap, clearanceOf(piece.net()));
            pieces.add(new PourMemory.Piece(PourMemory.Role.OTHER, new Grown(piece.origin(), gap), reachOf(envelopeOf(piece.shape()), gap),
                    () -> grown(areaOf(piece.shape()), gap)));
            return;
        }
        if (piece.kind().joining() == CopperPieces.PourJoining.THERMAL) {
            CopperNets.PlacedPad pad = nets.pads().get(piece.index());
            double thermalGap = rule(THERMAL_GAP);
            pieces.add(new PourMemory.Piece(PourMemory.Role.THERMAL, new Grown(piece.origin(), thermalGap), reachOf(envelopeOf(piece.shape()), thermalGap),
                    () -> grown(areaOf(piece.shape()), thermalGap)));
            List<Geometry> spokes = new ArrayList<>();
            spokes(pad, item, spokes, anchors);
            Envelope spokesReach = new Envelope();
            spokes.forEach(spoke -> spokesReach.expandToInclude(spoke.getEnvelopeInternal()));
            pieces.add(new PourMemory.Piece(PourMemory.Role.SPOKES, new Spokes(piece.origin(), thermalGap, rule(THERMAL_SPOKE_WIDTH)), reachOf(spokesReach, 0),
                    () -> Areas.unionOf(spokes, GEOMETRY)));
            return;
        }
        for (double[] line : piece.shape().pieces()) {
            anchors.add(new Anchor(item, new Coordinate(line[0], line[1])));
            anchors.add(new Anchor(item, new Coordinate(line[2], line[3])));
        }
    }

    /** A piece of copper, as what it is the copper of, grown by a gap: what a pour keeps clear of, or relieves round. */
    private record Grown(Object origin, double gap) {
    }

    /** A pad's four thermal spokes, as the pad they join. */
    private record Spokes(Object origin, double gap, double width) {
    }

    /** The box a piece of copper covers, its line's width included. */
    private static Envelope envelopeOf(CopperShape shape) {
        return new Envelope(shape.box().left(), shape.box().right(), shape.box().top(), shape.box().bottom());
    }

    /**
     * A piece of copper's area, whatever it is: its straight pieces followed on into runs; where they close round an area, the area by the
     * even-odd rule; grown by its line's radius — a track's half width, a round pad's radius —, as round as every pour's offsets are.
     */
    private Geometry areaOf(CopperShape shape) {
        List<List<Coordinate>> runs = new ArrayList<>();
        List<Coordinate> run = new ArrayList<>();
        for (double[] line : shape.pieces()) {
            Coordinate from = new Coordinate(line[0], line[1]);
            if (run.isEmpty() || !run.getLast().equals2D(from)) {
                if (!run.isEmpty()) {
                    runs.add(run);
                }
                run = new ArrayList<>(List.of(from));
            }
            run.add(new Coordinate(line[2], line[3]));
        }
        if (!run.isEmpty()) {
            runs.add(run);
        }
        Geometry area = GEOMETRY.createPolygon();
        List<Geometry> lines = new ArrayList<>();
        for (List<Coordinate> points : runs) {
            boolean closes = points.size() > 3 && points.getFirst().equals2D(points.getLast());
            if (shape.area() && closes) {
                area = Areas.exclusive(area, GEOMETRY.createPolygon(points.toArray(Coordinate[]::new)));
            } else if (points.size() == 2 && points.getFirst().equals2D(points.getLast())) {
                lines.add(GEOMETRY.createPoint(points.getFirst()));
            } else {
                lines.add(GEOMETRY.createLineString(points.toArray(Coordinate[]::new)));
            }
        }
        if (shape.radius() <= 0) {
            return area;
        }
        lines.add(area);
        return grown(Areas.unionOf(lines, GEOMETRY), shape.radius());
    }

    /**
     * Four spokes along a pad's own axes, from its middle out across the thermal gap; the pad itself is cut out of the
     * pour, so the spoke just beyond the gap is what the pour must reach.
     */
    private void spokes(CopperNets.PlacedPad pad, String item, List<Geometry> spokes, List<Anchor> anchors) {
        double reach = Math.max(pad.pad().width(), pad.pad().height()) / 2 + rule(THERMAL_GAP) + rule(THERMAL_SPOKE_WIDTH);
        double turn = Math.toRadians(-pad.pad().rotation());
        BufferParameters flat = new BufferParameters(BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_FLAT);
        for (double[] along : new double[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            double x = along[0] * reach;
            double y = along[1] * reach;
            PcbDrawing.Point end = pad.placement().placed(pad.pad().x() + x * Math.cos(turn) - y * Math.sin(turn),
                    pad.pad().y() + x * Math.sin(turn) + y * Math.cos(turn));
            Coordinate centre = new Coordinate(pad.centre().x(), pad.centre().y());
            spokes.add(BufferOp.bufferOp(GEOMETRY.createLineString(new Coordinate[] {centre, new Coordinate(end.x(), end.y())}),
                    rule(THERMAL_SPOKE_WIDTH) / 2, flat));
            double beyond = (reach - rule(THERMAL_SPOKE_WIDTH) / 2) / reach;
            anchors.add(new Anchor(item, new Coordinate(centre.x + (end.x() - centre.x) * beyond, centre.y + (end.y() - centre.y) * beyond)));
        }
    }

    /**
     * The pour less every island that joins nothing: an island is kept when it reaches two things of its net — two pads, a
     * pad and a track, a via and a pad —, for copper is there to join. One that reaches a single pad carries no current
     * anywhere: it is metal left on the board for nothing, and a pad's thermal into it is spokes to nowhere.
     *
     * <p>The anchors are indexed once; an island asks only those within its box, through its outline prepared, and stops at the
     * second thing it reaches. Asking every anchor of a dense board of every island, through an outline walked whole each time,
     * was what a pour cost — seconds after every change.</p>
     */
    private static Geometry withoutIslands(Geometry filled, List<Anchor> anchors) {
        STRtree anchorsIndexed = new STRtree();
        anchors.forEach(anchor -> anchorsIndexed.insert(new Envelope(anchor.at()), anchor));
        List<Geometry> kept = new ArrayList<>();
        for (int index = 0; index < filled.getNumGeometries(); index++) {
            Geometry island = filled.getGeometryN(index);
            if (joinsEnough(island, anchorsIndexed)) {
                kept.add(island);
            }
        }
        return kept.isEmpty() ? GEOMETRY.createPolygon() : Areas.unionOf(kept, GEOMETRY);
    }

    /** Whether an island reaches as many things as joining takes: the anchors within its box asked through its outline prepared. */
    private static boolean joinsEnough(Geometry island, STRtree anchors) {
        PreparedGeometry outline = PreparedGeometryFactory.prepare(island);
        Set<String> joined = new HashSet<>();
        for (Object near : anchors.query(island.getEnvelopeInternal())) {
            Anchor anchor = (Anchor) near;
            if (!joined.contains(anchor.item()) && outline.intersects(GEOMETRY.createPoint(anchor.at()))) {
                joined.add(anchor.item());
                if (joined.size() >= JOINED) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A point of its net's copper the pour must reach, and what it is of: {@code pad 3}, {@code track 7}. */
    private record Anchor(String item, Coordinate at) {
    }

    /** What an island must reach to join anything: two things, by what joining is. */
    private static final int JOINED = 2;

    private double rule(String word) {
        return rules.getOrDefault(word, 0.0);
    }

    /** The gap a net's copper keeps: the pcb's, or its class's when that asks more. */
    private double clearanceOf(Optional<String> net) {
        return netRules.clearanceOf(net);
    }

    /** The pcb's own area — inside its edge, out of its cut-outs —, shrunk by the gap copper keeps from the edge. */
    private Optional<Geometry> pcbArea() {
        if (pcb.outline() == null || pcb.outline().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(grown(evenOdd(pcb.outline()), -rule(EDGE_CLEARANCE)));
    }

    /** An area a path closes: its rings joined, a ring inside another adding to it. */
    private Geometry area(String path) {
        return Areas.unionOf(rings(path, curveSteps).stream().map(ring -> (Geometry) GEOMETRY.createPolygon(ring)).toList(), GEOMETRY);
    }

    /** An area a path closes by the even-odd rule: a ring inside another cuts it out — the pcb's cut-outs. */
    private Geometry evenOdd(String path) {
        return areaOf(path, curveSteps);
    }

    /** The area a closed path draws, even-odd: a run inside another is a hole in it. Each curve followed in {@code curveSteps} pieces. */
    public static Geometry areaOf(String path, int curveSteps) {
        Geometry area = GEOMETRY.createPolygon();
        for (LinearRing ring : rings(path, curveSteps)) {
            area = Areas.exclusive(area, GEOMETRY.createPolygon(ring));
        }
        return area;
    }

    private static List<LinearRing> rings(String path, int curveSteps) {
        List<LinearRing> rings = new ArrayList<>();
        for (List<double[]> run : PathPolylines.of(PATHS.parse(path), curveSteps)) {
            if (run.size() < 3) {
                continue;
            }
            List<Coordinate> coordinates = new ArrayList<>(run.stream().map(point -> new Coordinate(point[0], point[1])).toList());
            if (!coordinates.getFirst().equals2D(coordinates.getLast())) {
                coordinates.add(coordinates.getFirst());
            }
            rings.add(GEOMETRY.createLinearRing(coordinates.toArray(Coordinate[]::new)));
        }
        return rings;
    }

    private Geometry grown(Geometry area, double distance) {
        return offsets.grown(area, distance);
    }

    private Geometry grownBack(Geometry area, double distance) {
        return offsets.grownBack(area, distance);
    }

    /** The polygons of a pour, the largest first: one that holds another is drawn before what stands in its hole. */
    public static List<Polygon> polygonsOf(Geometry pour) {
        List<Polygon> polygons = new ArrayList<>();
        for (int index = 0; index < pour.getNumGeometries(); index++) {
            if (pour.getGeometryN(index) instanceof Polygon polygon && !polygon.isEmpty()) {
                polygons.add(polygon);
            }
        }
        polygons.sort(Comparator.comparingDouble(Geometry::getArea).reversed());
        return polygons;
    }
}
