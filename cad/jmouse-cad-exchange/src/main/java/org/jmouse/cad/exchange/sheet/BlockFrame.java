package org.jmouse.cad.exchange.sheet;

import org.jmouse.cad.Point;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.circuit.SheetBoundary;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.cad.simulation.elaboration.SheetUnfolding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.jmouse.cad.exchange.sheet.SheetGeometry.BOX_LEAD;
import static org.jmouse.cad.exchange.sheet.SheetGeometry.BOX_PITCH;
import static org.jmouse.cad.exchange.sheet.SheetGeometry.GRID;
import static org.jmouse.cad.exchange.sheet.SheetGeometry.roundUp;
import static org.jmouse.cad.exchange.sheet.SheetGeometry.widest;

/**
 * A block — a sheet placed on another — as the sheet frames it ({@code SchematicDocument.sheetBlockGeometry}) and KiCad
 * draws a hierarchical sheet: its own size when its layout line gives one, else as wide as its ports' names and as tall as
 * its longer edge; each wired port where its line places it, else an output on the right edge and every other port on the
 * left, two grid steps apart from the top, in the first place on its edge no port stands at.
 *
 * @param halfWidth  half the frame's width
 * @param halfHeight half the frame's height
 * @param wired      each port the block wires → the parent's net it is
 * @param ports      each wired port where it stands, in the block's own drawing
 */
public record BlockFrame(double halfWidth, double halfHeight, Map<String, String> wired, List<PortAt> ports) {

    /** A block's least size: a frame that reads as a sheet. */
    private static final double LEAST_WIDTH  = GRID * 6;
    private static final double LEAST_HEIGHT = GRID * 4;

    public BlockFrame {
        wired = Collections.unmodifiableMap(new LinkedHashMap<>(wired));
        ports = List.copyOf(ports);
    }

    /**
     * A port of a block where it stands.
     *
     * @param port  the sheet's port
     * @param place its edge and how far along it
     * @param edge  the point on the frame it stands at: where KiCad's sheet pin is
     * @param lead  the end of the lead the sheet draws out of the frame: where the sheet's wire meets it
     */
    public record PortAt(SheetBoundary.Port port, Placement.PortPlace place, Point edge, Point lead) {
    }

    /**
     * The frame of a block placing {@code sheet}.
     *
     * @throws IllegalArgumentException when the block does not fit its sheet: it wires a port the sheet has not
     */
    public static BlockFrame of(Part block, Placement.PlacedPart item, Circuit sheet) {
        Map<String, String> wired = SheetUnfolding.portsOf(block, sheet);
        List<SheetBoundary.Port> left = new ArrayList<>();
        List<SheetBoundary.Port> right = new ArrayList<>();
        for (SheetBoundary.Port port : sheet.boundary().ports()) {
            if (wired.containsKey(port.net())) {
                (SimulationVocabulary.OUTPUT.equals(port.direction()) ? right : left).add(port);
            }
        }
        double width = item.size() != null ? item.size().x()
                : Math.max(LEAST_WIDTH, roundUp(widest(netsOf(left)) + widest(netsOf(right)) + GRID * 2, BOX_PITCH));
        double height = item.size() != null ? item.size().y() : Math.max(LEAST_HEIGHT, (Math.max(left.size(), right.size()) + 1) * BOX_PITCH);
        double halfWidth = roundUp(width / 2, GRID);
        double halfHeight = roundUp(height / 2, GRID);
        List<PortAt> ports = new ArrayList<>();
        for (Placement.PortPlace place : portPlaces(item, left, right)) {
            SheetBoundary.Port port = sheet.boundary().ports().stream().filter(candidate -> candidate.net().equals(place.port()))
                    .findFirst().orElseThrow();
            ports.add(switch (place.edge()) {
                case LEFT -> portAt(port, place, -halfWidth, -halfHeight + place.along(), -BOX_LEAD, 0);
                case RIGHT -> portAt(port, place, halfWidth, -halfHeight + place.along(), BOX_LEAD, 0);
                case TOP -> portAt(port, place, -halfWidth + place.along(), -halfHeight, 0, -BOX_LEAD);
                case BOTTOM -> portAt(port, place, -halfWidth + place.along(), halfHeight, 0, BOX_LEAD);
            });
        }
        return new BlockFrame(halfWidth, halfHeight, wired, ports);
    }

    private static PortAt portAt(SheetBoundary.Port port, Placement.PortPlace place, double x, double y, double outX, double outY) {
        return new PortAt(port, place, new Point(x, y), new Point(x + outX, y + outY));
    }

    private static List<String> netsOf(List<SheetBoundary.Port> ports) {
        return ports.stream().map(SheetBoundary.Port::net).toList();
    }

    /** Where each wired port stands on the frame: where its layout line places it, else the first free place on its edge. */
    private static List<Placement.PortPlace> portPlaces(Placement.PlacedPart item, List<SheetBoundary.Port> left,
                                                        List<SheetBoundary.Port> right) {
        Map<String, Placement.PortPlace> written = new LinkedHashMap<>();
        item.ports().forEach(place -> written.putIfAbsent(place.port(), place));
        List<Placement.PortPlace> places = new ArrayList<>();
        for (List<SheetBoundary.Port> side : List.of(left, right)) {
            Placement.PortPlace.Edge edge = side == left ? Placement.PortPlace.Edge.LEFT : Placement.PortPlace.Edge.RIGHT;
            Set<Double> taken = new HashSet<>();
            side.stream().map(port -> written.get(port.net())).filter(Objects::nonNull).filter(place -> place.edge() == edge)
                    .forEach(place -> taken.add(place.along()));
            double along = BOX_PITCH;
            for (SheetBoundary.Port port : side) {
                Placement.PortPlace place = written.get(port.net());
                if (place != null) {
                    places.add(place);
                    continue;
                }
                while (taken.contains(along)) {
                    along += BOX_PITCH;
                }
                taken.add(along);
                places.add(new Placement.PortPlace(port.net(), edge, along));
            }
        }
        return places;
    }
}
