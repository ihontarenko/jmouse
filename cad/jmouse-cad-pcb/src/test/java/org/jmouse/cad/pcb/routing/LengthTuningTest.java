package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.checks.PcbChecks;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.geometry.PlacedDesignator;
import org.jmouse.cad.pcb.geometry.Teardrops;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tune length, as a person uses it: a differential pair, the shorter track pointed at, and the two come out the same length.
 * Moved from the editor's own tests with the tuning on 2026-11-12: the editor asks the engine.
 */
class LengthTuningTest {

    private static final RoutingSettings SETTINGS = read();

    private static final Map<String, Double> VALUES = Map.of("clearance", 0.2, "track-width", 0.25, "least-track-width", 0.15, "edge-clearance", 0.3);

    private static final FootprintDrawing FOOTPRINT = new FootprintDrawing(List.of(
            new FootprintDrawing.Pad("1", "surface", "rectangle", 0, 0, 0, 1, 1, 0, 0, 0, "", "", "", null, null),
            new FootprintDrawing.Pad("2", "surface", "rectangle", 0, 1.5, 0, 1, 1, 0, 0, 0, "", "", "", null, null)), List.of(), List.of(), null, List.of());

    private static final List<PcbDrawing.Pair> PAIRS = List.of(new PcbDrawing.Pair("D", 0.25, 0.2, 0, "D_P", "D_N"));

    private static final PcbDrawing.Track LONGER = line(0, 0, 20, 0, 20, -6, 30, -6, 30, 0, 40, 0);

    private static final PcbDrawing.Track SHORTER = line(0, 1.5, 40, 1.5);

    private static RoutingSettings read() {
        try (InputStream stream = RoutingSettings.class.getResourceAsStream(RoutingSettings.SECTION.resource())) {
            return JsonMapper.builder().build().readValue(stream, RoutingSettings.class);
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static PcbDrawing.Track line(double... coordinates) {
        List<Point> points = new java.util.ArrayList<>();
        for (int index = 0; index < coordinates.length; index += 2) {
            points.add(new Point(coordinates[index], coordinates[index + 1]));
        }
        return new PcbDrawing.Track(PcbDrawing.TOP, 0.25, 0, points);
    }

    private static double lengthOf(List<Point> points) {
        return Lines.lengthOf(points);
    }

    private static LengthTuning tuningOf(List<PcbDrawing.Track> tracks, List<PcbDrawing.Pair> pairs) {
        List<PcbDrawing.Placement> placements = List.of(new PcbDrawing.Placement("A", 0, 0, 0, PcbDrawing.TOP, Optional.empty()),
                new PcbDrawing.Placement("B", 40, 0, 0, PcbDrawing.TOP, Optional.empty()));
        PcbDrawing pcb = new PcbDrawing("circuit", Optional.empty(), List.of(), null, List.of(), pairs, 2, "M-100 -100H100V100H-100Z", placements,
                tracks, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        Map<String, Map<String, String>> parts = Map.of("A", Map.of("1", "D_P", "2", "D_N"), "B", Map.of("1", "D_P", "2", "D_N"));
        RoutingBoard board = new RoutingBoard(Map.of("A", FOOTPRINT, "B", FOOTPRINT), Map.of("A", Optional.of("FP"), "B", Optional.of("FP")), parts,
                VALUES, Map.of(), new PcbChecks.Settings(0.005, 6, 16, new PlacedDesignator.Rules(0.6, 0.9, 1.6, 0.25, 0.6),
                new Teardrops.Shape(8, 0.9, 0.8, 14, 24)), new PadOutline(16, 6), SETTINGS);
        return new LengthTuning(new RoutingWorld(pcb, board));
    }

    @Test
    void lengthensTheShorterTrackOfAPairToItsPartner() {
        LengthTuning.Tuned tuned = assertInstanceOf(LengthTuning.Tuned.class,
                tuningOf(List.of(LONGER, SHORTER), PAIRS).tunedAt(PcbDrawing.TOP, new Point(20, 1.5), 0.5, OptionalDouble.empty()));
        assertTrue(Math.abs(lengthOf(tuned.points()) - lengthOf(LONGER.points())) < 1);
        assertEquals(new Point(0, 1.5), tuned.points().getFirst());
        assertEquals(new Point(40, 1.5), tuned.points().getLast());
    }

    @Test
    void lengthensAnyTrackToALengthAskedFor() {
        LengthTuning tuning = tuningOf(List.of(SHORTER), List.of());
        LengthTuning.Tuned tuned = assertInstanceOf(LengthTuning.Tuned.class,
                tuning.tunedAt(PcbDrawing.TOP, new Point(20, 1.5), 0.5, OptionalDouble.of(50)));
        assertTrue(Math.abs(lengthOf(tuned.points()) - 50) < 1);
        LengthTuning.Refused already = assertInstanceOf(LengthTuning.Refused.class,
                tuning.tunedAt(PcbDrawing.TOP, new Point(20, 1.5), 0.5, OptionalDouble.of(30)));
        assertTrue(already.refusal().contains("long already"), already.refusal());
    }

    @Test
    void saysWhyWhenItCannot() {
        LengthTuning paired = tuningOf(List.of(LONGER, SHORTER), PAIRS);
        assertTrue(assertInstanceOf(LengthTuning.Refused.class, paired.tunedAt(PcbDrawing.TOP, new Point(10, 0), 0.5, OptionalDouble.empty()))
                .refusal().contains("already as long"));
        String noPair = assertInstanceOf(LengthTuning.Refused.class, tuningOf(List.of(LONGER, SHORTER), List.of())
                .tunedAt(PcbDrawing.TOP, new Point(10, 0), 0.5, OptionalDouble.empty())).refusal();
        assertTrue(noPair.contains("no differential pair") && noPair.contains("write the length"), noPair);
        assertTrue(assertInstanceOf(LengthTuning.Refused.class, paired.tunedAt(PcbDrawing.TOP, new Point(10, 40), 0.5, OptionalDouble.empty()))
                .refusal().contains("Point at a track"));
    }
}
