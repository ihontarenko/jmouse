package org.jmouse.cad.plugin.drawings;

import org.jmouse.cad.pcb.PcbLayers;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.Point;
import org.jmouse.cad.settings.CadSettings;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ExportedFile;
import org.jmouse.cad.exchange.ExportedFiles;
import org.jmouse.cad.exchange.pcb.ArtworkFeature;
import org.jmouse.cad.exchange.pcb.PcbArtwork;
import org.jmouse.cad.exchange.pcb.PcbFabrication;
import org.jmouse.cad.exchange.pcb.PcbSolidLayers;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadStandards;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.PcbDrills;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.strokefont.StrokeFont;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a pcb's drawing says, read from a pcb in the language. */
class DrawingsExportTest {

    private static final DocumentLanguage LANGUAGE = CadLanguage.of(CadLanguage.engineModules()).language();
    private static final SheetSettings    SETTINGS = CadSettings.shipped().read(DrawingsPlugin.SHEETS);
    private static final StrokeFont       FONT     = StrokeFont.newStroke();
    private static final SheetLettering   LETTERING = SheetLettering.strokes(FONT, SETTINGS.curveSteps());

    /** The catalogue of a pcb that lays out a circuit of nothing. */
    private static final CatalogueDocuments CIRCUIT = slug -> Optional.of(LANGUAGE.readOne("circuit:empty-circuit(\"Empty\") {\n}\n", slug));

    private static DocumentNode board(String edge) {
        return LANGUAGE.readOne("""
                pcb:drawn("Плата") {
                    circuit(empty-circuit)
                    edge("%s")
                    hole(at: 5 5, drill: 3.2)
                    hole(at: 35 5, drill: 3.2)
                    hole(at: 20 15, drill: 1)
                    dimension(from: 5 5, to: 35 5, offset: 4)
                    text(documentation, size: 1, at: 1 1, "Keep the corners sharp")
                }
                """.formatted(edge), "drawn");
    }

    private static List<Sheet> sheetsOf(DocumentNode document) {
        PcbFabrication fabrication = PcbFabrication.of(document, CIRCUIT);
        Map<String, Double> rules = CadStandards.shipped().pcbRules().valuesOf(fabrication.pcb().rules());
        PcbArtwork drawn = PcbArtwork.of(fabrication, rules, CadSettings.shipped().read(PcbArtwork.SECTION), FONT);
        return new FactorySheets(SETTINGS, LETTERING, drawn, CadSettings.shipped().read(PcbSolidLayers.SECTION),
                rules.get("pcb-thickness"), document.title(), "r1", slug -> slug).sheets();
    }

    private static Sheet sheetNamed(List<Sheet> sheets, String name) {
        return sheets.stream().filter(sheet -> sheet.name().equals(name)).findFirst().orElseThrow();
    }

    private static List<String> wordsOf(Sheet sheet) {
        return sheet.marks().stream().map(mark -> mark instanceof Sheet.Mark.Inked inked ? inked.mark() : mark).filter(Sheet.Mark.Words.class::isInstance)
                .map(mark -> ((Sheet.Mark.Words) mark).text()).toList();
    }

    @Test
    void theFactorysMaskOpensOverCopperLeftBareAndOverWhatIsDrawnOnIt() {
        DocumentNode document = LANGUAGE.readOne("""
                pcb:bare("Bare") {
                    circuit(empty-circuit)
                    edge("M0 0H40V20H0Z")
                    track(top, width: 2, from: 5 5, to: 25 5, finish: bare)
                    track(top, width: 2, from: 5 10, to: 25 10)
                    track(bottom, width: 2, from: 5 15, to: 25 15, finish: bare)
                    shape(top-mask, filled, "M5 8H9V12H5Z")
                }
                """, "bare");
        PcbFabrication fabrication = PcbFabrication.of(document, CIRCUIT);
        Map<String, Double> rules = CadStandards.shipped().pcbRules().valuesOf(fabrication.pcb().rules());
        PcbArtwork.Settings settings = CadSettings.shipped().read(PcbArtwork.SECTION);
        List<ArtworkFeature> top = PcbArtwork.of(fabrication, rules, settings, FONT).maskOpenings(PcbVocabulary.TOP);
        List<ArtworkFeature.Stroke> strokes = top.stream().filter(ArtworkFeature.Stroke.class::isInstance).map(ArtworkFeature.Stroke.class::cast).toList();
        assertEquals(1, strokes.size(), "only the track left bare on this side: " + top);
        assertEquals(2 + settings.maskExpansion() * 2, strokes.getFirst().width(), 1e-9, "wider than its copper by the mask's expansion");
        assertEquals(1, top.stream().filter(ArtworkFeature.Region.class::isInstance).count(), "the window drawn on the mask");
    }

