package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.drawing.DrawnState;
import org.jmouse.cad.kicad.KiCadLibraries;
import org.jmouse.cad.kicad.InstalledKiCadLibraries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.cad.document.part.FetchedFile;
import org.jmouse.cad.mesh.ModelModule;
import org.jmouse.cad.mesh.ModelSource;
import org.jmouse.core.io.ByteArrayResource;
import org.jmouse.cad.exchange.ExchangeFiles;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.kicad.KiCadLibraryMap;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.CategoryDocument;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A design read with its board: what must hold whatever the board, on the smallest design that shows each of it. */
class KiCadBoardImportTest {

    static final String SCHEMATIC = """
            (kicad_sch (version 20231120) (generator "eeschema") (uuid "root")
              (lib_symbols
                (symbol "Device:D" (in_bom yes) (on_board yes)
                  (property "Reference" "D" (at 0 0 0))
                  (property "Value" "D" (at 0 0 0))
                  (symbol "D_1_1"
                    (pin passive line (at -3.81 0 0) (length 2.54) (name "K" (effects)) (number "1" (effects)))
                    (pin passive line (at 3.81 0 180) (length 2.54) (name "A" (effects)) (number "2" (effects))))))
              (label "CATHODE" (at 90 50 0))
              (label "ANODE" (at 110 50 0))
              (wire (pts (xy 90 50) (xy 96.19 50)))
              (wire (pts (xy 103.81 50) (xy 110 50)))
              (symbol (lib_id "Device:D") (at 100 50 0) (unit 1) (in_bom yes) (on_board yes) (uuid "diode")
                (property "Reference" "D1" (at 100 45 0))
                (property "Value" "1N4148" (at 100 55 0))
                (property "Footprint" "Diode_THT:D_DO-35" (at 100 55 0))
                (pin "1" (uuid "p1")) (pin "2" (uuid "p2")))
              (bus (pts (xy 20 20) (xy 40 20)))
              (bus (pts (xy 40 20) (xy 40 40)))
              (label "D[0..2]" (at 30 20 0)))
            """;

    static final String BOARD = """
            (kicad_pcb (version 20240108) (generator "pcbnew")
              (layers (0 "F.Cu" signal) (31 "B.Cu" signal) (44 "Edge.Cuts" user))
              (footprint "Diode_THT:D_DO-35" (layer "B.Cu")
                (at 110 60 90)
                (property "Reference" "D1" (at 0 2 90) (layer "B.SilkS"))
                (path "/diode")
                (pad "1" thru_hole rect (at 0 0 90) (size 1.6 1.6) (drill 0.8) (layers "*.Cu" "B.Mask") (net 1 "CATHODE"))
                (pad "2" thru_hole oval (at 7.62 0 90) (size 1.6 1.6) (drill 0.8) (layers "*.Cu" "B.Mask") (net 2 "ANODE")))
              (footprint "MountingHole:M3" (layer "F.Cu")
                (at 105 55)
                (property "Reference" "H1" (at 0 -3 0) (layer "F.SilkS"))
                (pad "1" thru_hole circle (at 0 0) (size 5 5) (drill 3) (layers "*.Cu" "F.Mask") (net 1 "CATHODE")))
              (gr_rect (start 100 50) (end 130 70) (stroke (width 0.1) (type default)) (layer "Edge.Cuts"))
              (gr_line (start 101 51) (end 105 51) (stroke (width 0.15) (type default)) (layer "F.SilkS"))
              (gr_poly (pts (xy 110 52) (xy 112 52) (xy 112 54)) (stroke (width 0) (type solid)) (fill solid) (layer "B.Mask"))
              (zone (net 1) (net_name "CATHODE") (layers "B.Cu" "B.Mask") (connect_pads (clearance 0.5))
                (polygon (pts (xy 120 52) (xy 125 52) (xy 125 57) (xy 120 57))))
              (gr_line (start 101 60) (end 105 60) (stroke (width 0.2) (type default)) (layer "F.Adhes"))
              (gr_line (start 101 62) (end 105 62) (stroke (width 0.2) (type default)) (layer "Cmts.User"))
              (gr_text "Note" (at 120 60) (layer "Dwgs.User") (effects (font (size 1 1))))
              (footprint "TestPoint:TP" (layer "F.Cu")
                (at 125 65)
                (property "Reference" "TP1" (at 0 -2 0) (layer "F.SilkS"))
                (pad "1" smd circle (at 0 0) (size 1 1) (layers "F.Cu" "F.Mask") (solder_mask_margin 0.1) (net 1 "CATHODE"))
                (pad "2" smd rect (at 3 0) (size 2 2) (layers "F.Cu" "F.Mask" "F.Paste") (solder_paste_margin_ratio -0.1) (net 1 "CATHODE"))
                (model "${KICAD8_3DMODEL_DIR}/TestPoint.3dshapes/TP.wrl"))
              (via (at 120 55) (size 0.6) (drill 0.3) (layers "F.Cu" "B.Cu") (tenting (front no) (back yes)) (net 1))
              (via (at 121 55) (size 0.6) (drill 0.3) (layers "F.Cu" "B.Cu") (net 1))
              (segment (start 110 60) (end 120 60) (width 0.25) (layer "B.Cu") (net 1))
              (zone (net 1) (net_name "CATHODE") (layer "F.Cu") (connect_pads (clearance 0.5))
                (polygon (pts (xy 100 50) (xy 130 50) (xy 130 70)))))
            """;

