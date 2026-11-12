package org.jmouse.cad.exchange.pcb;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.locationtech.jts.geom.util.PolygonExtracter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The pcb's profile as a fabrication format writes it: the widest area its edge closes, with that area's cut-outs, in
 * the pcb's frame. An edge that closes more than one area is said in {@code notes}: only the widest is the pcb.
 */
public record PcbProfile(List<PcbDrawing.Point> outline, List<List<PcbDrawing.Point>> cutouts, List<String> notes) {

    public PcbProfile {
        outline = List.copyOf(outline);
        cutouts = List.copyOf(cutouts);
        notes = List.copyOf(notes);
    }

    /**
     * The profile of a pcb's edge.
     *
     * @throws IllegalArgumentException when the edge closes no area: such a pcb has no profile to write
     */
    public static PcbProfile of(PcbArtwork drawn) {
        List<Polygon> areas = new ArrayList<>();
        for (Object found : PolygonExtracter.getPolygons(PcbSolid.areaOf(drawn))) {
            areas.add((Polygon) found);
        }
        if (areas.isEmpty()) {
            throw new IllegalArgumentException("The pcb has no closed edge, so it has no profile to write.");
        }
        areas.sort((first, second) -> Double.compare(second.getArea(), first.getArea()));
        List<String> notes = areas.size() > 1
                ? List.of("The pcb's edge closes " + areas.size() + " separate areas; only the largest is written as its profile.")
                : List.of();
        Polygon widest = areas.getFirst();
        List<List<PcbDrawing.Point>> cutouts = new ArrayList<>();
        for (int hole = 0; hole < widest.getNumInteriorRing(); hole++) {
            cutouts.add(ring(widest.getInteriorRingN(hole)));
        }
        return new PcbProfile(ring(widest.getExteriorRing()), cutouts, notes);
    }

    private static List<PcbDrawing.Point> ring(LineString ring) {
        return Arrays.stream(ring.getCoordinates()).map((Coordinate point) -> new PcbDrawing.Point(point.x, point.y)).toList();
    }
}
