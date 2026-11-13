package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.geometry.ZonePours;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The engine's pours — the only ones: the editor draws what the engine pours — on two 0805 resistors the library ships.
 * A zone keeps its gap from copper of other nets, reaches its own pads across thermal spokes, loses islands that join
 * nothing, and a zone of higher priority is poured first and kept clear of.
 */
class ZonePoursTest {

    private static final int CURVE_STEPS = 16;

    /** The arc error the artwork pours with (`pcb-artwork.json`). */
    private static final double ARC_ERROR = 0.005;

    /** R1's pads stand at 4 5 and 6 5, R2's at 14 5 and 16 5. */
    private static final Map<String, Map<String, String>> NETS = Map.of("R1", Map.of("1", "in", "2", "a"), "R2", Map.of("1", "a", "2", "out"));

    private record Poured(PcbDrawing pcb, CopperNets nets, Map<String, Double> rules, List<Optional<Geometry>> pours) {
    }

    private static Poured poured(String body) {
        DocumentNode document = CadLanguage.shipped().language().readOne("""
                pcb:pour-probe("Pour probe") {
                    circuit(nowhere)
                    layers(2)
                    edge("M0 0H20V10H0Z")
                    place(R1, at: 5 5)
                    place(R2, at: 15 5)
                %s
                }
                """.formatted(body), "pour-probe");
        PcbDrawing pcb = PcbDrawing.of(document);
        FootprintDrawing chip = FootprintDrawing.of(CadLibrary.library().findCoded("FP.CHIP:R0805").orElseThrow());
        CopperNets nets = CopperNets.of(pcb, Map.of("R1", chip, "R2", chip), NETS, new PadOutline(CURVE_STEPS, 6), CURVE_STEPS);
        Map<String, Double> rules = CadStandards.shipped().pcbRules().valuesOf(pcb.rules());
        return new Poured(pcb, nets, rules, new ZonePours(pcb, nets, rules, CURVE_STEPS, ARC_ERROR).pour());
    }

    private static Geometry padOf(Poured poured, String designator, String number) {
        return poured.nets().pads().stream().filter(pad -> pad.designator().equals(designator) && pad.pad().number().equals(number))
                .findFirst().orElseThrow().shape();
    }

    @Test
    void aZoneKeepsItsGapFromOtherNetsAndReachesItsOwnPadsAcrossSpokes() {
        Poured poured = poured("""
                    zone(top, net: a, "M1 1H19V9H1Z")
                """);
        Geometry pour = poured.pours().getFirst().orElseThrow();
        double clearance = poured.rules().get("clearance");
        for (String[] other : new String[][] {{"R1", "1"}, {"R2", "2"}}) {
            double gap = pour.distance(padOf(poured, other[0], other[1]));
            assertTrue(gap >= clearance, other[0] + " pad " + other[1] + " is " + gap + " mm from the pour, under the " + clearance + " mm clearance");
        }
        for (String[] own : new String[][] {{"R1", "2"}, {"R2", "1"}}) {
            Geometry pad = padOf(poured, own[0], own[1]);
            Geometry ring = pad.buffer(poured.rules().get("thermal-gap")).difference(pad);
            // The ring round the pad is cut out but for the four spokes across it, which join the pad to the pour.
            assertTrue(pour.intersection(ring).getArea() < ring.getArea() / 2, own[0] + " pad " + own[1] + " is cut out across its thermal gap");
            assertTrue(pour.intersects(pad), own[0] + " pad " + own[1] + " is reached by its spokes");
        }
    }

    @Test
    void anIslandThatJoinsNothingIsTakenAway() {
        // A zone of a net no pad of the board is on reaches nothing to join.
        Poured poured = poured("""
                    zone(top, net: elsewhere, "M1 1H19V9H1Z")
                """);
        assertEquals(Optional.empty(), poured.pours().getFirst());
    }

    @Test
    void theHigherPriorityIsPouredFirstAndKeptClearOf() {
        Poured poured = poured("""
                    zone(top, net: a, priority: 1, "M1 1H19V9H1Z")
                    zone(top, net: out, "M1 1H19V9H1Z")
                """);
        Geometry first = poured.pours().getFirst().orElseThrow();
        poured.pours().get(1).ifPresent(second -> assertTrue(first.distance(second) >= poured.rules().get("clearance") - 1e-6,
                "the lower zone keeps its gap from the higher"));
        assertTrue(first.getArea() > poured.pours().get(1).map(Geometry::getArea).orElse(0.0), "the higher zone takes the board");
    }
}
