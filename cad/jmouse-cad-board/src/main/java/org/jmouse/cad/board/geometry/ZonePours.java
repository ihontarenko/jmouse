package org.jmouse.cad.board.geometry;

import org.jmouse.cad.board.drawing.BoardDrawing;
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
 * The board's zones poured as a board house pours them: the area drawn, inside the board's edge less its clearance and
 * less every cut-out, less every keepout on its layer; copper of every other net cut back by the gap the two keep, a zone
 * of higher priority poured first and kept clear of too; a pad of its own net joined across a thermal gap by four spokes,
 * so it can be soldered; nothing left narrower than copper can be made; every island that touches none of its net's copper
 * taken away.
 */
public final class ZonePours {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    private static final PathParser PATHS = new PathParser();

    /** The rule words a pour is made by: its gaps, its thermal reliefs, the narrowest copper it may leave. */
    private static final String CLEARANCE           = "clearance";
    private static final String EDGE_CLEARANCE      = "edge-clearance";
    private static final String THERMAL_GAP         = "thermal-gap";
    private static final String THERMAL_SPOKE_WIDTH = "thermal-spoke-width";
    private static final String LEAST_WIDTH         = "least-track-width";

    private final BoardDrawing        board;
    private final CopperNets          nets;
    private final Map<String, Double> rules;
    private final int                 curveSteps;

    /**
     * @param rules the values the board is held to, by rule word — {@code clearance}, {@code thermal-gap} …
     */
    public ZonePours(BoardDrawing board, CopperNets nets, Map<String, Double> rules, int curveSteps) {
        this.board = board;
        this.nets = nets;
        this.rules = Map.copyOf(rules);
        this.curveSteps = curveSteps;
    }

