package org.jmouse.cad.exchange.pcb;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.Areas;
import org.jmouse.cad.pcb.geometry.PcbDrills;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A pcb as it is made, as areas: the substrate cut to its edge and drilled, and on each outer side the copper, where the mask
 * opens, the copper it leaves bare, the laminate it shows where it opens over no copper, the mask itself, the paste, and the
 * silkscreen printed on the mask — and each part's body where its part stands. The engine's one picture of a made pcb: the
 * 3D files are raised from it, and every view of the pcb as made — the editor's mask, its 3D view — draws it.
 *
 * @param substrate the pcb's area less every hole
 * @param sides     each outer side, {@code top} then {@code bottom}
 * @param bodies    each part's body, its height where its footprint gives one
 */
public record PcbSolid(Geometry substrate, Map<String, Side> sides, List<PartBodies.PartBody> bodies) {

    private static final GeometryFactory AREAS = new GeometryFactory();

    /**
     * One outer side as made. On a side the stack has no copper on, only its mask and its silkscreen are there.
     *
     * @param copper     every piece of copper on the side
     * @param openings   where the mask opens: over pads, bare copper, and what is drawn on the mask
     * @param bare       the copper the mask leaves bare: what the finish covers
     * @param laminate   where the mask opens over no copper: the laminate shows
     * @param mask       the mask itself
     * @param paste      the paste on the side's pads
     * @param silkscreen the ink printed on the mask
     */
    public record Side(Geometry copper, Geometry openings, Geometry bare, Geometry laminate, Geometry mask, Geometry paste, Geometry silkscreen) {
    }

    public PcbSolid {
        sides = Map.copyOf(sides);
        bodies = List.copyOf(bodies);
    }

    /**
     * The pcb as made.
     *
     * @param areas     how the artwork's features become areas
     * @param drilled   every hole the pcb is drilled with
     * @param drillSteps how many straight steps a quarter of a drill's circle is drawn with
     */
    public static PcbSolid of(PcbArtwork artwork, ArtworkAreas areas, List<PcbDrills.Drill> drilled, int drillSteps, PartBodies bodies) {
        Geometry pcb = areaOf(artwork);
        Geometry holes = drillArea(drilled, drillSteps);
        List<String> stack = artwork.fabrication().pcb().copperLayerIds();
        Map<String, Side> sides = new LinkedHashMap<>();
        for (String side : List.of(PcbDrawing.TOP, PcbDrawing.BOTTOM)) {
            if (!stack.contains(side)) {
                Geometry mask = pcb.difference(holes);
                Geometry nothing = AREAS.createPolygon();
                sides.put(side, new Side(nothing, nothing, nothing, nothing, mask, nothing, Areas.common(areas.areaOf(artwork.silkscreen(side)), mask)));
                continue;
            }
            Geometry copper = Areas.common(areas.areaOf(artwork.copper(side)), pcb).difference(holes);
            Geometry openings = Areas.common(areas.areaOf(artwork.maskOpenings(side)), pcb);
            Geometry mask = pcb.difference(openings).difference(holes);
            sides.put(side, new Side(copper, openings, Areas.common(copper, openings), openings.difference(copper).difference(holes), mask,
                    Areas.common(areas.areaOf(artwork.paste(side)), pcb), Areas.common(areas.areaOf(artwork.silkscreen(side)), mask)));
        }
        List<PartBodies.PartBody> standing = new ArrayList<>();
        artwork.fabrication().parts().forEach(part -> bodies.of(part).ifPresent(standing::add));
        return new PcbSolid(pcb.difference(holes), sides, standing);
    }

    /** The pcb's area: its edge's closed runs, an inner run a cut-out of the run around it. */
    public static Geometry areaOf(PcbArtwork artwork) {
        Geometry area = AREAS.createPolygon();
        for (List<double[]> run : artwork.runs(artwork.fabrication().pcb().outline())) {
            List<Coordinate> ring = new ArrayList<>(run.stream().map(point -> new Coordinate(point[0], point[1])).toList());
            if (ring.size() < 3) {
                continue;
            }
            if (!ring.getFirst().equals2D(ring.getLast())) {
                ring.add(ring.getFirst());
            }
            area = area.symDifference(AREAS.createPolygon(ring.toArray(Coordinate[]::new)).buffer(0));
        }
        return area;
    }

    /** Every hole's area: a drill's circle, a slot the tool's width along its line. */
    public static Geometry drillArea(List<PcbDrills.Drill> drilled, int drillSteps) {
        Geometry area = AREAS.createPolygon();
        for (PcbDrills.Drill drill : drilled) {
            Coordinate from = new Coordinate(drill.from().x(), drill.from().y());
            Coordinate to = new Coordinate(drill.to().x(), drill.to().y());
            Geometry line = from.equals2D(to) ? AREAS.createPoint(from) : AREAS.createLineString(new Coordinate[] {from, to});
            area = area.union(line.buffer(drill.width() / 2, drillSteps));
        }
        return area;
    }

    /** A side by its name. */
    public Optional<Side> side(String name) {
        return Optional.ofNullable(sides.get(name));
    }
}
