package org.jmouse.cad.libre.pcb;

import org.jmouse.cad.exchange.imported.SheetMarks;
import org.jmouse.cad.library.CadLibrary;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.libre.pcb.LibrePcbProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("a LibrePCB sheet's bus lines")
class LibrePcbBusImportTest {

    /** The marks as the shipped library draws them. */
    private static final SheetMarks MARKS = SheetMarks.of(CadLibrary.library(), LibrePcbFiling.standard().markSymbols());

    /** A project of one sheet holding one bus, D[0..3], drawn as a segment of two lines meeting at a corner. */
    private static final Map<String, String> PROJECT = Map.of(
            "project/metadata.lp", "(librepcb_project_metadata m (name \"Buses\") (author \"A\") (version \"v1\"))",
            "circuit/circuit.lp", """
                    (librepcb_circuit
                     (netclass c (name "default") (default_trace_width inherit) (default_via_drill_diameter inherit) (min_copper_copper_clearance 0.0))
                     (bus u (name "D[0..3]") (prefix_nets false) (max_trace_length_difference 0.0)))
                    """,
            "schematics/schematics.lp", "(librepcb_schematics (schematic \"schematics/main/schematic.lp\"))",
            "schematics/main/schematic.lp", """
                    (librepcb_schematic x (name "Main")
                     (bussegment v (bus u)
                      (junction a (position 0.0 0.0))
                      (junction b (position 0.0 5.08))
                      (junction e (position 7.62 5.08))
                      (line m (width 0.5) (from (junction a)) (to (junction b)))
                      (line o (width 0.5) (from (junction b)) (to (junction e)))))
                    """,
            "boards/boards.lp", "(librepcb_boards)");

    private final DocumentLanguage language = CadLanguage.standard().language();

    @Test
    @DisplayName("come across as one line, named as the circuit names the bus, with the nets that name carries")
    void aBusComesAcrossNamed() {
        LibrePcbProject project = LibrePcbProject.read(PROJECT);
        ImportedDocuments imported = LibrePcbDesignImport.read(project, Map.of(), "buses.lpp", "Buses", LibrePcbFiling.standard(), MARKS);
        String circuit = language.write(imported.designs().getFirst());
        assertTrue(circuit.contains("bus:B1(D"), circuit);
        assertTrue(circuit.contains("D0") && circuit.contains("D3"), circuit);
        assertEquals(1, circuit.split("bus:", -1).length - 1, circuit);
        assertDoesNotThrow(() -> language.readOne(circuit, "buses"));
    }
}