    @Test
    void aSmallPcbIsDrawnLargerOnTheSmallestPaper() {
        List<Sheet> sheets = sheetsOf(board("M0 0H40V20H0Z"));
        assertEquals(List.of("overview", "copper-1", "copper-2", "fabrication", "tables"), sheets.stream().map(Sheet::name).toList(), "no parts: no placement, no materials");
        assertEquals("A4", sheets.getFirst().paper().name());
        assertEquals("5:1  millimetres", sheets.getFirst().scale());
    }

    @Test
    void aLargePcbTakesTheFirstPaperItFitsAtFullSize() {
        Sheet sheet = sheetsOf(board("M0 0H252V92H0Z")).getFirst();
        assertEquals("A3", sheet.paper().name());
        assertEquals("1:1  millimetres", sheet.scale());
    }

    @Test
    void theSheetSaysTheHolesTheSizesAndTheNotes() {
        List<Sheet> sheets = sheetsOf(board("M0 0H40V20H0Z"));
        List<String> drawing = wordsOf(sheetNamed(sheets, "fabrication"));
        assertTrue(drawing.containsAll(List.of("40", "20", "30", "A", "B")), "the overall dimensions, the pcb's own, the holes' marks: " + drawing);
        assertTrue(drawing.containsAll(List.of("Плата", "r1", "5:1  millimetres")), "the title block: " + drawing);
        List<String> tables = wordsOf(sheetNamed(sheets, "tables"));
        assertTrue(tables.containsAll(List.of("A", "B", "1", "3.2", "bare", "2", "Total")), "the tables, on their own sheet: " + tables);
        assertTrue(tables.stream().anyMatch(word -> word.contains("sharp")), "what the pcb notes itself: " + tables);
    }

    @Test
    void everythingOnASheetStaysOnItsPaper() {
        Sheet sheet = sheetNamed(sheetsOf(board("M0 0H40V20H0Z")), "fabrication");
        for (Sheet.Mark mark : sheet.marks()) {
            List<Sheet.Point> points = switch (mark) {
                case Sheet.Mark.Line line -> line.points();
                case Sheet.Mark.Area area -> area.rings().stream().flatMap(List::stream).toList();
                case Sheet.Mark.Circle circle -> List.of(circle.centre());
                case Sheet.Mark.Words word -> List.of(word.at());
                case Sheet.Mark.Inked inked -> List.of();
            };
            for (Sheet.Point point : points) {
                assertTrue(point.x() >= 0 && point.x() <= sheet.paper().width() && point.y() >= 0 && point.y() <= sheet.paper().height(),
                        mark + " is off the paper");
            }
        }
    }

    @Test
    void holesOfOneToolShareAMarkAndPlatedOnesComeFirst() {
        Point at = new Point(0, 0);
        Point along = new Point(3, 0);
        List<FactorySheets.HoleTool> tools = FactorySheets.toolsOf(List.of(new PcbDrills.Drill(false, 3.2, at, at), new PcbDrills.Drill(true, 0.8, at, at),
                new PcbDrills.Drill(true, 0.8, at, at), new PcbDrills.Drill(true, 1, at, along)));
        assertEquals(List.of("A", "B", "C"), tools.stream().map(FactorySheets.HoleTool::mark).toList());
        assertEquals(2, tools.getFirst().drills().size());
        assertEquals(4, tools.get(1).slot(), 1e-9, "a slot's whole length");
        assertFalse(tools.get(2).plated());
    }

    @Test
    void itWritesOnePdfAndADxfForEachSheet() {
        ExportedFiles files = new DrawingsExport(CadSettings.shipped(), StrokeFont.newStroke(), new SheetFonts(List::of, List::of), CadStandards.shipped()).write(board("M0 0H40V20H0Z"), CIRCUIT);
        assertEquals(List.of("drawn-drawing.pdf", "drawn-fabrication.dxf"), files.files().stream().map(ExportedFile::fileName).toList(), "a DXF of what a factory draws from alone");
        assertTrue(new String(files.files().getFirst().content(), StandardCharsets.ISO_8859_1).startsWith("%PDF-"));
        String dxf = files.files().get(1).text();
        assertTrue(dxf.contains("AC1009") && dxf.contains("\r\ndimensions\r\n") && dxf.endsWith("EOF\r\n"));
        assertTrue(dxf.contains("\\U+041F\\U+043B\\U+0430\\U+0442\\U+0430"), "the title beyond ASCII");
    }

