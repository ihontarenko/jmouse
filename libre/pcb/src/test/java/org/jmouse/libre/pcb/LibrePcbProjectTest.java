package org.jmouse.libre.pcb;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibrePcbProjectTest {

    private static final Map<String, String> PROJECT = Map.of(
            "project/metadata.lp", "(librepcb_project_metadata m (name \"Demo\") (author \"A\") (version \"v1\"))",
            "circuit/circuit.lp", """
                    (librepcb_circuit
                     (netclass c (name "default") (default_trace_width inherit) (default_via_drill_diameter 0.4) (min_copper_copper_clearance 0.2))
                     (net n (auto true) (name "N1") (netclass c))
                     (bus u (name "D[0..7]") (prefix_nets false) (max_trace_length_difference 0.0))
                     (component p (lib_component k) (lib_variant v) (name "R1") (value "1k") (device d)
                      (signal s (net n)) (signal t (net none))))
                    """,
            "schematics/schematics.lp", "(librepcb_schematics (schematic \"schematics/main/schematic.lp\"))",
            "schematics/main/schematic.lp", """
                    (librepcb_schematic x (name "Main")
                     (symbol y (component p) (lib_gate g) (position 10.0 20.0) (rotation 90.0) (mirror true))
                     (netsegment z (net n)
                      (junction j (position 1.0 2.0))
                      (line l (width 0.15) (from (symbol y) (pin a)) (to (junction j))))
                     (bussegment v (bus u)
                      (junction a (position 0.0 0.0))
                      (junction b (position 0.0 5.0))
                      (line m (width 0.5) (from (junction a)) (to (junction b)))))
                    """,
            "boards/boards.lp", "(librepcb_boards (board \"boards/default/board.lp\"))",
            "boards/default/board.lp", """
                    (librepcb_board b (name "default") (layers (inner 2)) (thickness 1.6)
                     (design_rules (default_trace_width 0.5)
                      (stopmask_clearance (ratio 0.0) (min 0.1) (max 0.1))
                      (solderpaste_clearance (ratio 0.1) (min 0.0) (max 1.0))
                      (stopmask_max_via_drill_diameter 0.5))
                     (design_rule_check (min_copper_width 0.2) (solder_resist))
                     (device p (lib_device d) (lib_footprint f) (position 5.0 5.0) (rotation 90.0) (flip true) (lock false))
                     (netsegment z (net n)
                      (via w (from top_cu) (to bot_cu) (position 1.0 1.0) (drill 0.3) (size 0.7) (exposure off))
                      (trace t (layer top_cu) (width 0.3) (from (device p) (pad q)) (to (via w)))))
                    """,
            "library/pkg/f/package.lp", """
                    (librepcb_package f (name "R0805") (pad q (name "1"))
                     (footprint r (name "default")
                      (pad q (side top) (shape roundrect) (position 1.0 0.0) (rotation 0.0) (size 1.0 2.0) (radius 0.5)
                       (stop_mask auto) (solder_paste off) (clearance 0.0) (function standard) (package_pad q))))
                    """);

    @Test
    void aProjectReadsWhole() {
        LibrePcbProject project = LibrePcbProject.read(PROJECT);
        assertEquals("Demo", project.name());
        LibrePcbCircuit.Part part = project.circuit().parts().get("p");
        assertEquals(Map.of("s", "n"), part.nets());
        assertFalse(project.circuit().nets().get("n").named());
        assertTrue(project.circuit().netClasses().get("c").traceWidth().isEmpty());
        assertEquals(0.4, project.circuit().netClasses().get("c").viaDrill().orElseThrow());
        LibrePcbBoard board = project.boards().getFirst();
        assertEquals(2, board.innerLayers());
        assertEquals(Map.of("min_copper_width", 0.2), board.rules().checks());
        assertEquals(0.1, board.rules().maskClearance().of(5));
        LibrePcbBoard.Trace trace = board.segments().getFirst().traces().getFirst();
        assertEquals(new LibrePcbAnchor.OnPad("p", "q"), trace.from());
        assertEquals(new LibrePcbAnchor.OnVia("w"), trace.to());
        assertInstanceOf(LibrePcbOpening.Off.class, board.segments().getFirst().vias().getFirst().exposure());
    }

    @Test
    void aBusIsTheCircuitsAndItsLinesTheSheets() {
        LibrePcbProject project = LibrePcbProject.read(PROJECT);
        assertEquals("D[0..7]", project.circuit().buses().get("u").name());
        LibrePcbSchematic.BusSegment segment = project.schematics().getFirst().busSegments().getFirst();
        assertEquals("u", segment.bus());
        assertEquals(2, segment.junctions().size());
        assertEquals(new LibrePcbAnchor.OnJunction("b"), segment.lines().getFirst().to());
    }

    @Test
    void aPadSaysWhatItOpens() {
        LibrePcbLibrary.FootprintPad pad = LibrePcbProject.read(PROJECT).library().packages().get("f").footprints().getFirst().pads().getFirst();
        assertInstanceOf(LibrePcbOpening.Automatic.class, pad.stopMask());
        assertInstanceOf(LibrePcbOpening.Off.class, pad.solderPaste());
        assertEquals("q", pad.packagePad().orElseThrow());
        assertEquals(LibrePcbLibrary.PadShape.ROUNDED_RECTANGLE, pad.shape());
    }

    @Test
    void aFlippedDeviceIsMirroredThenTurned() {
        LibrePcbBoard.DeviceOnBoard device = LibrePcbProject.read(PROJECT).boards().getFirst().devices().getFirst();
        LibrePcbPoint placed = device.transform().placed(new LibrePcbPoint(1, 0));
        // Mirrored to -1, 0; a quarter turn counter-clockwise makes that 0, -1; moved by 5, 5.
        assertEquals(5, placed.x(), 1e-9);
        assertEquals(4, placed.y(), 1e-9);
    }

    @Test
    void aMissingFileIsSaidByItsName() {
        LibrePcbReadException refused = assertThrows(LibrePcbReadException.class, () -> LibrePcbProject.read(Map.of()));
        assertTrue(refused.getMessage().contains(LibrePcbFile.METADATA.path()), refused.getMessage());
    }

    @Test
    void aMaskWordThisReaderDoesNotKnowIsRefused() {
        assertThrows(LibrePcbReadException.class, () -> LibrePcbOpening.of(Optional.of("sometimes")));
    }

    @Test
    void aProjectIsFoundByItsMarker() {
        assertTrue(LibrePcbFile.isProject("can2usb.LPP"));
        assertTrue(LibrePcbFile.isRead("boards/default/board.lp"));
        assertFalse(LibrePcbFile.isRead("resources/pcb.png"));
    }
}
