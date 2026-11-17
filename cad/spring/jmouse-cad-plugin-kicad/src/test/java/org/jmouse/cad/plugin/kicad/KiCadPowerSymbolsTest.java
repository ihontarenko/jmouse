package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.kicad.InstalledKiCadLibraries;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.KiCadLibraryMap;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.simulation.SimulationReading;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * KiCad's power symbols as KiCad joins them: one whose pin takes power names its net across the design, two of them on one wire
 * are one net, named once; KiCad's power flag — its pin gives power — names no net at all: it tells ERC the net is fed.
 */
class KiCadPowerSymbolsTest {

    private static final String SYMBOLS = """
            (lib_symbols
              (symbol "power:VDD" (power) (in_bom yes) (on_board yes)
                (property "Reference" "#PWR" (at 0 0 0)) (property "Value" "VDD" (at 0 0 0))
                (symbol "VDD_0_1" (pin power_in line (at 0 0 90) (length 0) (name "VDD" (effects)) (number "1" (effects)))))
              (symbol "power:+3V3" (power) (in_bom yes) (on_board yes)
                (property "Reference" "#PWR" (at 0 0 0)) (property "Value" "+3V3" (at 0 0 0))
                (symbol "+3V3_0_1" (pin power_in line (at 0 0 90) (length 0) (name "+3V3" (effects)) (number "1" (effects)))))
              (symbol "power:PWR_FLAG" (power) (in_bom yes) (on_board yes)
                (property "Reference" "#FLG" (at 0 0 0)) (property "Value" "PWR_FLAG" (at 0 0 0))
                (symbol "PWR_FLAG_0_1" (pin power_out line (at 0 0 90) (length 0) (name "pwr" (effects)) (number "1" (effects)))))
              (symbol "Device:R" (in_bom yes) (on_board yes)
                (property "Reference" "R" (at 0 0 0)) (property "Value" "R" (at 0 0 0))
                (symbol "R_1_1"
                  (pin passive line (at 0 3.81 270) (length 1.27) (name "~" (effects)) (number "1" (effects)))
                  (pin passive line (at 0 -3.81 90) (length 1.27) (name "~" (effects)) (number "2" (effects))))))
            """;

    /** A resistor between a rail named twice on one wire — VDD and +3V3, a power flag on it — and a second rail, +5V's flagged. */
    private static final String SHEET = """
            (kicad_sch (version 20231120) (generator "eeschema") (uuid "root")
              %s
              (wire (pts (xy 50.8 40.64) (xy 50.8 46.99)))
              (wire (pts (xy 50.8 40.64) (xy 60.96 40.64)))
              (wire (pts (xy 50.8 54.61) (xy 50.8 60.96)))
              (symbol (lib_id "power:VDD") (at 50.8 40.64 0) (unit 1) (in_bom yes) (on_board yes) (uuid "p1")
                (property "Reference" "#PWR01" (at 0 0 0)) (property "Value" "VDD" (at 0 0 0)) (pin "1" (uuid "p1p"))
                (instances (project "design" (path "/root" (reference "#PWR01") (unit 1)))))
              (symbol (lib_id "power:+3V3") (at 60.96 40.64 0) (unit 1) (in_bom yes) (on_board yes) (uuid "p2")
                (property "Reference" "#PWR02" (at 0 0 0)) (property "Value" "+3V3" (at 0 0 0)) (pin "1" (uuid "p2p"))
                (instances (project "design" (path "/root" (reference "#PWR02") (unit 1)))))
              (symbol (lib_id "power:PWR_FLAG") (at 50.8 44.45 0) (unit 1) (in_bom yes) (on_board yes) (uuid "f1")
                (property "Reference" "#FLG01" (at 0 0 0)) (property "Value" "PWR_FLAG" (at 0 0 0)) (pin "1" (uuid "f1p"))
                (instances (project "design" (path "/root" (reference "#FLG01") (unit 1)))))
              (symbol (lib_id "power:PWR_FLAG") (at 50.8 60.96 0) (unit 1) (in_bom yes) (on_board yes) (uuid "f2")
                (property "Reference" "#FLG02" (at 0 0 0)) (property "Value" "PWR_FLAG" (at 0 0 0)) (pin "1" (uuid "f2p"))
                (instances (project "design" (path "/root" (reference "#FLG02") (unit 1)))))
              (symbol (lib_id "Device:R") (at 50.8 50.8 0) (unit 1) (in_bom yes) (on_board yes) (uuid "r1")
                (property "Reference" "R1" (at 0 0 0)) (property "Value" "10k" (at 0 0 0)) (pin "1" (uuid "r1p1")) (pin "2" (uuid "r1p2"))
                (instances (project "design" (path "/root" (reference "R1") (unit 1)))))
              (sheet_instances (path "/" (page "1"))))
            """.formatted(SYMBOLS);

    private static Circuit imported() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("design.kicad_sch", SHEET);
        files.put("design.kicad_pro", "{}");
        ImportedDocuments imported = new KiCadDesignImport(() -> InstalledKiCadLibraries.NONE, () -> KiCadBoardImportTest.NO_MODELS,
                KiCadLibraryMap.readFrom(KiCadLibraryMap.SHIPPED), KiCadLibraries.readFrom(KiCadLibraries.SHIPPED))
                .read(new ExchangeFiles(files, "design.kicad_pro", "Design"), CadLibrary.library());
        DocumentNode root = imported.designs().stream().filter(design -> design.slug().equals("design")).findFirst().orElseThrow();
        return SimulationReading.circuit(root);
    }

    @Test
    void twoPowerSymbolsOnOneWireAreOneNetAndEveryMarkOfItSaysThatNet() {
        Circuit circuit = imported();
        // The resistor's one connected end: the other stands only on a power flag, which joins it to nothing.
        String top = circuit.parts().getFirst().connections().getFirst().net();
        Set<String> marked = circuit.layout().stream().filter(Placement.Mark.class::isInstance).map(Placement.Mark.class::cast)
                .filter(mark -> mark.reference().startsWith("power")).map(Placement.Mark::net).collect(Collectors.toSet());
        assertEquals(Set.of(top), marked);
    }

    @Test
    void aPowerFlagNamesNoNetAndJoinsNothing() {
        Circuit circuit = imported();
        List<String> nets = circuit.parts().getFirst().connections().stream().map(connection -> connection.net()).toList();
        assertFalse(nets.stream().anyMatch(net -> net.toUpperCase().contains("PWR_FLAG")), nets.toString());
        // The two flags stand on the two ends of the resistor: were they a net, its two ends would be one net of that name. The end
        // with a flag alone joins nothing.
        assertEquals(1, nets.size(), nets.toString());
        assertFalse(circuit.layout().stream().filter(Placement.Mark.class::isInstance).map(Placement.Mark.class::cast)
                .anyMatch(mark -> mark.net() != null && mark.net().toUpperCase().contains("PWR_FLAG")));
    }
}
