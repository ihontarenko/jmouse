package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.settings.CadSettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ExportedFile;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.KiCadLibraryMap;
import org.jmouse.cad.kicad.KiCadProjectExport;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.kicad.KiCadProjectSettings;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A pcb written as a KiCad project reads back as the pcb it was: the design of {@link KiCadBoardImportTest} read, written
 * and read again, every part where it stood on the pads' nets, its copper, its pours, its edge.
 */
class KiCadProjectExportTest {

    private static final PcbArtwork.Settings ARTWORK = CadSettings.read(PcbArtwork.SECTION);

    private static ImportedDocuments read(Map<String, String> files, String project) {
        return new KiCadDesignImport(() -> KiCadFootprintLibraries.NONE, () -> KiCadBoardImportTest.NO_MODELS)
                .read(new ExchangeFiles(files, project, "Design"), CadLibrary.library());
    }

    private static CatalogueDocuments documentsOf(ImportedDocuments imported) {
        Map<String, DocumentNode> made = new HashMap<>();
        // As the store finds them: by slug, and by the code a reference names them by.
        imported.library().forEach(document -> {
            made.put(document.slug(), document);
            made.put(document.referenceName(), document);
        });
        imported.designs().forEach(document -> made.put(document.slug(), document));
        made.put(imported.root().slug(), imported.root());
        return slug -> Optional.ofNullable(made.get(slug)).or(() -> CadLibrary.library().find(slug));
    }

