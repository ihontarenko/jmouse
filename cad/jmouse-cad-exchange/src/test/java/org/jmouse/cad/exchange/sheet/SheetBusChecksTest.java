package org.jmouse.cad.exchange.sheet;

import org.jmouse.cad.document.FindingSeverity;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.simulation.SimulationReading;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The engine's checks of a sheet's buses: a line drawing a bus the sheet does not declare and a bus port with no bus are errors;
 * an entry standing on no line, or taking off a net its bus does not carry, is a warning; a net a bus carries and nothing is on is
 * a notice.
 */
class SheetBusChecksTest {

    private static final SheetChecks.PinRules RULES = new SheetChecks.PinRules(1,
            Map.of("output", "an output", "power-output", "a power output", "power-input", "a power input", "ground", "a ground pin"), List.of());

    private static List<SheetFinding> checked(String body) {
        var sheet = CadLanguage.shipped().language().readOne("""
                circuit:bus-probe("Bus probe") {
                    ports {
                        bus(D)
                    }
                    buses {
                        bus(D[0..2])
                    }
                    part:R1(resistor, [D_0, D_1])
                    place:R1(at: 600 200, rotate: 90)
                    bus:B1(D, via: [100 100, 400 100])
                %s
                }
                """.formatted(body), "bus-probe");
        return SheetChecks.of(SimulationReading.circuit(sheet), CadLibrary.library(), RULES);
    }

    private static List<String> said(List<SheetFinding> findings, FindingSeverity severity) {
        return findings.stream().filter(finding -> finding.severity() == severity).map(SheetFinding::message).toList();
    }

    @Test
    void aWellDrawnBusSaysOnlyWhatNothingIsOn() {
        List<SheetFinding> findings = checked("mark:E1(entry, D_0, at: 200 100, rotate: 45)");
        assertTrue(said(findings, FindingSeverity.ERROR).isEmpty(), findings.toString());
        assertTrue(said(findings, FindingSeverity.WARNING).stream().noneMatch(message -> message.contains("Entry") || message.contains("Bus")),
                findings.toString());
        assertEquals(List.of("Bus D carries D_2, which nothing on the sheet is on"),
                said(findings, FindingSeverity.NOTICE).stream().filter(message -> message.startsWith("Bus")).toList());
    }

    @Test
    void aLineOfABusTheSheetDoesNotDeclareIsAnError() {
        List<String> errors = said(checked("bus:B2(ADDRESS, via: [100 300, 400 300])"), FindingSeverity.ERROR);
        assertEquals(List.of("Bus line B2 draws the bus ADDRESS, which the sheet does not declare: declare it in its buses block"), errors);
    }

    @Test
    void anEntryOffEveryLineOrOfANetItsBusDoesNotCarryIsAWarning() {
        List<String> warnings = said(checked("""
                mark:E1(entry, D_0, at: 200 250, rotate: 45)
                mark:E2(entry, CLOCK, at: 300 100, rotate: 45)
                """), FindingSeverity.WARNING);
        assertTrue(warnings.contains("Entry E1 stands on no bus line: it takes nothing off a bus"), warnings.toString());
        assertTrue(warnings.contains("Entry E2 takes CLOCK off the bus D, which carries D_0, D_1, D_2"), warnings.toString());
    }

    @Test
    void aBusPortWithNoBusOfItsNameIsAnError() {
        var sheet = CadLanguage.shipped().language().readOne("""
                circuit:bus-port-probe("Bus port probe") {
                    ports {
                        bus(DATA)
                    }
                }
                """, "bus-port-probe");
        List<String> errors = said(SheetChecks.of(SimulationReading.circuit(sheet), CadLibrary.library(), RULES), FindingSeverity.ERROR);
        assertEquals(List.of("The sheet hands the bus DATA across its edge and declares no bus of that name"), errors);
    }
}