    /** Every zone poured, by its place in the board's list; empty where nothing of it is left. */
    public List<Optional<Geometry>> pour() {
        List<Optional<Geometry>> pours = new ArrayList<>(board.zones().stream().map(zone -> Optional.<Geometry>empty()).toList());
        Optional<Geometry> boardArea = boardArea();
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < board.zones().size(); index++) {
            order.add(index);
        }
        // The higher priority first; of two alike, the one drawn first.
        order.sort(Comparator.comparingInt((Integer index) -> -board.zones().get(index).priority()).thenComparingInt(index -> index));
        for (int index : order) {
            BoardDrawing.Zone zone = board.zones().get(index);
            Geometry poured = poured(zone, boardArea, pours);
            pours.set(index, poured.isEmpty() ? Optional.empty() : Optional.of(poured));
        }
        return pours;
    }

    private Geometry poured(BoardDrawing.Zone zone, Optional<Geometry> boardArea, List<Optional<Geometry>> pours) {
        Geometry area = area(zone.path());
        if (boardArea.isPresent()) {
            area = area.intersection(boardArea.get());
        }
        for (BoardDrawing.Keepout keepout : board.keepouts()) {
            if (keepout.layers().contains(zone.layer())) {
                area = area.difference(area(keepout.path()));
            }
        }
        double zoneGap = zone.clearance() > 0 ? zone.clearance() : clearanceOf(Optional.of(zone.net()));
        List<Geometry> others = new ArrayList<>();
        List<Geometry> thermals = new ArrayList<>();
        List<Geometry> spokes = new ArrayList<>();
        List<Coordinate> anchors = new ArrayList<>();
        for (int index = 0; index < nets.pads().size(); index++) {
            CopperNets.PlacedPad pad = nets.pads().get(index);
            if (!pad.pad().hasCopperOn(zone.layer(), pad.placement())) {
                continue;
            }
            Optional<String> net = nets.padNets().get(index);
            if (!net.equals(Optional.of(zone.net()))) {
                others.add(pad.shape().buffer(Math.max(zoneGap, clearanceOf(net))));
                continue;
            }
            thermals.add(pad.shape().buffer(rule(THERMAL_GAP)));
            spokes(pad, spokes, anchors);
        }
        for (int index = 0; index < board.tracks().size(); index++) {
            BoardDrawing.Track track = board.tracks().get(index);
            if (track.layer().equals(zone.layer())) {
                ownOrOther(nets.trackNets().get(index), zone, zoneGap, CopperNets.line(track.points(), track.width()),
                        track.points(), others, anchors);
            }
        }
        for (int index = 0; index < board.arcs().size(); index++) {
            BoardDrawing.Arc arc = board.arcs().get(index);
            if (arc.layer().equals(zone.layer())) {
                ownOrOther(nets.arcNets().get(index), zone, zoneGap,
                        CopperNets.line(CopperNets.arcPoints(arc, curveSteps), arc.width()), List.of(arc.from(), arc.to()), others, anchors);
            }
        }
        for (int index = 0; index < board.vias().size(); index++) {
            BoardDrawing.Via via = board.vias().get(index);
            if (!board.reaches(via, zone.layer())) {
                continue;
            }
            Optional<String> net = nets.viaNets().get(index);
            // A via of no net yet stitches the zone: it is poured over, as a via of its own net is.
            if (net.isEmpty() || net.get().equals(zone.net())) {
                anchors.add(new Coordinate(via.x(), via.y()));
            } else {
                others.add(circle(via.x(), via.y(), via.diameter() / 2 + Math.max(zoneGap, clearanceOf(net))));
            }
        }
        // A mounting hole is poured round, its ring or its bare edge kept a gap off.
        for (BoardDrawing.Hole hole : board.holes()) {
            others.add(circle(hole.x(), hole.y(), Math.max(hole.ring(), hole.drill()) / 2 + Math.max(zoneGap, clearanceOf(Optional.empty()))));
        }
        for (int index = 0; index < pours.size(); index++) {
            BoardDrawing.Zone earlier = board.zones().get(index);
            if (pours.get(index).isPresent() && earlier.layer().equals(zone.layer()) && !earlier.net().equals(zone.net())) {
                others.add(pours.get(index).get().buffer(Math.max(zoneGap, clearanceOf(Optional.of(earlier.net())))));
            }
        }
        Geometry open = area.difference(UnaryUnionOp.union(others, GEOMETRY));
        Geometry filled = open.difference(UnaryUnionOp.union(thermals, GEOMETRY))
                .union(UnaryUnionOp.union(spokes, GEOMETRY).intersection(open));
        double narrowest = rule(LEAST_WIDTH) / 2;
        filled = filled.buffer(-narrowest).buffer(narrowest);
        return withoutIslands(filled, anchors);
    }

    /** A track or an arc: a point the pour must reach when it is of the zone's net, copper to keep clear of otherwise. */
    private void ownOrOther(Optional<String> net, BoardDrawing.Zone zone, double zoneGap, Geometry copper,
                            List<BoardDrawing.Point> points, List<Geometry> others, List<Coordinate> anchors) {
        if (net.equals(Optional.of(zone.net()))) {
            points.forEach(point -> anchors.add(new Coordinate(point.x(), point.y())));
        } else {
            others.add(copper.buffer(Math.max(zoneGap, clearanceOf(net))));
        }
    }

    /**
     * Four spokes along a pad's own axes, from its middle out across the thermal gap; the pad itself is cut out of the
     * pour, so the spoke just beyond the gap is what the pour must reach.
     */
    private void spokes(CopperNets.PlacedPad pad, List<Geometry> spokes, List<Coordinate> anchors) {
        double reach = Math.max(pad.pad().width(), pad.pad().height()) / 2 + rule(THERMAL_GAP) + rule(THERMAL_SPOKE_WIDTH);
        double turn = Math.toRadians(-pad.pad().rotation());
        BufferParameters flat = new BufferParameters(BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_FLAT);
        for (double[] along : new double[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            double x = along[0] * reach;
            double y = along[1] * reach;
            BoardDrawing.Point end = pad.placement().placed(pad.pad().x() + x * Math.cos(turn) - y * Math.sin(turn),
                    pad.pad().y() + x * Math.sin(turn) + y * Math.cos(turn));
            Coordinate centre = new Coordinate(pad.centre().x(), pad.centre().y());
            spokes.add(BufferOp.bufferOp(GEOMETRY.createLineString(new Coordinate[] {centre, new Coordinate(end.x(), end.y())}),
                    rule(THERMAL_SPOKE_WIDTH) / 2, flat));
            double beyond = (reach - rule(THERMAL_SPOKE_WIDTH) / 2) / reach;
            anchors.add(new Coordinate(centre.x + (end.x() - centre.x) * beyond, centre.y + (end.y() - centre.y) * beyond));
        }
    }

    /** The pour less every island that touches none of its net's copper: one of its net's points must stand in it. */
    private static Geometry withoutIslands(Geometry filled, List<Coordinate> anchors) {
        List<Geometry> kept = new ArrayList<>();
        for (int index = 0; index < filled.getNumGeometries(); index++) {
            Geometry island = filled.getGeometryN(index);
            if (anchors.stream().anyMatch(anchor -> island.intersects(GEOMETRY.createPoint(anchor)))) {
                kept.add(island);
            }
        }
        return kept.isEmpty() ? GEOMETRY.createPolygon() : UnaryUnionOp.union(kept, GEOMETRY);
    }

    private double rule(String word) {
        return rules.getOrDefault(word, 0.0);
    }

    /** The gap a net's copper keeps: the board's, or its class's when that asks more. */
    private double clearanceOf(Optional<String> net) {
        double usual = rule(CLEARANCE);
        return net.flatMap(name -> board.netClasses().stream().filter(netClass -> netClass.nets().contains(name)).findFirst())
                .map(netClass -> Math.max(usual, netClass.clearance())).orElse(usual);
    }

    /** The board's own area — inside its edge, out of its cut-outs —, shrunk by the gap copper keeps from the edge. */
    private Optional<Geometry> boardArea() {
        if (board.outline() == null || board.outline().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(evenOdd(board.outline()).buffer(-rule(EDGE_CLEARANCE)));
    }

    /** An area a path closes: its rings joined, a ring inside another adding to it. */
    private Geometry area(String path) {
        return UnaryUnionOp.union(rings(path).stream().map(ring -> (Geometry) GEOMETRY.createPolygon(ring)).toList(), GEOMETRY);
    }

    /** An area a path closes by the even-odd rule: a ring inside another cuts it out — the board's cut-outs. */
    private Geometry evenOdd(String path) {
        Geometry area = GEOMETRY.createPolygon();
        for (LinearRing ring : rings(path)) {
            area = area.symDifference(GEOMETRY.createPolygon(ring));
        }
        return area;
    }

    private List<LinearRing> rings(String path) {
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

    private static Geometry circle(double x, double y, double radius) {
        return GEOMETRY.createPoint(new Coordinate(x, y)).buffer(radius);
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
