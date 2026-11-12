package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.drawing.NetRules;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.operation.union.UnaryUnionOp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The pcb's zones poured as a pcb house pours them: the area drawn, inside the pcb's edge less its clearance and
 * less every cut-out, less every keepout on its layer; copper of every other net cut back by the gap the two keep, a zone
 * of higher priority poured first and kept clear of too; a pad of its own net joined across a thermal gap by four spokes,
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
    private final Map<String, Double> rules;
    private final int                 curveSteps;
    private final AreaOffsets         offsets;
    private final NetRules            netRules;

    /**
     * @param rules    the values the pcb is held to, by rule word — {@code clearance}, {@code thermal-gap} …
     * @param arcError how far a round offset followed in chords may stand off the true curve; every gap is grown by it
     */
    public ZonePours(PcbDrawing pcb, CopperNets nets, Map<String, Double> rules, int curveSteps, double arcError) {
        this.pcb = pcb;
        this.nets = nets;
        this.rules = Map.copyOf(rules);
        this.curveSteps = curveSteps;
        this.offsets = new AreaOffsets(arcError);
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
        // The higher priority first; of two alike, the one drawn first.
        order.sort(Comparator.comparingInt((Integer index) -> -pcb.zones().get(index).priority()).thenComparingInt(index -> index));
        for (int index : order) {
            PcbDrawing.Zone zone = pcb.zones().get(index);
            Geometry poured = poured(zone, pcbArea, pours);
            pours.set(index, poured.isEmpty() ? Optional.empty() : Optional.of(poured));
        }
        return pours;
    }

    private Geometry poured(PcbDrawing.Zone zone, Optional<Geometry> pcbArea, List<Optional<Geometry>> pours) {
        Geometry area = area(zone.path());
        if (pcbArea.isPresent()) {
            area = Areas.common(area, pcbArea.get());
        }
        for (PcbDrawing.Keepout keepout : pcb.keepouts()) {
            if (keepout.layers().contains(zone.layer())) {
                area = area.difference(area(keepout.path()));
            }
        }
        double zoneGap = netRules.zoneGapOf(zone);
        List<Geometry> others = new ArrayList<>();
        List<Geometry> thermals = new ArrayList<>();
        List<Geometry> spokes = new ArrayList<>();
        List<Anchor> anchors = new ArrayList<>();
        for (int index = 0; index < nets.pads().size(); index++) {
            CopperNets.PlacedPad pad = nets.pads().get(index);
            if (!pad.pad().hasCopperOn(zone.layer(), pad.placement())) {
                continue;
            }
            Optional<String> net = nets.padNets().get(index);
            if (!net.equals(Optional.of(zone.net()))) {
                others.add(grown(pad.shape(), Math.max(zoneGap, clearanceOf(net))));
                continue;
            }
            thermals.add(grown(pad.shape(), rule(THERMAL_GAP)));
            spokes(pad, PAD + index, spokes, anchors);
        }
        for (int index = 0; index < pcb.tracks().size(); index++) {
            PcbDrawing.Track track = pcb.tracks().get(index);
            if (track.layer().equals(zone.layer())) {
                ownOrOther(nets.trackNets().get(index), zone, zoneGap, CopperNets.line(track.points(), track.width()),
                        track.points(), TRACK + index, others, anchors);
            }
        }
        for (int index = 0; index < pcb.arcs().size(); index++) {
            PcbDrawing.Arc arc = pcb.arcs().get(index);
            if (arc.layer().equals(zone.layer())) {
                ownOrOther(nets.arcNets().get(index), zone, zoneGap,
                        CopperNets.line(CopperNets.arcPoints(arc, curveSteps), arc.width()), List.of(arc.from(), arc.to()), ARC + index,
                        others, anchors);
            }
        }
        for (int index = 0; index < pcb.vias().size(); index++) {
            PcbDrawing.Via via = pcb.vias().get(index);
            if (!pcb.reaches(via, zone.layer())) {
                continue;
            }
            Optional<String> net = nets.viaNets().get(index);
            // A via of no net yet stitches the zone: it is poured over, as a via of its own net is.
            if (net.isEmpty() || net.get().equals(zone.net())) {
                anchors.add(new Anchor(VIA + index, new Coordinate(via.x(), via.y())));
            } else {
                others.add(circle(via.x(), via.y(), via.diameter() / 2 + Math.max(zoneGap, clearanceOf(net))));
            }
        }
        // A mounting hole is poured round, its ring or its bare edge kept a gap off.
        for (PcbDrawing.Hole hole : pcb.holes()) {
            others.add(circle(hole.x(), hole.y(), Math.max(hole.ring(), hole.drill()) / 2 + Math.max(zoneGap, clearanceOf(Optional.empty()))));
        }
        for (int index = 0; index < pours.size(); index++) {
            PcbDrawing.Zone earlier = pcb.zones().get(index);
            if (pours.get(index).isPresent() && earlier.layer().equals(zone.layer()) && !earlier.net().equals(zone.net())) {
                others.add(grown(pours.get(index).get(), Math.max(zoneGap, clearanceOf(Optional.of(earlier.net())))));
            }
        }
        Geometry open = area.difference(UnaryUnionOp.union(others, GEOMETRY));
        Geometry filled = open.difference(UnaryUnionOp.union(thermals, GEOMETRY))
                .union(Areas.common(UnaryUnionOp.union(spokes, GEOMETRY), open));
        // Nothing narrower than copper can be made: shrunk by at least half the least width, then grown back by exactly it,
        // so what is left never reaches past what was open — a gap kept above stays kept.
        double narrowest = rule(LEAST_WIDTH) / 2;
        filled = grownBack(grown(filled, -narrowest), narrowest);
        return withoutIslands(filled, anchors);
    }

    /** A track or an arc: a point the pour must reach when it is of the zone's net, copper to keep clear of otherwise. */
    private void ownOrOther(Optional<String> net, PcbDrawing.Zone zone, double zoneGap, Geometry copper,
                            List<PcbDrawing.Point> points, String item, List<Geometry> others, List<Anchor> anchors) {
        if (net.equals(Optional.of(zone.net()))) {
            points.forEach(point -> anchors.add(new Anchor(item, new Coordinate(point.x(), point.y()))));
        } else {
            others.add(grown(copper, Math.max(zoneGap, clearanceOf(net))));
        }
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
     */
    private static Geometry withoutIslands(Geometry filled, List<Anchor> anchors) {
        List<Geometry> kept = new ArrayList<>();
        for (int index = 0; index < filled.getNumGeometries(); index++) {
            Geometry island = filled.getGeometryN(index);
            long joined = anchors.stream().filter(anchor -> island.intersects(GEOMETRY.createPoint(anchor.at())))
                    .map(Anchor::item).distinct().count();
            if (joined >= JOINED) {
                kept.add(island);
            }
        }
        return kept.isEmpty() ? GEOMETRY.createPolygon() : UnaryUnionOp.union(kept, GEOMETRY);
    }

    /** A point of its net's copper the pour must reach, and what it is of: {@code pad 3}, {@code track 7}. */
    private record Anchor(String item, Coordinate at) {
    }

    /** What an island must reach to join anything: two things, by what joining is. */
    private static final int JOINED = 2;

    /** What each kind of copper an anchor is of is called, before its place among its kind. */
    private static final String PAD = "pad ";
    private static final String TRACK = "track ";
    private static final String ARC = "arc ";
    private static final String VIA = "via ";

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
        return UnaryUnionOp.union(rings(path, curveSteps).stream().map(ring -> (Geometry) GEOMETRY.createPolygon(ring)).toList(), GEOMETRY);
    }

    /** An area a path closes by the even-odd rule: a ring inside another cuts it out — the pcb's cut-outs. */
    private Geometry evenOdd(String path) {
        return areaOf(path, curveSteps);
    }

    /** The area a closed path draws, even-odd: a run inside another is a hole in it. Each curve followed in {@code curveSteps} pieces. */
    public static Geometry areaOf(String path, int curveSteps) {
        Geometry area = GEOMETRY.createPolygon();
        for (LinearRing ring : rings(path, curveSteps)) {
            area = area.symDifference(GEOMETRY.createPolygon(ring));
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

    private Geometry circle(double x, double y, double radius) {
        return grown(GEOMETRY.createPoint(new Coordinate(x, y)), radius);
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
