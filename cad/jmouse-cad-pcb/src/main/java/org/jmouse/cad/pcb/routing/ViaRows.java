package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.NetRules;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.AreaOffsets;
import org.jmouse.cad.pcb.geometry.Areas;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.union.UnaryUnionOp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rows of vias made over the zones the engine has poured: vias that stitch a zone to its net's pours on other layers, and vias
 * that fence a track, as a radio-frequency line is fenced. A via stands only where its whole pad lies on the copper it joins
 * and keeps its gap from everything else — another net's copper on every layer it passes, every hole by the hole's gap.
 */
public final class ViaRows {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    /** How much further from a track than the gap a fence's vias stand, in millimetres: so a centre stands inside the room, not on its edge. */
    private static final double INSIDE_THE_ROOM = 0.01;

    /** The steps a fence's offset is rounded up to: a hundredth of a millimetre. */
    private static final double OFFSET_STEP = 0.01;

    /** Where vias go, or why none can, in words. */
    public sealed interface Placed permits Placed.Vias, Placed.Refused {

        /** The vias, through the pcb. */
        record Vias(List<PcbDrawing.Via> vias) implements Placed {
        }

        /** Why none can stand. */
        record Refused(String refusal) implements Placed {
        }
    }

    private final PcbDrawing                pcb;
    private final CopperNets                nets;
    private final List<Optional<Geometry>>  pours;
    private final NetRules                  rules;
    private final AreaOffsets               offsets;
    private final int                       curveSteps;

    /**
     * @param pours each zone's copper as poured, by its place in the pcb's list
     * @param curveSteps how many straight pieces a curve is followed in
     */
    public ViaRows(PcbDrawing pcb, CopperNets nets, List<Optional<Geometry>> pours, NetRules rules, AreaOffsets offsets, int curveSteps) {
        this.pcb = pcb;
        this.nets = nets;
        this.pours = List.copyOf(pours);
        this.rules = rules;
        this.offsets = offsets;
        this.curveSteps = curveSteps;
    }

    /** The closest two through vias of a size stand, centre to centre: pads apart, and holes a hole's gap apart. */
    private double viaPitch(PcbDrawing.ViaSize via) {
        return Math.max(via.diameter(), via.drill() + rules.value(PcbVocabulary.RULE_HOLE_CLEARANCE));
    }

    private static String number(double value) {
        return Double.toString(Math.round(value * 1000) / 1000.0).replaceAll("\\.0$", "");
    }

    /** A piece of copper as an area, grown by a gap beyond its own edge. */
    private Geometry copperArea(CopperShape shape, double gap) {
        List<Coordinate> coordinates = new ArrayList<>();
        for (double[] piece : shape.pieces()) {
            if (coordinates.isEmpty()) {
                coordinates.add(new Coordinate(piece[0], piece[1]));
            }
            Coordinate end = new Coordinate(piece[2], piece[3]);
            if (!coordinates.getLast().equals2D(end)) {
                coordinates.add(end);
            }
        }
        if (shape.area() && coordinates.size() >= 3) {
            if (!coordinates.getFirst().equals2D(coordinates.getLast())) {
                coordinates.add(coordinates.getFirst());
            }
            Geometry ring = GEOMETRY.createPolygon(coordinates.toArray(Coordinate[]::new));
            double grownBy = shape.radius() + gap;
            return grownBy > 0 ? offsets.grown(ring, grownBy) : ring;
        }
        Geometry line = coordinates.size() == 1 ? GEOMETRY.createPoint(coordinates.getFirst()) : GEOMETRY.createLineString(coordinates.toArray(Coordinate[]::new));
        return offsets.grown(line, shape.radius() + gap);
    }

    private Geometry circle(double x, double y, double radius) {
        return offsets.grown(GEOMETRY.createPoint(new Coordinate(x, y)), radius);
    }