    /** No 3D model library installed. */
    static final ModelSource NO_MODELS = new ModelSource() {
        @Override
        public String id() {
            return "none";
        }

        @Override
        public List<FetchedModel> modelsOf(DocumentNode footprint) {
            return List.of();
        }
    };

    /** The model the board's test point names. */
    private static final String TEST_POINT_MODEL = "kicad-testpoint-tp-model";
    /** The code the board's test point names its model by. */
    private static final String TEST_POINT_MODEL_CODE = "MD.TESTPOINT:TP";

    /** A library that holds every model a footprint names, and one it names nowhere, which is never brought. */
    private static final ModelSource ONE_MODEL = new ModelSource() {
        @Override
        public String id() {
            return "one";
        }

        @Override
        public List<FetchedModel> modelsOf(DocumentNode footprint) {
            // As KiCad's model folders give them: a model stored under its slug, named by the code its footprint writes.
            List<String> codes = new java.util.ArrayList<>(FootprintDrawing.of(footprint).models().stream().map(FootprintDrawing.Model::reference).toList());
            codes.add("MD.KICAD:UNNAMED");
            return codes.stream().map(code -> {
                String slug = "kicad-" + code.substring(code.indexOf('.') + 1).toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-") + "-model";
                DocumentNode model = new DocumentNode(ModelModule.MODEL, "Body", slug, null, 1);
                model.addExpression(org.jmouse.cad.document.MetaNode.filed("Body", code, null, List.of()));
                return new FetchedModel(model, List.of(new FetchedFile(slug, "body.wrl", new ByteArrayResource(new byte[] {1}))));
            }).toList();
        }
    };