    private static ImportedDocuments original() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("design.kicad_sch", KiCadBoardImportTest.SCHEMATIC);
        files.put("design.kicad_pcb", KiCadBoardImportTest.BOARD);
        files.put("design.kicad_pro", "{}");
        return read(files, "design.kicad_pro");
    }

    private static ExportedFiles written(ImportedDocuments imported) {
        return KiCadProjectExport.of(imported.root(), documentsOf(imported), KiCadLibraryMap.standard(), KiCadLibraries.standard(),
                CadStandards.pcbRules().valuesOf(PcbDrawing.of(imported.root()).rules()), ARTWORK);
    }

    private static PcbFabrication fabricated(ImportedDocuments imported) {
        return PcbFabrication.of(imported.root(), documentsOf(imported));
    }

    private static Map<String, String> filesOf(ExportedFiles written) {
        Map<String, String> files = new LinkedHashMap<>();
        for (ExportedFile file : written.files()) {
            files.put(file.fileName(), file.text());
        }
        return files;
    }

    @Test
    void aProjectIsItsBoardItsSchematicAndItsFileNamedAfterTheCircuit() {
        ExportedFiles written = written(original());
        assertEquals(List.of("design.kicad_pro", "design.kicad_sch", "design.kicad_pcb"),
                written.files().stream().map(ExportedFile::fileName).toList());
        assertEquals("design-kicad", written.name());
    }

    @Test
    void theBoardReadsBackAsItWasWritten() {
        ImportedDocuments first = original();
        PcbFabrication before = fabricated(first);
        ExportedFiles written = written(first);
        ImportedDocuments again = read(filesOf(written), "design.kicad_pro");
        PcbFabrication after = fabricated(again);
        assertFalse(again.notes().stream().anyMatch(note -> note.contains("no longer has") || note.contains("disagree")),
                String.valueOf(again.notes()));
        assertEquals(before.parts().stream().map(PcbFabrication.SeatedPart::designator).toList(),
                after.parts().stream().map(PcbFabrication.SeatedPart::designator).toList());
        for (PcbFabrication.SeatedPart part : before.parts()) {
            PcbFabrication.SeatedPart read = after.parts().stream().filter(candidate -> candidate.designator().equals(part.designator()))
                    .findFirst().orElseThrow();
            PcbDrawing.Placement was = part.placement();
            PcbDrawing.Placement is = read.placement();
            assertEquals(was.side(), is.side(), part.designator());
            assertEquals(was.x(), is.x(), 1e-3, part.designator());
            assertEquals(was.y(), is.y(), 1e-3, part.designator());
            assertEquals(0, Math.floorMod(Math.round(was.rotation() - is.rotation()), 360), part.designator());
            assertEquals(part.padNets(), read.padNets(), part.designator());
            // Every pad stands where it stood: the footprint's own drawing, turned and mirrored as the part is.
            for (var pad : part.drawing().orElseThrow().pads()) {
                var other = read.drawing().orElseThrow().pads().stream().filter(candidate -> candidate.number().equals(pad.number()))
                        .findFirst().orElseThrow();
                PcbDrawing.Point there = was.placed(pad.x(), pad.y());
                PcbDrawing.Point here = is.placed(other.x(), other.y());
                assertEquals(there.x(), here.x(), 1e-3, part.designator() + "." + pad.number());
                assertEquals(there.y(), here.y(), 1e-3, part.designator() + "." + pad.number());
            }
        }
        assertEquals(before.pcb().tracks().size(), after.pcb().tracks().size());
        assertEquals(before.pcb().vias().size(), after.pcb().vias().size());
        assertEquals(before.pcb().zones().stream().map(PcbDrawing.Zone::net).toList(), after.pcb().zones().stream().map(PcbDrawing.Zone::net).toList());
        assertTrue(after.pcb().texts().stream().anyMatch(text -> text.text().equals("Note") && text.layer().equals("documentation")));
    }

    @Test
    void eachFootprintLeavesWithWhatIsKnownOfWhoseItIs() {
        // The board's footprints are the project's own library's: they go out saying where they were taken from, as a field of
        // the footprint KiCad keeps hidden, and claim no holder nobody named — the project's terms are the project's.
        String board = filesOf(written(original())).get("design.kicad_pcb");
        assertTrue(board.contains("(property \"Source\" \"KiCad footprint Diode_THT:D_DO-35, from the project's own library\""), board);
        assertFalse(board.contains("(property \"Author\""), board);
        int source = board.indexOf("(property \"Source\"");
        assertTrue(board.substring(source, board.indexOf("(uuid", source)).contains("(hide yes)"), board.substring(source, source + 200));
    }

    @Test
    void aViaLeftOpenIsAnOpeningOnItsSidesMask() {
        ImportedDocuments again = read(filesOf(written(original())), "design.kicad_pro");
        PcbDrawing pcb = fabricated(again).pcb();
        PcbDrawing.Via open = pcb.vias().getFirst();
        // The opening KiCad keeps as a drawing on the front's mask, round the via.
        assertTrue(pcb.shapes().stream().anyMatch(shape -> shape.layer().equals("top-mask") && shape.filled()
                && shape.path().contains(String.valueOf((int) open.x()))), String.valueOf(pcb.shapes()));
    }

    @Test
    void aViaStandingInAPourIsOnThePoursNet() {
        String board = filesOf(written(original())).get("design.kicad_pcb");
        org.jmouse.sexpression.node.SExpressionList root = new org.jmouse.sexpression.SExpressionParser().parse("design.kicad_pcb", board).root();
        Map<Double, String> names = new LinkedHashMap<>();
        root.lists("net").forEach(net -> names.put(net.number(1), net.word(2)));
        // Both stitch the CATHODE pours — a net a label of the root sheet names, so /CATHODE to KiCad —: each via names it by its number.
        List<String> viaNets = root.lists("via").stream().map(via -> names.get(via.requireList("net").number(1))).toList();
        assertEquals(List.of("/CATHODE", "/CATHODE"), viaNets);
    }

    @Test
    void theProjectFileSaysHowTheBoardIsMade() {
        ImportedDocuments first = original();
        Map<String, Double> rules = CadStandards.pcbRules().valuesOf(PcbDrawing.of(first.root()).rules());
        KiCadProjectSettings settings = KiCadProjectSettings.of(filesOf(written(first)).get("design.kicad_pro"));
        assertEquals(rules.get(PcbDrawing.RULE_CLEARANCE), settings.usual().orElseThrow().clearance(), 1e-9);
        assertEquals(rules.get(PcbDrawing.RULE_TRACK_WIDTH), settings.usual().orElseThrow().trackWidth(), 1e-9);
        assertTrue(settings.rules().containsKey("min_track_width"), String.valueOf(settings.rules()));
    }
}
