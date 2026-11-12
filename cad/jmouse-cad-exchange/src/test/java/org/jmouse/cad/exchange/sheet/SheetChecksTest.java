package org.jmouse.cad.exchange.sheet;

import org.jmouse.cad.Point;
import org.jmouse.cad.document.FindingSeverity;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.simulation.SimulationReading;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The engine's checks of a sheet, on two Schmitt inverters and a resistor the library ships: outputs that drive one net
 * are an error, supply pins nothing powers and a pin joined to nothing are warnings — and a no-connect mark on that pin
 * says it is open on purpose.
 */
class SheetChecksTest {

    private static final SheetChecks.PinRules RULES = new SheetChecks.PinRules(1,
            Map.of("output", "an output", "power-output", "a power output", "power-input", "a power input", "ground", "a ground pin"),
            List.of(new SheetChecks.PinRules.Conflict(List.of("output", "output"), "error"),
                    new SheetChecks.PinRules.Conflict(List.of("output", "power-output"), "error")));

    private static List<SheetFinding> checked(String marks) {
        var sheet = CadLanguage.shipped().language().readOne("""
                circuit:checks-probe("Checks probe") {
                    part:U1(schmitt-inverter, [a: in, output: y, Vcc: vcc, GND: 0])
                    part:U2(schmitt-inverter, [a: in, output: y, Vcc: vcc, GND: 0])
                    part:R1(resistor, [in, lone])
                    place:U1(at: 200 200)
                    place:U2(at: 200 400)
                    place:R1(at: 600 200, rotate: 90)
                    mark:LX(label, nowhere, at: 900 900)
                %s
                }
                """.formatted(marks), "checks-probe");
        return SheetChecks.of(SimulationReading.circuit(sheet), CadLibrary.library(), RULES);
    }

    private static List<String> said(List<SheetFinding> findings, FindingSeverity severity) {
        return findings.stream().filter(finding -> finding.severity() == severity).map(SheetFinding::message).toList();
    }

    @Test
    void outputsSharingANetAreAnError() {
        List<SheetFinding> findings = checked("");
        assertEquals(List.of("Net y joins 2 × an output: U1.output, U2.output"), said(findings, FindingSeverity.ERROR));
        SheetFinding error = findings.stream().filter(finding -> finding.severity() == FindingSeverity.ERROR).findFirst().orElseThrow();
        assertEquals(List.of("item:U1", "item:U2"), error.keys());
    }

    @Test
    void anUnpoweredSupplyAndAnOpenPinAreWarnings() {
        List<String> warnings = said(checked(""), FindingSeverity.WARNING);
        assertTrue(warnings.contains("Net vcc takes power and nothing on it gives any: U1.Vcc, U2.Vcc"), warnings.toString());
        assertTrue(warnings.contains("R1.negative is joined to nothing: wire it, or mark it no-connect"), warnings.toString());
    }

    @Test
    void aLabelOnNothingIsSaid() {
        assertEquals(List.of("Label nowhere is on nothing else: it joins nothing"), said(checked(""), FindingSeverity.NOTICE));
    }

    @Test
    void aNoConnectMarkOnAnOpenPinSaysItIsOpenOnPurpose() {
        Point open = checked("").stream().filter(finding -> finding.message().startsWith("R1.negative is joined to nothing"))
                .findFirst().orElseThrow().at().orElseThrow();
        String mark = String.format(Locale.ROOT, "    mark:NC1(no-connect, at: %s %s)", open.x(), open.y());
        List<String> warnings = said(checked(mark), FindingSeverity.WARNING);
        assertTrue(warnings.stream().noneMatch(warning -> warning.startsWith("R1.negative")), warnings.toString());
    }

    @Test
    void aNoConnectMarkOnAJoinedPinIsAWarning() {
        Point open = checked("").stream().filter(finding -> finding.message().startsWith("R1.negative is joined to nothing"))
                .findFirst().orElseThrow().at().orElseThrow();
        // R1.positive is on net in with both inverters' inputs: a mark beside it, half a resistor away, is on that pin.
        Point joined = new Point(open.x(), 2 * 200 - open.y());
        String mark = String.format(Locale.ROOT, "    mark:NC1(no-connect, at: %s %s)", joined.x(), joined.y());
        List<String> warnings = said(checked(mark), FindingSeverity.WARNING);
        assertTrue(warnings.contains("R1.positive is marked no-connect, and it is on net in"), warnings.toString());
    }
}
