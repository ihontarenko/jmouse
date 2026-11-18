package org.jmouse.cad.pcb.layout;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A circuit's pcb laid out from its sheet: courtyards apart, parts on the grid inside the edge, the bench's nets on a header of
 * their own, the ground poured, and a sheet drawn in one row not blown up. Moved from the editor's own tests with the layout
 * on 2026-11-12: a new pcb is the engine's.
 */
class CircuitLayoutTest {

    private static final PcbLayout LAYOUT = read();

    private static final CircuitLayout.Settings SETTINGS = LAYOUT.circuit();

    private static PcbLayout read() {
        try (InputStream stream = PcbLayout.class.getResourceAsStream(PcbLayout.SECTION.resource())) {
            return JsonMapper.builder().build().readValue(stream, PcbLayout.class);
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static FootprintDrawing.Pad pad(String number, double x, double y) {
        return new FootprintDrawing.Pad(number, "through-hole", "circle", x, y, 0, 1.6, 1.6, 0.8, 0.8, 0, "", "", "", null, null);
    }

    /** A two-lead footprint so long, its courtyard round it. */
    private static FootprintDrawing twoLead(double length) {
        String courtyard = "M" + (-length / 2 - 1.5) + " -1.5H" + (length / 2 + 1.5) + "V1.5H" + (-length / 2 - 1.5) + "Z";
        return new FootprintDrawing(List.of(pad("1", -length / 2, 0), pad("2", length / 2, 0)),
                List.of(new FootprintDrawing.Outline("courtyard", 0.05, false, courtyard)), List.of(), null, List.of());
    }

    private static FootprintDrawing header(int pins) {
        List<FootprintDrawing.Pad> pads = new ArrayList<>();
        for (int index = 0; index < pins; index++) {
            pads.add(pad(Integer.toString(index + 1), 0, index * 2.54));
        }
        return new FootprintDrawing(pads, List.of(new FootprintDrawing.Outline("courtyard", 0.05, false, "M-1.5 -1.5H1.5V" + ((pins - 1) * 2.54 + 1.5) + "H-1.5Z")),
                List.of(), null, List.of());
    }

    private static final Map<String, FootprintDrawing> FOOTPRINTS = Map.of("axial", twoLead(10), SETTINGS.connector().footprintFor(2), header(2));

    private static final List<CircuitLayout.BoardPart> PARTS = List.of(
            new CircuitLayout.BoardPart("R1", Optional.of("axial"), Map.of("1", "in", "2", "mid")),
            new CircuitLayout.BoardPart("R2", Optional.of("axial"), Map.of("1", "mid", "2", "0")),
            new CircuitLayout.BoardPart("R3", Optional.of("axial"), Map.of("1", "mid", "2", "0")));

    private static PcbDrawing laidOut(Map<String, CircuitLayout.SheetPlace> places, List<String> benchNets, Optional<String> ground) {
        return new CircuitLayout(LAYOUT).laidOut(new CircuitLayout.Request("divider", PARTS, FOOTPRINTS, places, benchNets, ground));
    }

    /** The box a placed part's courtyard takes, its corners turned and set where it stands: what the layout keeps apart. */
    private static double[] boxOf(Placement placement) {
        // A resistor's courtyard round its 10 mm, the header's round its two pins.
        double[][] corners = placement.designator().startsWith("R") ? new double[][] {{-6.5, -1.5}, {6.5, 1.5}} : new double[][] {{-1.5, -1.5}, {1.5, 2.54 + 1.5}};
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (double[] corner : new double[][] {corners[0], {corners[1][0], corners[0][1]}, {corners[0][0], corners[1][1]}, corners[1]}) {
            Point placed = placement.placed(corner[0], corner[1]);
            left = Math.min(left, placed.x());
            top = Math.min(top, placed.y());
            right = Math.max(right, placed.x());
            bottom = Math.max(bottom, placed.y());
        }
        return new double[] {left, top, right, bottom};
    }

    @Test
    void keepsEveryCourtyardApartEveryPartOnTheGridAndInsideTheEdge() {
        PcbDrawing pcb = laidOut(Map.of("R1", new CircuitLayout.SheetPlace(100, 100, 0), "R2", new CircuitLayout.SheetPlace(120, 110, 0),
                "R3", new CircuitLayout.SheetPlace(125, 105, 90)), List.of("0", "in"), Optional.of("0"));
        List<double[]> boxes = pcb.placements().stream().map(CircuitLayoutTest::boxOf).toList();
        for (int first = 0; first < boxes.size(); first++) {
            for (int second = first + 1; second < boxes.size(); second++) {
                double[] a = boxes.get(first);
                double[] b = boxes.get(second);
                double apartX = Math.max(a[0], b[0]) - Math.min(a[2], b[2]);
                double apartY = Math.max(a[1], b[1]) - Math.min(a[3], b[3]);
                assertTrue(Math.max(apartX, apartY) >= SETTINGS.gap() - 1e-9, pcb.placements().get(first).designator() + " and "
                        + pcb.placements().get(second).designator());
            }
        }
        for (Placement placement : pcb.placements()) {
            assertEquals(0, Math.abs(placement.x() / SETTINGS.grid() - Math.round(placement.x() / SETTINGS.grid())), 1e-9);
            assertEquals(0, Math.abs(placement.y() / SETTINGS.grid() - Math.round(placement.y() / SETTINGS.grid())), 1e-9);
        }
        assertTrue(boxes.stream().mapToDouble(box -> box[0]).min().orElseThrow() >= SETTINGS.margin() - 1e-9);
    }

    @Test
    void bringsTheBenchsNetsOutOnAHeaderOfItsOwnTheGroundFirstAndPoursTheGround() {
        PcbDrawing pcb = laidOut(Map.of("R1", new CircuitLayout.SheetPlace(100, 100, 0), "R2", new CircuitLayout.SheetPlace(120, 110, 0),
                "R3", new CircuitLayout.SheetPlace(125, 105, 0)), List.of("0", "in"), Optional.of("0"));
        Placement connector = pcb.placements().stream().filter(placement -> placement.designator().equals("J1")).findFirst().orElseThrow();
        assertEquals(Map.of("1", "0", "2", "in"), connector.nets());
        assertEquals(Optional.of(SETTINGS.connector().footprintFor(2)), connector.footprint());
        assertEquals(1, pcb.zones().size());
        assertEquals(PcbVocabulary.BOTTOM, pcb.zones().getFirst().layer());
        assertEquals("0", pcb.zones().getFirst().net());
    }

    @Test
    void doesNotBlowASheetDrawnInOneRowUpForItsWantOfHeight() {
        PcbDrawing pcb = laidOut(Map.of("R1", new CircuitLayout.SheetPlace(100, 200, 0), "R2", new CircuitLayout.SheetPlace(300, 200, 0),
                "R3", new CircuitLayout.SheetPlace(500, 200, 0)), List.of(), Optional.empty());
        assertTrue(pcb.placements().stream().map(CircuitLayoutTest::boxOf).mapToDouble(box -> box[2]).max().orElseThrow() < 80);
        assertEquals(0, pcb.zones().size());
    }
}
