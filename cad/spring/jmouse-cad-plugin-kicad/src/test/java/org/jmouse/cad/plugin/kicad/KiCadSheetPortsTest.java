package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.kicad.InstalledKiCadLibraries;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.simulation.elaboration.SheetUnfolding;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.KiCadLibraryMap;
import org.jmouse.cad.kicad.KiCadSchematicExport;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.SimulationReading;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.connectivity.KiCadConnectivity;
import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.kicad.schematic.KiCadSchematic;
import org.jmouse.kicad.schematic.KiCadSheet;
import org.jmouse.sexpression.SExpressionParser;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hierarchical sheet's pins stand where KiCad's author put them — on any edge, at any distance —, and they come back
 * there: the import writes them on the block's line ({@code ports IN left 80, …}), the language reads them again, and the
 * export stands KiCad's pins on the same points, so every wire drawn to one still ends on it.
 */
class KiCadSheetPortsTest {

    private static final String DIODE = """
            (symbol "Device:D" (in_bom yes) (on_board yes)
              (property "Reference" "D" (at 0 0 0))
              (property "Value" "D" (at 0 0 0))
              (symbol "D_1_1"
                (pin passive line (at -3.81 0 0) (length 2.54) (name "K" (effects)) (number "1" (effects)))
                (pin passive line (at 3.81 0 180) (length 2.54) (name "A" (effects)) (number "2" (effects)))))
            """;

    /** Two diodes from IN and EN to OUT. */
    private static final String CHILD = """
            (kicad_sch (version 20231120) (generator "eeschema") (uuid "child")
              (lib_symbols %s)
              (hierarchical_label "IN" (shape input) (at 50.8 50.8 180))
              (hierarchical_label "EN" (shape input) (at 50.8 60.96 180))
              (hierarchical_label "OUT" (shape output) (at 71.12 50.8 0))
              (wire (pts (xy 50.8 50.8) (xy 57.15 50.8)))
              (wire (pts (xy 64.77 50.8) (xy 71.12 50.8)))
              (wire (pts (xy 50.8 60.96) (xy 57.15 60.96)))
              (wire (pts (xy 64.77 60.96) (xy 71.12 60.96) (xy 71.12 50.8)))
              (junction (at 71.12 50.8))
              (symbol (lib_id "Device:D") (at 60.96 50.8 0) (unit 1) (in_bom yes) (on_board yes) (uuid "d1")
                (property "Reference" "D1" (at 60.96 46 0)) (property "Value" "1N4148" (at 60.96 55 0))
                (pin "1" (uuid "d1p1")) (pin "2" (uuid "d1p2"))
                (instances (project "design" (path "/root/box" (reference "D1") (unit 1)))))
              (symbol (lib_id "Device:D") (at 60.96 60.96 0) (unit 1) (in_bom yes) (on_board yes) (uuid "d2")
                (property "Reference" "D2" (at 60.96 56 0)) (property "Value" "1N4148" (at 60.96 65 0))
                (pin "1" (uuid "d2p1")) (pin "2" (uuid "d2p2"))
                (instances (project "design" (path "/root/box" (reference "D2") (unit 1))))))
            """.formatted(DIODE);

    /**
     * A box whose pins stand on three edges at uneven distances: IN on the left 5.08 mm down, OUT on the right 2.54 mm down,
     * EN on the bottom 5.08 mm in; each wired to a label of the root.
     */
    private static final String ROOT = """
            (kicad_sch (version 20231120) (generator "eeschema") (uuid "root")
              (lib_symbols)
              (sheet (at 101.6 40.64) (size 20.32 15.24) (uuid "box")
                (property "Sheetname" "child") (property "Sheetfile" "child.kicad_sch")
                (pin "IN" input (at 101.6 45.72 180))
                (pin "OUT" output (at 121.92 43.18 0))
                (pin "EN" input (at 106.68 55.88 270)))
              (label "SUPPLY" (at 93.98 45.72 0))
              (wire (pts (xy 93.98 45.72) (xy 101.6 45.72)))
              (label "LOAD" (at 129.54 43.18 0))
              (wire (pts (xy 121.92 43.18) (xy 129.54 43.18)))
              (label "ENABLE" (at 106.68 63.5 0))
              (wire (pts (xy 106.68 55.88) (xy 106.68 63.5)))
              (sheet_instances (path "/" (page "1"))))
            """;