    @Test
    void aPcbWithoutAnEdgeHasNoDrawing() {
        DocumentNode none = LANGUAGE.readOne("pcb:drawn(\"X\") {\n    circuit(empty-circuit)\n    hole(at: 5 5, drill: 1)\n}\n", "drawn");
        assertThrows(IllegalArgumentException.class, () -> new DrawingsExport(CadSettings.shipped(), StrokeFont.newStroke(), new SheetFonts(List::of, List::of), CadStandards.shipped()).write(none, CIRCUIT));
    }

    @Test
    void aSchematicFillsItsPaperAndIsNumberedWithThePcb() {
        SchematicPages.Drawn drawn = new SchematicPages.Drawn("top", "Top sheet", "r7",
                List.of(new SchematicPages.Run(new double[]{100, 100, 900, 100, 900, 500}, false, 2, "#14181f", null)),
                List.of(new SchematicPages.Lettering("R1", 500, 300, 14, 90, "middle", "middle", "#14181f")));
        SheetPage page = new SchematicPages(SETTINGS, LETTERING).page(drawn);
        Sheet sheet = new SheetTitles(SETTINGS, "Плата").titled(List.of(page)).getFirst();
        assertEquals("A3", sheet.paper().name());
        Sheet.Mark.Line line = sheet.marks().stream().filter(Sheet.Mark.Inked.class::isInstance).map(mark -> ((Sheet.Mark.Inked) mark).mark())
                .filter(Sheet.Mark.Line.class::isInstance).map(Sheet.Mark.Line.class::cast).findFirst().orElseThrow();
        double across = line.points().get(1).x() - line.points().getFirst().x();
        assertTrue(across > sheet.paper().width() * 0.8, "it takes the paper's width: " + across);
        assertTrue(line.points().get(2).y() < line.points().get(1).y(), "the drawing's Y runs down, the paper's up");
        Sheet.Mark.Words words = sheet.marks().stream().filter(Sheet.Mark.Inked.class::isInstance).map(mark -> ((Sheet.Mark.Inked) mark).mark())
                .filter(Sheet.Mark.Words.class::isInstance).map(Sheet.Mark.Words.class::cast).findFirst().orElseThrow();
        assertEquals(270, words.rotation(), 1e-9, "clockwise on the drawing, anticlockwise on the paper");
        List<String> said = wordsOf(sheet);
        assertTrue(said.containsAll(List.of("Плата", "Schematic · Top sheet", "r7", "1 of 1", "to fit")), "its title block: " + said);
    }

    @Test
    void aDocumentHasACoverAndContentsAndCountsThemIn() {
        SheetSettings settings = SETTINGS;
        DocumentFront front = new DocumentFront(settings, LETTERING);
        SheetFacts facts = new SheetFacts("Плата", "Автор", "r1", "2026-11-04", "2026", List.of(new SheetFacts.Fact("Parts", "33")));
        Sheet cover = new SheetTitles(settings, "Плата").titled(List.of(front.cover(facts))).getFirst();
        List<String> said = wordsOf(cover);
        assertTrue(said.containsAll(List.of("jmCAD", "Плата", "2026", "Автор", "r1", "2026-11-04", "33")), "what a cover tells: " + said);
        assertTrue(cover.marks().stream().noneMatch(mark -> mark instanceof Sheet.Mark.Line line && line.layer() == SheetLayer.FRAME && line.closed()), "a cover has no frame");
        SheetPage body = new SheetPage("x", "A sheet", settings, settings.papers().getFirst(), "", LETTERING, "r1", SheetPurpose.DOCUMENT);
        List<String> contents = wordsOf(new SheetTitles(settings, "Плата").titled(front.contents(List.of(body), "r1")).getFirst());
        assertTrue(contents.containsAll(List.of("3", "A sheet")), "the first sheet after the cover and its contents is the third: " + contents);
    }

    @Test
    void lengthsAreSaidShort() {
        assertEquals("3.2", FactorySheets.length(3.2));
        assertEquals("30", FactorySheets.length(30));
        assertEquals("0.333", FactorySheets.length(1 / 3.0));
    }
}