    /**
     * Where the centre of a through via of a net may stand inside a room: its whole pad on that copper, its copper a gap from
     * every other net's on every layer it passes, its hole a hole's gap from every other hole, and a via of its own net no nearer
     * than their pads allow.
     */
    private Geometry viaRoom(String net, Geometry room, PcbDrawing.ViaSize via) {
        Optional<String> own = Optional.of(net);
        double radius = via.diameter() / 2;
        double holeClearance = rules.value(PcbVocabulary.RULE_HOLE_CLEARANCE);
        java.util.function.Function<Optional<String>, Double> copperGap = other ->
                Math.max(rules.clearanceOf(own), rules.clearanceOf(other)) + radius;
        java.util.function.DoubleUnaryOperator holeGap = drill -> drill / 2 + holeClearance + via.drill() / 2;
        List<Geometry> obstacles = new ArrayList<>();
        for (int index = 0; index < nets.pads().size(); index++) {
            CopperNets.PlacedPad pad = nets.pads().get(index);
            Optional<String> padNet = nets.padNets().get(index);
            if (!padNet.equals(own)) {
                obstacles.add(copperArea(pad.copper(), copperGap.apply(padNet)));
            }
            if (pad.pad().drillWidth() > 0) {
                obstacles.add(circle(pad.centre().x(), pad.centre().y(), holeGap.applyAsDouble(Math.max(pad.pad().drillWidth(), pad.pad().drillHeight()))));
            }
        }
        for (CopperPieces.Piece piece : CopperPieces.of(pcb, nets, List.of(), curveSteps)) {
            boolean conductor = piece.kind() == CopperPieces.Kind.TRACK || piece.kind() == CopperPieces.Kind.ARC || piece.kind() == CopperPieces.Kind.DRAWN;
            if (conductor && !piece.net().equals(own)) {
                obstacles.add(copperArea(piece.shape(), copperGap.apply(piece.net())));
            }
        }
        for (int index = 0; index < pcb.vias().size(); index++) {
            PcbDrawing.Via other = pcb.vias().get(index);
            Optional<String> viaNet = nets.viaNets().get(index);
            double keep = Math.max(holeGap.applyAsDouble(other.drill()), other.diameter() / 2 + (viaNet.equals(own) ? radius : copperGap.apply(viaNet)));
            obstacles.add(circle(other.x(), other.y(), keep));
        }
        for (PcbDrawing.Hole hole : pcb.holes()) {
            obstacles.add(circle(hole.x(), hole.y(), CopperShapes.reach(hole) + Math.max(copperGap.apply(Optional.empty()), holeGap.applyAsDouble(0))));
        }
        for (int index = 0; index < pcb.zones().size(); index++) {
            PcbDrawing.Zone other = pcb.zones().get(index);
            if (!other.net().equals(net) && pours.get(index).isPresent()) {
                obstacles.add(offsets.grown(pours.get(index).get(), copperGap.apply(Optional.of(other.net()))));
            }
        }
        Geometry shrunk = offsets.grown(room, -radius);
        return obstacles.isEmpty() ? shrunk : shrunk.difference(UnaryUnionOp.union(obstacles, GEOMETRY));
    }

    private static PcbDrawing.Via throughVia(double x, double y, PcbDrawing.ViaSize via) {
        return new PcbDrawing.Via(Lines.settled(x), Lines.settled(y), via.drill(), via.diameter(), PcbDrawing.TOP, PcbDrawing.BOTTOM);
    }

    /**
     * A zone stitched to its net's pours on the other layers: through vias on a grid of a pitch, every one where the zone and
     * another layer's pour of its net both lie under its whole pad, clear of everything else. The grid is the pcb's, not the
     * zone's, so stitching again adds only what is missing.
     *
     * @param size the vias' size; empty for the one the zone's net is laid with
     */
    public Placed stitching(int zoneIndex, double pitch, Optional<PcbDrawing.ViaSize> size) {
        if (zoneIndex < 0 || zoneIndex >= pcb.zones().size() || pours.get(zoneIndex).isEmpty()) {
            return new Placed.Refused("Zone " + (zoneIndex + 1) + " has no copper poured yet");
        }
        PcbDrawing.Zone zone = pcb.zones().get(zoneIndex);
        PcbDrawing.ViaSize via = size.orElseGet(() -> rules.viaOf(Optional.of(zone.net())));
        double least = viaPitch(via);
        if (pitch < least) {
            return new Placed.Refused("Vias of " + number(via.diameter()) + " mm stand at least " + number(least) + " mm apart");
        }
        List<Geometry> partners = new ArrayList<>();
        for (int index = 0; index < pcb.zones().size(); index++) {
            PcbDrawing.Zone other = pcb.zones().get(index);
            if (index != zoneIndex && other.net().equals(zone.net()) && !other.layer().equals(zone.layer()) && pours.get(index).isPresent()) {
                partners.add(pours.get(index).get());
            }
        }
        if (partners.isEmpty()) {
            return new Placed.Refused("No other layer pours " + zone.net() + ": a via here would join nothing");
        }
        Geometry room = Areas.common(pours.get(zoneIndex).get(), UnaryUnionOp.union(partners, GEOMETRY));
        Geometry area = viaRoom(zone.net(), room, via);
        if (area.isEmpty()) {
            return new Placed.Vias(List.of());
        }
        PreparedGeometry inside = PreparedGeometryFactory.prepare(area);
        Envelope box = area.getEnvelopeInternal();
        List<PcbDrawing.Via> vias = new ArrayList<>();
        for (long row = (long) Math.ceil(box.getMinY() / pitch); row * pitch <= box.getMaxY(); row++) {
            for (long column = (long) Math.ceil(box.getMinX() / pitch); column * pitch <= box.getMaxX(); column++) {
                double x = Lines.settled(column * pitch);
                double y = Lines.settled(row * pitch);
                if (inside.contains(GEOMETRY.createPoint(new Coordinate(x, y)))) {
                    vias.add(throughVia(x, y, via));
                }
            }
        }
        return new Placed.Vias(vias);
    }