    @Test
    void aDesignBringsTheModelsItsFootprintsNameEachOnceBeforeThem() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("design.kicad_sch", SCHEMATIC);
        files.put("design.kicad_pcb", BOARD);
        files.put("design.kicad_pro", "{}");
        ImportedDocuments imported = new KiCadDesignImport(() -> InstalledKiCadLibraries.NONE, () -> ONE_MODEL, KiCadLibraryMap.readFrom(KiCadLibraryMap.SHIPPED), KiCadLibraries.readFrom(KiCadLibraries.SHIPPED))
                .read(new ExchangeFiles(files, "design.kicad_pro", "Design"), CadLibrary.library());
        List<String> slugs = imported.library().stream().map(DocumentNode::slug).toList();
        assertEquals(1, slugs.stream().filter(TEST_POINT_MODEL::equals).count(), slugs.toString());
        List<String> drawings = imported.library().stream().filter(document -> !document.kind().equals(SimulationModule.CATEGORY))
                .map(DocumentNode::slug).toList();
        assertEquals(TEST_POINT_MODEL, drawings.getFirst(), "what a footprint names comes before it");
        assertEquals(1, imported.files().size(), "the model nothing names is not brought");
    }

    /** Every KiCad library a drawing comes from is a category of its own under KiCad's, standing before what is filed in it. */
    @Test
    void eachKiCadLibraryIsACategoryStandingBeforeItsDrawings() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("design.kicad_sch", SCHEMATIC);
        files.put("design.kicad_pcb", BOARD);
        files.put("design.kicad_pro", "{}");
        ImportedDocuments imported = new KiCadDesignImport(() -> InstalledKiCadLibraries.NONE, () -> ONE_MODEL, KiCadLibraryMap.readFrom(KiCadLibraryMap.SHIPPED), KiCadLibraries.readFrom(KiCadLibraries.SHIPPED))
                .read(new ExchangeFiles(files, "design.kicad_pro", "Design"), CadLibrary.library());
        List<String> standing = new ArrayList<>();
        for (DocumentNode document : imported.library()) {
            // The model this test's place brings writes no meta, so it names no category.
            String filedIn = document.block(MetaNode.class).flatMap(meta -> meta.text(MetaNode.CATEGORY)).orElse("kicad");
            if (document.kind().equals(SimulationModule.CATEGORY)) {
                CategoryDocument category = CategoryDocument.of(document);
                assertEquals("kicad", category.parent(), category.slug());
                assertEquals("kicad-" + category.title().toLowerCase(Locale.ROOT).replace('_', '-') + "-library", category.slug());
            } else if (!filedIn.equals("kicad")) {
                assertTrue(standing.contains(filedIn), document.slug() + " is filed in " + filedIn + ", which does not stand before it");
            }
            standing.add(document.slug());
        }
        assertTrue(standing.contains("kicad-diode-tht-library"), standing.toString());
    }

    @Test
    void aModelThePlaceHoldsIsNotBroughtAgain() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("design.kicad_sch", SCHEMATIC);
        files.put("design.kicad_pcb", BOARD);
        files.put("design.kicad_pro", "{}");
        CatalogueDocuments holding = slug -> slug.equals(TEST_POINT_MODEL_CODE)
                ? Optional.of(new DocumentNode(ModelModule.MODEL, "Body", slug, null, 1)) : CadLibrary.library().find(slug);
        ImportedDocuments imported = new KiCadDesignImport(() -> InstalledKiCadLibraries.NONE, () -> ONE_MODEL, KiCadLibraryMap.readFrom(KiCadLibraryMap.SHIPPED), KiCadLibraries.readFrom(KiCadLibraries.SHIPPED))
                .read(new ExchangeFiles(files, "design.kicad_pro", "Design"), holding);
        assertTrue(imported.library().stream().noneMatch(document -> document.slug().equals(TEST_POINT_MODEL)));
        assertTrue(imported.files().isEmpty());
    }

    @Test
    void theLibraryFindsAFootprintByTheCodeTheMapPairsKiCadsWith() {
        String code = KiCadLibraryMap.shipped().footprintCodeOf("Capacitor_THT:CP_Radial_D10.0mm_P5.00mm").orElseThrow();
        DocumentNode ours = CadLibrary.library().findCoded(code).orElseThrow();
        assertEquals("footprint", ours.kind().word());
        assertTrue(CadLibrary.library().findCoded("FP.NOTHING:AT-ALL").isEmpty());
    }

    @Test
    void aPartOnAFootprintTheMapPairsLandsOnOursAndNothingIsConverted() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("design.kicad_sch", SCHEMATIC.replace("\"Diode_THT:D_DO-35\"", "\"Diode_THT:D_DO-35_SOD27_P7.62mm_Horizontal\""));
        files.put("design.kicad_pro", "{}");
        ImportedDocuments imported = new KiCadDesignImport(() -> InstalledKiCadLibraries.NONE, () -> NO_MODELS, KiCadLibraryMap.readFrom(KiCadLibraryMap.SHIPPED), KiCadLibraries.readFrom(KiCadLibraries.SHIPPED))
                .read(new ExchangeFiles(files, "design.kicad_pro", "Design"), CadLibrary.library());
        // The part names ours, by its code, wherever the design keeps what it sits on: its component, or the circuit itself.
        DocumentLanguage language = CadLanguage.standard().language();
        String written = String.join(System.lineSeparator(), Stream.concat(imported.library().stream(), imported.designs().stream()).map(language::write).toList());
        String ours = "FP.AXIAL:DO-35-P7.62-H";
        assertTrue(CadLibrary.library().findCoded(ours).isPresent());
        assertTrue(written.contains(ours), ours + " in: " + written.lines().filter(line -> line.contains("footprint")).toList());
        assertTrue(imported.library().stream().noneMatch(document -> document.slug().contains("do-35") && document.slug().startsWith("kicad-")),
                imported.library().stream().map(DocumentNode::slug).toList().toString());
    }

    private ImportedDocuments imported() {
        return imported(BOARD);
    }

    private ImportedDocuments imported(String board) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("design.kicad_sch", SCHEMATIC);
        files.put("design.kicad_pcb", board);
        files.put("design.kicad_pro", "{}");
        return new KiCadDesignImport(() -> InstalledKiCadLibraries.NONE, () -> NO_MODELS, KiCadLibraryMap.readFrom(KiCadLibraryMap.SHIPPED), KiCadLibraries.readFrom(KiCadLibraries.SHIPPED)).read(new ExchangeFiles(files, "design.kicad_pro", "Design"), CadLibrary.library());
    }

    private PcbFabrication fabricated(ImportedDocuments imported) {
        Map<String, DocumentNode> made = new HashMap<>();
        // As the store finds them: by slug, and by the code a reference names them by.
        imported.library().forEach(document -> {
            made.put(document.slug(), document);
            made.put(document.referenceName(), document);
        });
        imported.designs().forEach(document -> made.put(document.slug(), document));
        CatalogueDocuments documents = slug -> Optional.ofNullable(made.get(slug)).or(() -> CadLibrary.library().find(slug));
        return PcbFabrication.of(imported.root(), documents);
    }

    @Test
    void aBusLineComesAcrossWholeAndNamed() {
        DocumentNode design = imported().designs().stream().filter(document -> document.slug().equals("design")).findFirst().orElseThrow();
        String written = org.jmouse.cad.library.CadLanguage.standard().language().write(design);
        assertTrue(written.contains("bus:B1(D"), written);
        assertTrue(written.contains("members: D0 D1 D2"), written);
        // Two stretches meeting end to end, one line of three corners; its label is the bus's and no net of the sheet.
        assertEquals(1, written.lines().filter(line -> line.contains("bus:B")).count(), written);
        assertTrue(!written.contains("D[0..2]"), written);
    }

    /** The board with its diode and hole in a locked group, the hole locked itself, its track locked as KiCad 7 writes it. */
    static final String LOCKED_BOARD = BOARD
            .replace("(footprint \"Diode_THT:D_DO-35\" (layer \"B.Cu\")", "(footprint \"Diode_THT:D_DO-35\" (layer \"B.Cu\") (uuid \"diode-1\")")
            .replace("(footprint \"MountingHole:M3\" (layer \"F.Cu\")", "(footprint \"MountingHole:M3\" (locked yes) (layer \"F.Cu\") (uuid \"hole-1\")")
            .replace("(segment (start 110 60)", "(segment locked (start 110 60)")
            .replace("(zone (net 1) (net_name \"CATHODE\") (layer \"F.Cu\")",
                    "(group \"Front panel\" (uuid \"group-1\") (locked yes) (members \"diode-1\" \"hole-1\")) (zone (net 1) (net_name \"CATHODE\") (layer \"F.Cu\")");

    @Test
    void whatKiCadLockedAndGroupedStaysLockedAndGrouped() {
        String board = LOCKED_BOARD;
        PcbDrawing pcb = fabricated(imported(board)).pcb();
        assertEquals(List.of(new DrawnGroup("front-panel", DrawnState.LOCKED)), pcb.groups());
        PcbDrawing.Placement diode = pcb.placements().stream().filter(placement -> placement.designator().equals("D1")).findFirst().orElseThrow();
        PcbDrawing.Placement hole = pcb.placements().stream().filter(placement -> placement.printed().equals("H1")).findFirst().orElseThrow();
        assertEquals(Arrangement.in("front-panel"), diode.arrangement());
        assertEquals(new Arrangement("front-panel", DrawnState.LOCKED), hole.arrangement());
        assertTrue(pcb.tracks().getFirst().arrangement().locked());
        assertTrue(pcb.drawnGroups().holds(diode.arrangement()));
    }

    @Test
    void theBoardIsWhatTheImportStandsFor() {
        ImportedDocuments imported = imported();
        assertEquals("design-pcb", imported.root().slug());
        assertTrue(imported.designs().stream().anyMatch(document -> document.slug().equals("design")));
    }

    @Test
    void aFootprintOnTheBackStandsWhereKiCadPutsIt() {
        PcbFabrication fabrication = fabricated(imported());
        PcbDrawing.Placement diode = fabrication.parts().stream().filter(part -> part.designator().equals("D1")).findFirst().orElseThrow().placement();
        assertEquals(PcbDrawing.BOTTOM, diode.side());
        // KiCad's frame less the edge's corner.
        assertEquals(10, diode.x());
        assertEquals(10, diode.y());
        // Pad 2 is 7.62 along the footprint: KiCad mirrors Y then turns by 90, which lands it straight up the page.
        PcbDrawing.Point pad = diode.placed(7.62, 0);
        assertEquals(10, pad.x(), 1e-6);
        assertEquals(10 - 7.62, pad.y(), 1e-6);
    }

    @Test
    void aPadCarriesTheNetItsSymbolPinNumberSays() {
        PcbFabrication fabrication = fabricated(imported());
        PcbFabrication.SeatedPart diode = fabrication.parts().stream().filter(part -> part.designator().equals("D1")).findFirst().orElseThrow();
        // A KiCad diode's pad 1 is its cathode, whatever the numbers of the component it became.
        assertEquals("CATHODE", diode.padNets().get("1"));
        assertEquals("ANODE", diode.padNets().get("2"));
    }

    @Test
    void whatOnlyTheBoardHasStandsOnItsOwnFootprintAndNets() {
        PcbFabrication fabrication = fabricated(imported());
        PcbFabrication.SeatedPart hole = fabrication.parts().stream().filter(part -> part.designator().equals("H1")).findFirst().orElseThrow();
        assertTrue(hole.placement().ownedByPcb());
        assertTrue(hole.drawing().isPresent());
        assertEquals("CATHODE", hole.padNets().get("1"));
    }

    @Test
    void itsCopperIsOnItsNetsAndNoteDisagreesWithTheSchematic() {
        ImportedDocuments imported = imported();
        PcbFabrication fabrication = fabricated(imported);
        assertEquals("CATHODE", fabrication.pcb().zones().getFirst().net());
        assertEquals(1, fabrication.pcb().tracks().size());
        assertFalse(imported.notes().stream().anyMatch(note -> note.contains("disagree")), String.valueOf(imported.notes()));
    }

    @Test
    void everyLayerTheBoardDrawsOnIsKept() {
        PcbFabrication fabrication = fabricated(imported());
        Set<String> layers = fabrication.pcb().shapes().stream().map(PcbDrawing.Shape::layer).collect(Collectors.toSet());
        assertEquals(Set.of("top-silkscreen", "bottom-mask", "top-adhesive", "comments"), layers);
        PcbDrawing.Shape window = fabrication.pcb().shapes().stream().filter(shape -> shape.layer().equals("bottom-mask")).findFirst().orElseThrow();
        assertTrue(window.filled());
        assertEquals(2, fabrication.pcb().shapes().stream().filter(shape -> shape.layer().equals("bottom-mask")).count(), "a pour on copper and on the mask at once keeps its window");
        assertTrue(fabrication.pcb().texts().stream().anyMatch(text -> text.layer().equals("documentation") && text.text().equals("Note")));
    }

    @Test
    void whatAPadAndAViaOpenIsWhatKiCadSays() {
        PcbFabrication fabrication = fabricated(imported());
        FootprintDrawing point = fabrication.parts().stream().filter(part -> part.designator().equals("TP1")).findFirst().orElseThrow().drawing().orElseThrow();
        FootprintDrawing.Pad bare = point.pads().stream().filter(pad -> pad.number().equals("1")).findFirst().orElseThrow();
        assertTrue(bare.opensMask());
        assertFalse(bare.opensPaste());
        assertEquals(0.1, bare.maskMargin());
        FootprintDrawing.Pad pasted = point.pads().stream().filter(pad -> pad.number().equals("2")).findFirst().orElseThrow();
        assertTrue(pasted.opensPaste());
        assertEquals(-0.2, pasted.pasteMargin(), 1e-9);
        assertTrue(fabrication.pcb().vias().getFirst().opensMaskOn(PcbDrawing.TOP));
        assertFalse(fabrication.pcb().vias().getFirst().opensMaskOn(PcbDrawing.BOTTOM));
        assertFalse(fabrication.pcb().vias().getLast().opensMaskOn(PcbDrawing.TOP));
    }

    @Test
    void aProjectWithoutASchematicOfItsNameOpensTheOneBesideIt() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("copy.kicad_pro", "{}");
        files.put("design.kicad_sch", SCHEMATIC);
        ImportedDocuments imported = new KiCadDesignImport(() -> InstalledKiCadLibraries.NONE, () -> NO_MODELS, KiCadLibraryMap.readFrom(KiCadLibraryMap.SHIPPED), KiCadLibraries.readFrom(KiCadLibraries.SHIPPED)).read(new ExchangeFiles(files, "copy.kicad_pro", "Copy"), CadLibrary.library());
        assertTrue(imported.notes().stream().anyMatch(note -> note.contains("design.kicad_sch")));
    }
}
