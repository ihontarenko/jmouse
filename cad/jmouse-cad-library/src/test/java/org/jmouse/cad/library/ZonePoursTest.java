package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.geometry.PourMemory;
import org.jmouse.cad.pcb.geometry.ZonePours;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The engine's pours — the only ones: the editor draws what the engine pours — on two 0805 resistors the library ships.
 * A zone keeps its gap from copper of other nets, reaches its own pads across thermal spokes, loses islands that join
 * nothing, and a zone of higher priority is poured first and kept clear of. Every piece of copper of another net is kept clear of
 * as copper, whatever it is: a zone drawn inside another, copper drawn as a shape.
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
        return poured(body, PourMemory.none());
    }

    private static Poured poured(String body, PourMemory memory) {
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
        FootprintDrawing chip = FootprintDrawing.of(CadLibrary.library().findCoded("CHIP:R0805:F").orElseThrow());
        CopperNets nets = CopperNets.of(pcb, Map.of("R1", chip, "R2", chip), NETS, new PadOutline(CURVE_STEPS, 6), CURVE_STEPS);
        Map<String, Double> rules = CadStandards.shipped().pcbRules().valuesOf(pcb.rules());
        return new Poured(pcb, nets, rules, new ZonePours(pcb, nets, CopperPieces.of(pcb, nets, List.of(), CURVE_STEPS), rules, CURVE_STEPS, ARC_ERROR, memory).pour());
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

    /** A zone drawn inside another, of another net, at the same priority: it is what is asked for there — poured, and given way to. */
    @Test
    void aZoneOfAnotherNetDrawnInsideAnotherIsPouredAndGivenWayTo() {
        Poured poured = poured("""
                    zone(top, net: a, "M1 1H19V9H1Z")
                    zone(top, net: out, "M14.5 2H18.5V8.5H14.5Z")
                    track(top, width: 0.4, from: 16 5, to: 17.8 7.6)
                """);
        Geometry outer = poured.pours().getFirst().orElseThrow();
        Geometry inner = poured.pours().get(1).orElseThrow(() -> new AssertionError("the zone drawn inside is not poured"));
        double clearance = poured.rules().get("clearance");
        assertTrue(outer.distance(inner) >= clearance - 1e-6, "the outer zone keeps its gap from the inner: " + outer.distance(inner) + " mm");
        assertTrue(inner.getArea() > 4, "the inner zone takes its area: " + inner.getArea() + " mm²");
    }

    /** Copper drawn as a shape is copper: a pour of another net keeps its gap from it, as from a track. */
    @Test
    void copperDrawnAsAShapeOfAnotherNetIsKeptClearOf() {
        Poured poured = poured("""
                    zone(top, net: a, "M1 1H19V9H1Z")
                    shape(top, filled, net: out, "M9 2H11V8H9Z")
                """);
        Geometry pour = poured.pours().getFirst().orElseThrow();
        Geometry block = new GeometryFactory().toGeometry(new Envelope(9, 11, 2, 8));
        double clearance = poured.rules().get("clearance");
        assertTrue(pour.distance(block) >= clearance - 1e-6, "the pour comes " + pour.distance(block) + " mm from the drawn copper");
    }

    /**
     * A zone poured again after a change, from what it was poured from before, is the copper a pour from nothing makes: worked out
     * again only round the track that moved, kept as it was elsewhere.
     */
    @Test
    void aChangePoursAgainOnlyWhereItReachedAndPoursTheSameCopper() {
        PourMemory memory = PourMemory.keeping(8);
        String zone = "    zone(top, net: a, \"M1 1H19V9H1Z\")\n";
        poured(zone + "    track(top, width: 0.4, from: 16 5, to: 17.5 8.5)\n", memory);
        // Moved; moved elsewhere; asked again unchanged — the last kept whole —; then copper of other nets come and gone at angles,
        // whose changes meet the pour at the corners of the boxes they are worked out in.
        for (String moved : new String[] {"    track(top, width: 0.4, from: 16 5, to: 18.5 7)\n", "    track(top, width: 0.4, from: 16 5, to: 15 8.5)\n",
                                          "    track(top, width: 0.4, from: 16 5, to: 15 8.5)\n",
                                          "    track(top, width: 0.4, from: 16 5, to: 15 8.5)\n    track(top, width: 0.3, from: 4 5, to: 2 2)\n",
                                          "    track(top, width: 0.3, from: 4 5, to: 2.6 2.4)\n    track(top, width: 0.3, from: 6 5, to: 9 8)\n",
                                          "    track(top, width: 0.3, from: 6 5, to: 9.4 8.2)\n"}) {
            Geometry kept = poured(zone + moved, memory).pours().getFirst().orElseThrow();
            Geometry fresh = poured(zone + moved).pours().getFirst().orElseThrow();
            double apart = kept.symDifference(fresh).getArea();
            assertTrue(apart < 1e-6, "the pour worked out where the track moved differs from a whole pour by " + apart + " mm²");
        }
    }
}