    /**
     * A track fenced: a row of a net's through vias down each side of it, a pitch apart along it, as near as the track's gap and
     * the vias' own pads allow — every one where two layers pour the net under its whole pad, clear of everything else, and a
     * via's pitch from every other via of the fence.
     *
     * @param size the vias' size; empty for the one the net is laid with
     */
    public Placed fence(int trackIndex, String net, double pitch, Optional<PcbDrawing.ViaSize> size) {
        PcbDrawing.ViaSize via = size.orElseGet(() -> rules.viaOf(Optional.of(net)));
        if (trackIndex < 0 || trackIndex >= pcb.tracks().size()) {
            return new Placed.Refused("There is no track " + (trackIndex + 1));
        }
        PcbDrawing.Track track = pcb.tracks().get(trackIndex);
        Optional<String> trackNet = nets.trackNets().get(trackIndex);
        if (trackNet.equals(Optional.of(net))) {
            return new Placed.Refused("Track " + (trackIndex + 1) + " is on " + net + " itself: a fence of its own net guards nothing");
        }
        double least = viaPitch(via);
        if (pitch < least) {
            return new Placed.Refused("Vias of " + number(via.diameter()) + " mm stand at least " + number(least) + " mm apart");
        }
        Map<String, List<Geometry>> byLayer = new LinkedHashMap<>();
        for (int index = 0; index < pcb.zones().size(); index++) {
            PcbDrawing.Zone zone = pcb.zones().get(index);
            if (zone.net().equals(net) && pours.get(index).isPresent()) {
                byLayer.computeIfAbsent(zone.layer(), ignored -> new ArrayList<>()).add(pours.get(index).get());
            }
        }
        List<Geometry> poured = byLayer.values().stream().map(areas -> UnaryUnionOp.union(areas, GEOMETRY)).toList();
        if (poured.size() < 2) {
            return new Placed.Refused("No two layers pour " + net + ": a fence via would join nothing");
        }
        // Where two layers or more pour the net: every pair's overlap.
        List<Geometry> overlaps = new ArrayList<>();
        for (int first = 0; first < poured.size(); first++) {
            for (int second = first + 1; second < poured.size(); second++) {
                overlaps.add(Areas.common(poured.get(first), poured.get(second)));
            }
        }
        PreparedGeometry inside = PreparedGeometryFactory.prepare(viaRoom(net, UnaryUnionOp.union(overlaps, GEOMETRY), via));
        // The pours keep their zone's gap from the track — the wider of it and the track's own — and the fence stands past it.
        double gap = rules.clearanceOf(trackNet);
        for (PcbDrawing.Zone zone : pcb.zones()) {
            if (zone.net().equals(net)) {
                gap = Math.max(gap, rules.zoneGapOf(zone));
            }
        }
        // As near the track as the room lets a via's centre stand: the pour kept its gap from the track, and the room its pad from the
        // pour's edge, each grown by the arc error so the gap is kept exactly — then a hundredth further, inside the room, not on its edge.
        double nearest = track.width() / 2 + gap + via.diameter() / 2 + 2 * offsets.arcError();
        double offset = Math.ceil(nearest / OFFSET_STEP) * OFFSET_STEP + INSIDE_THE_ROOM;
        List<PcbDrawing.Via> vias = new ArrayList<>();
        for (int side : new int[] {1, -1}) {
            // The distance along the track is carried from segment to segment, so the pitch runs on round a corner.
            double carried = 0;
            for (int segment = 0; segment < track.points().size() - 1; segment++) {
                PcbDrawing.Point from = track.points().get(segment);
                PcbDrawing.Point to = track.points().get(segment + 1);
                double length = Lines.lengthBetween(from, to);
                if (length == 0) {
                    continue;
                }
                double unitX = (to.x() - from.x()) / length;
                double unitY = (to.y() - from.y()) / length;
                double along = carried;
                for (; along <= length; along += pitch) {
                    double x = from.x() + unitX * along + unitY * side * offset;
                    double y = from.y() + unitY * along - unitX * side * offset;
                    boolean clearOfFence = vias.stream().allMatch(other -> Math.hypot(other.x() - x, other.y() - y) >= least);
                    if (inside.contains(GEOMETRY.createPoint(new Coordinate(x, y))) && clearOfFence) {
                        vias.add(throughVia(x, y, via));
                    }
                }
                carried = along - length;
            }
        }
        return new Placed.Vias(vias);
    }
}