    private static ImportedDocuments imported() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("design.kicad_sch", ROOT);
        files.put("child.kicad_sch", CHILD);
        files.put("design.kicad_pro", "{}");
        return new KiCadDesignImport(() -> InstalledKiCadLibraries.NONE, () -> KiCadBoardImportTest.NO_MODELS, KiCadLibraryMap.readFrom(KiCadLibraryMap.SHIPPED), KiCadLibraries.readFrom(KiCadLibraries.SHIPPED))
                .read(new ExchangeFiles(files, "design.kicad_pro", "Design"), CadLibrary.library());
    }

    private static DocumentNode root(ImportedDocuments imported) {
        return imported.designs().stream().filter(design -> design.slug().equals("design")).findFirst().orElseThrow();
    }

    private static CatalogueDocuments documentsOf(ImportedDocuments imported) {
        Map<String, DocumentNode> made = new HashMap<>();
        imported.library().forEach(document -> made.put(document.slug(), document));
        imported.designs().forEach(document -> made.put(document.slug(), document));
        return slug -> Optional.ofNullable(made.get(slug)).or(() -> CadLibrary.library().find(slug));
    }

    private static Placement.PlacedPart block(DocumentNode circuit) {
        return SimulationReading.circuit(circuit).layout().stream().filter(Placement.PlacedPart.class::isInstance)
                .map(Placement.PlacedPart.class::cast).filter(placed -> placed.reference().equals("child")).findFirst().orElseThrow();
    }

    @Test
    void theImportWritesWhereEachPortStandsOnTheBlocksFrame() {
        ImportedDocuments imported = imported();
        List<Placement.PortPlace> expected = List.of(new Placement.PortPlace("IN", Placement.PortPlace.Edge.LEFT, 80),
                new Placement.PortPlace("OUT", Placement.PortPlace.Edge.RIGHT, 40),
                new Placement.PortPlace("EN", Placement.PortPlace.Edge.BOTTOM, 80));
        assertEquals(expected, block(root(imported)).ports(), String.valueOf(imported.notes()));
        // The language writes them on the block's line, and reads them back the same.
        String text = CadLanguage.standard().language().write(root(imported));
        assertTrue(text.contains("ports: [IN left 80, OUT right 40, EN bottom 80]"), text);
        assertEquals(expected, block(CadLanguage.standard().language().readOne(text, "design.jmx")).ports());
    }

    @Test
    void theExportStandsKiCadsPinsWhereTheyStood() {
        ImportedDocuments imported = imported();
        DocumentNode root = root(imported);
        KiCadSchematicExport.Exported exported = KiCadSchematicExport.of(root.slug(), root.title(), SimulationReading.circuit(root),
                documentsOf(imported), KiCadLibraryMap.shipped(), KiCadLibraries.shipped());
        KiCadSchematic written = KiCadSchematic.of(new SExpressionParser().parse("design.kicad_sch", exported.text()));
        KiCadSheet box = written.sheets().getFirst();
        Map<String, KiCadPoint> pins = new LinkedHashMap<>();
        box.pins().forEach(pin -> pins.put(pin.name(), pin.at()));
        assertTrue(pins.get("IN").sameAs(new KiCadPoint(101.6, 45.72)), String.valueOf(pins));
        assertTrue(pins.get("OUT").sameAs(new KiCadPoint(121.92, 43.18)), String.valueOf(pins));
        assertTrue(pins.get("EN").sameAs(new KiCadPoint(106.68, 55.88)), String.valueOf(pins));
    }

    private static final String RESISTOR = """
              (symbol "Device:R" (in_bom yes) (on_board yes)
                (property "Reference" "R" (at 0 0 0)) (property "Value" "R" (at 0 0 0))
                (symbol "R_1_1"
                  (pin passive line (at 0 3.81 270) (length 1.27) (name "~" (effects)) (number "1" (effects)))
                  (pin passive line (at 0 -3.81 90) (length 1.27) (name "~" (effects)) (number "2" (effects)))))
            """;

    /**
     * A sheet handing a bus out, D[0..2]: a resistor's ends on its nets D0 and D1, each an entry takes off a bus line and a label names —
     * D2 taken off by none —,
     * the bus leaving by a hierarchical label of its name.
     */
    private static final String BUS_CHILD = """
            (kicad_sch (version 20231120) (generator "eeschema") (uuid "bus-child")
              (lib_symbols %s)
              (symbol (lib_id "Device:R") (at 25.4 50.8 0) (unit 1) (in_bom yes) (on_board yes) (uuid "r2")
                (property "Reference" "R2" (at 0 0 0)) (property "Value" "1k" (at 0 0 0)) (pin "1" (uuid "r2p1")) (pin "2" (uuid "r2p2"))
                (instances (project "design" (path "/bus-root/box" (reference "R2") (unit 1)))))
              (label "D0" (at 25.4 46.99 0))
              (label "D1" (at 25.4 54.61 0))
              (bus (pts (xy 27.94 44.45) (xy 27.94 52.07)))
              (bus_entry (at 27.94 44.45) (size -2.54 2.54))
              (bus_entry (at 27.94 52.07) (size -2.54 2.54))
              (hierarchical_label "D[0..2]" (shape bidirectional) (at 50.8 50.8 180))
              (bus (pts (xy 50.8 50.8) (xy 76.2 50.8))))
            """.formatted(RESISTOR);

    /** A box with the bus's pin, wired to a bus of the root; a resistor of the root's on its nets D0 and D1, taken off a bus the same way. */
    private static final String BUS_ROOT = """
            (kicad_sch (version 20231120) (generator "eeschema") (uuid "bus-root")
              (lib_symbols %s)
              (symbol (lib_id "Device:R") (at 50.8 50.8 0) (unit 1) (in_bom yes) (on_board yes) (uuid "r1")
                (property "Reference" "R1" (at 0 0 0)) (property "Value" "1k" (at 0 0 0)) (pin "1" (uuid "r1p1")) (pin "2" (uuid "r1p2"))
                (instances (project "design" (path "/bus-root" (reference "R1") (unit 1)))))
              (label "D0" (at 50.8 46.99 0))
              (label "D1" (at 50.8 54.61 0))
              (bus (pts (xy 53.34 44.45) (xy 53.34 52.07)))
              (bus_entry (at 53.34 44.45) (size -2.54 2.54))
              (bus_entry (at 53.34 52.07) (size -2.54 2.54))
              (sheet (at 101.6 40.64) (size 20.32 15.24) (uuid "box")
                (property "Sheetname" "child") (property "Sheetfile" "child.kicad_sch")
                (pin "D[0..2]" bidirectional (at 101.6 45.72 180)))
              (bus (pts (xy 81.28 45.72) (xy 101.6 45.72)))
              (label "D[0..2]" (at 88.9 45.72 0))
              (sheet_instances (path "/" (page "1"))))
            """.formatted(RESISTOR);

    /**
     * A bus handed across a sheet's edge is one port, the box's pin bound to the root's bus of its name — declared on both sheets —,
     * and run it joins its nets member by member, as KiCad's netlist does: the two resistors are on the same two nets.
     */
    @Test
    void aBusHandedAcrossASheetsEdgeJoinsItsNetsMemberByMember() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("design.kicad_sch", BUS_ROOT);
        files.put("child.kicad_sch", BUS_CHILD);
        files.put("design.kicad_pro", "{}");
        ImportedDocuments imported = new KiCadDesignImport(() -> InstalledKiCadLibraries.NONE, () -> KiCadBoardImportTest.NO_MODELS,
                KiCadLibraryMap.readFrom(KiCadLibraryMap.SHIPPED), KiCadLibraries.readFrom(KiCadLibraries.SHIPPED))
                .read(new ExchangeFiles(files, "design.kicad_pro", "Design"), CadLibrary.library());
        Map<String, DocumentNode> made = new HashMap<>();
        imported.designs().forEach(document -> made.put(document.slug(), document));
        Circuit root = SimulationReading.circuit(root(imported));
        // The box hands the bus across whole: its pin D bound to the root's bus D, declared on both sheets.
        Part box = root.parts().stream().filter(part -> part.reference().equals("child")).findFirst().orElseThrow();
        assertEquals(List.of("D"), box.connections().stream().map(connection -> connection.net()).toList());
        assertEquals(3, root.bus("D").orElseThrow().size());
        // Run, the bus is bound member by member: the child's resistor stands on the root resistor's two nets.
        Circuit unfolded = SheetUnfolding.of(root, slug -> Optional.ofNullable(made.get(slug)));
        List<String> above = unfolded.part("R1").orElseThrow().connections().stream().map(connection -> connection.net()).toList();
        List<String> inside = unfolded.part("child_R2").orElseThrow().connections().stream().map(connection -> connection.net()).toList();
        assertEquals(Set.copyOf(above), Set.copyOf(inside), "inside " + inside + ", above " + above);
    }

    /**
     * A bus handed across a sheet's edge leaves for KiCad as KiCad draws one — a bus pin and a hierarchical label of the bus's text,
     * a bus line named by a label of it — and KiCad's reading of what is written joins it member by member, as before.
     */
    @Test
    void aBusLeavesForKiCadWholeAndKiCadJoinsItMemberByMember() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("design.kicad_sch", BUS_ROOT);
        files.put("child.kicad_sch", BUS_CHILD);
        files.put("design.kicad_pro", "{}");
        ImportedDocuments imported = new KiCadDesignImport(() -> InstalledKiCadLibraries.NONE, () -> KiCadBoardImportTest.NO_MODELS,
                KiCadLibraryMap.readFrom(KiCadLibraryMap.SHIPPED), KiCadLibraries.readFrom(KiCadLibraries.SHIPPED))
                .read(new ExchangeFiles(files, "design.kicad_pro", "Design"), CadLibrary.library());
        DocumentNode root = root(imported);
        Map<String, DocumentNode> made = new HashMap<>();
        imported.designs().forEach(document -> made.put(document.slug(), document));
        CatalogueDocuments documents = slug -> Optional.ofNullable(made.get(slug)).or(() -> CadLibrary.library().find(slug));
        KiCadSchematicExport.Exported exported = KiCadSchematicExport.of(root.slug(), root.title(), SimulationReading.circuit(root),
                documents, KiCadLibraryMap.shipped(), KiCadLibraries.shipped());
        Map<String, KiCadSchematic> written = new HashMap<>();
        written.put(exported.fileName(), KiCadSchematic.of(new SExpressionParser().parse(exported.fileName(), exported.text())));
        exported.sheets().forEach(sheet -> written.put(sheet.fileName(), KiCadSchematic.of(new SExpressionParser().parse(sheet.fileName(), sheet.text()))));
        KiCadSchematic top = written.get(exported.fileName());
        // D2, which no entry takes off, is a net of the bus's own; the bus leaves as KiCad writes a group of its nets.
        String text = "{" + String.join(" ", SimulationReading.circuit(root).bus("D").orElseThrow().members()) + "}";
        assertEquals("{D0 D1 D_D2_open}", text);
        assertTrue(top.sheets().getFirst().pins().stream().anyMatch(pin -> pin.name().equals(text)), exported.text());
        assertTrue(top.buses().size() > 0 && top.labels().stream().anyMatch(label -> label.text().equals(text)), exported.text());
        KiCadConnectivity connectivity = KiCadConnectivity.of(KiCadProject.of(exported.fileName(), written::get));
        Map<String, List<String>> nets = new HashMap<>();
        connectivity.nets().forEach(net -> nets.put(net.name(), net.pins().stream().map(Object::toString).toList()));
        String r1 = nets.entrySet().stream().filter(net -> net.getValue().contains("R1-1")).map(Map.Entry::getKey).findFirst().orElseThrow();
        assertTrue(nets.get(r1).contains("child_R2-1"), nets.toString());
    }
}
