package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.FindingSeverity;
import org.jmouse.cad.pcb.checks.PcbChecks;
import org.jmouse.cad.pcb.checks.PcbFinding;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.geometry.PlacedDesignator;
import org.jmouse.cad.pcb.geometry.Teardrops;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The engine's checks of a pcb, on two 0805 resistors the library ships: what a factory would refuse is an error, what a
 * person should look at a warning — each named as the editor names it.
 */
class PcbChecksTest {

    private static final int CURVE_STEPS = 16;
    private static final PcbChecks.Settings SETTINGS = new PcbChecks.Settings(0.005, 40, CURVE_STEPS,
            new PlacedDesignator.Rules(0.6, 0.9, 1.6, 0.25, 0.6), new Teardrops.Shape(8, 0.9, 0.8, 14, 24));

    /** R1's pads stand at 4 5 and 6 5, R2's at 14 5 and 16 5; each pad on a net of its own. */
    private static final Map<String, Map<String, String>> NETS = Map.of("R1", Map.of("1", "in", "2", "a"), "R2", Map.of("1", "b", "2", "out"));

    private static List<PcbFinding> checked(String body) {
        DocumentNode document = CadLanguage.shipped().language().readOne("""
                pcb:checks-probe("Checks probe") {
                    circuit(nowhere)
                    layers(2)
                    place(R1, at: 5 5)
                    place(R2, at: 15 5)
                %s
                }
                """.formatted(body), "checks-probe");
        PcbDrawing pcb = PcbDrawing.of(document);
        FootprintDrawing chip = FootprintDrawing.of(CadLibrary.library().findCoded("FP.CHIP:R0805").orElseThrow());
        Map<String, FootprintDrawing> footprints = Map.of("R1", chip, "R2", chip);
        PcbChecks.Subject subject = new PcbChecks.Subject(pcb, footprints, Map.of("R1", Optional.of("FP.CHIP:R0805"), "R2", Optional.of("FP.CHIP:R0805")),
                NETS, Optional.of(CadStandards.pcbRules().valuesOf(pcb.rules())));
        CopperNets nets = CopperNets.of(pcb, footprints, NETS, new PadOutline(CURVE_STEPS, 6), CURVE_STEPS);
        return PcbChecks.of(subject, nets, SETTINGS);
    }

    private static List<String> said(List<PcbFinding> findings, FindingSeverity severity) {
        return findings.stream().filter(finding -> finding.severity() == severity).map(PcbFinding::message).toList();
    }

    @Test
    void aTrackJoiningTwoNetsIsAShortAndAThinOneHangingInTheAirIsSaid() {
        List<PcbFinding> findings = checked("""
                    edge("M0 0H20V10H0Z")
                    track(top, width: 0.25, from: 6 5, to: 14 5)
                    track(top, width: 0.1, from: 5 8, to: 7 8)
                """);
        List<String> errors = said(findings, FindingSeverity.ERROR);
        assertTrue(errors.contains("Copper joins a and b, which the circuit keeps apart"), errors.toString());
        assertTrue(errors.contains("Track 2 is thinner than the 0.15 mm the PCB is made with"), errors.toString());
        assertEquals(List.of("Track 2 ends at 5, 8 joined to nothing", "Track 2 ends at 7, 8 joined to nothing"), said(findings, FindingSeverity.WARNING));
    }

    @Test
    void copperNearerThanTheClearanceIsMeasuredAndPlaced() {
        // R2's pad 2 reaches up to y 5.7; a track 0.25 wide at 5.95 comes within 0.125 mm of it.
        List<PcbFinding> findings = checked("""
                    edge("M0 0H20V10H0Z")
                    track(top, width: 0.25, from: 15.5 5.95, to: 17.5 5.95)
                """);
        PcbFinding gap = findings.stream().filter(finding -> finding.message().contains("from")).findFirst().orElseThrow();
        assertEquals(FindingSeverity.ERROR, gap.severity());
        assertTrue(gap.message().startsWith("R2 pad 2 (out) is 0.125 mm from Track 1 on top copper"), gap.message());
        assertEquals(List.of("placement:1", "track:0"), gap.keys());
        assertTrue(gap.at().isPresent());
    }

    @Test
    void anEdgeThatDoesNotCloseCannotBeCut() {
        List<String> errors = said(checked("""
                    edge("M0 0H20V10H0")
                """), FindingSeverity.ERROR);
        assertEquals(List.of("The PCB's edge is open between 0, 0 and 0, 10 mm: it cannot be cut"), errors);
    }
}
