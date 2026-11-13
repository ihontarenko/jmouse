package org.jmouse.cad.altium;

import org.jmouse.altium.AltiumPcbLibrary;
import org.jmouse.altium.AltiumPcbLibrary.Arc;
import org.jmouse.altium.AltiumPcbLibrary.Body;
import org.jmouse.altium.AltiumPcbLibrary.Expansion;
import org.jmouse.altium.AltiumPcbLibrary.Fill;
import org.jmouse.altium.AltiumPcbLibrary.Footprint;
import org.jmouse.altium.AltiumPcbLibrary.HoleShape;
import org.jmouse.altium.AltiumPcbLibrary.Model;
import org.jmouse.altium.AltiumPcbLibrary.Pad;
import org.jmouse.altium.AltiumPcbLibrary.PadShape;
import org.jmouse.altium.AltiumPcbLibrary.Point;
import org.jmouse.altium.AltiumPcbLibrary.Rotation;
import org.jmouse.altium.AltiumPcbLibrary.Text;
import org.jmouse.altium.AltiumPcbLibrary.Track;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.DocumentIndex;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An Altium PCB library converted: each footprint and each embedded model a document named by its code in the library's
 * namespace, its pads as Altium makes them, its lines on the layers its library names, and whose it is as the reader said.
 */
class AltiumPcbLibraryImportTest {

    private static final double MIL = 10_000;
    private static final Provenance WHOSE = new Provenance(null, "Maxime Clement", "MIT");
    /** The language the library's documents are read with: the engine's own, and the 3D models a footprint stands with. */
    private final DocumentLanguage language = CadLanguage.shipped().language();

    private static Pad pad(String name, int layer, double x, PadShape shape, double hole, HoleShape holeShape, double slot, int corner,
                           Expansion paste, double pasteMargin) {
        return new Pad(name, layer, new Point(x * MIL, 0), 20 * MIL, 25 * MIL, shape, 0, hole * MIL, holeShape, slot * MIL, 0, true, corner,
                paste, pasteMargin * MIL, Expansion.MANUAL, 2 * MIL, false, false);
    }

    private static AltiumPcbLibrary library() {
        Footprint resistor = new Footprint("R0402", "Chip resistor 0402", 0,
                List.of(pad("1", 1, -20, PadShape.RECTANGLE, 0, HoleShape.ROUND, 0, 0, Expansion.RULE, 0),
                        pad("2", 1, 20, PadShape.ROUNDED_RECTANGLE, 0, HoleShape.ROUND, 0, 50, Expansion.MANUAL, -100),
                        pad("3", 74, 60, PadShape.ROUND, 40, HoleShape.SLOT, 80, 0, Expansion.RULE, 0),
                        pad("4", 32, 100, PadShape.OCTAGON, 0, HoleShape.ROUND, 0, 0, Expansion.RULE, 0)),
                List.of(),
                List.of(new Track(33, new Point(-37.5 * MIL, 20 * MIL), new Point(37.5 * MIL, 20 * MIL), 5 * MIL),
                        new Track(71, new Point(-37.5 * MIL, -20 * MIL), new Point(37.5 * MIL, -20 * MIL), 5 * MIL),
                        new Track(56, new Point(0, 0), new Point(10 * MIL, 0), 5 * MIL)),
                List.of(new Arc(33, new Point(0, 0), 10 * MIL, 0, 360, 5 * MIL)),
                List.of(new Fill(35, new Point(-10 * MIL, -10 * MIL), new Point(10 * MIL, 10 * MIL), 0)),
                List.of(),
                List.of(new Body(69, Map.of("MODELID", "{ABC}", "MODEL.2D.X", "1mil", "MODEL.2D.Y", "2mil", "MODEL.3D.DZ", "0mil",
                        "MODEL.3D.ROTX", "90", "OVERALLHEIGHT", "13.78mil"))),
                // The designator above the part, upright; a word of the library's own beside it.
                List.of(new Text(33, new Point(-37.5 * MIL, 25 * MIL), 25 * MIL, 0, false, 5 * MIL, Text.DESIGNATOR),
                        new Text(33, new Point(40 * MIL, 0), 25 * MIL, 0, false, 5 * MIL, "DNI")),
                Map.of());
        Model model = new Model("{abc}", "0402 Res.step", new Rotation(0, 0, 0), 0, "ISO-10303-21;".getBytes(StandardCharsets.US_ASCII));
        return new AltiumPcbLibrary(List.of(resistor), List.of(model), Map.of(71, 1, 69, 26));
    }

    private ImportedDocuments imported() {
        // As the plugin reads it: whose it is as the reader says, then filed by its library.
        return AltiumPcbLibraryImport.read(library(), "rcl.PcbLib", AltiumLibraries.shipped()).whoseAsStated(WHOSE, "rcl.PcbLib")
                .filedBy(AltiumLibraries.shipped());
    }

    private DocumentNode named(ImportedDocuments imported, String code) {
        return imported.library().stream().filter(document -> document.referenceName().equals(code)).findFirst().orElseThrow();
    }

    @Test
    void eachDocumentIsNamedByItsCodeInTheLibrarysNamespaceAndReadsBack() {
        ImportedDocuments imported = imported();
        DocumentNode footprint = named(imported, "FP.RCL:R0402");
        DocumentNode model = named(imported, "MD.RCL:0402-RES");
        for (DocumentNode document : List.of(footprint, model)) {
            assertDoesNotThrow(() -> language.readOne(language.write(document), document.slug()), document.slug());
        }
        assertEquals(1, imported.files().size());
        assertTrue(imported.library().indexOf(model) < imported.library().indexOf(footprint), "a model before the footprint naming it");
    }

    @Test
    void theFootprintNamesItsModelByCodeAndStandsItWhereAltiumDoes() {
        DocumentNode read = readBack(named(imported(), "FP.RCL:R0402"));
        FootprintDrawing drawing = FootprintDrawing.of(read);
        assertEquals(1, drawing.models().size());
        FootprintDrawing.Model model = drawing.models().getFirst();
        assertEquals("MD.RCL:0402-RES", model.reference());
        // A mil and two, to the footprint's three places.
        assertEquals(0.025, model.offset().get(0), 1e-9);
        assertEquals(0.051, model.offset().get(1), 1e-9);
        assertEquals(-90, model.rotation().get(0), 1e-9);
        assertTrue(language.write(read).contains("body(height: 0.35)"), language.write(read));
    }

    @Test
    void padsAreAltiumsAsOursWritesThem() {
        String written = language.write(named(imported(), "FP.RCL:R0402"));
        assertTrue(written.contains("pad:1(surface, rectangle, at: -0.508 0, size: 0.508 x 0.635, mask-margin: 0.051)"), written);
        // A paste drawn smaller than nothing is drawn apart, as a fill: the pad opens its mask alone.
        assertTrue(written.contains("pad:2(surface, rounded-rectangle, at: 0.508 0, size: 0.508 x 0.635, roundness: 0.25, opens: mask"), written);
        // A slot is its length along the pad and its width across.
        assertTrue(written.contains("pad:3(through-hole, oval, at: 1.524 0, size: 0.508 x 0.635, drill: 2.032 x 1.016"), written);
        // On the bottom layer: the far face; an octagon is round copper.
        assertTrue(written.contains("pad:4(surface, oval, at: 2.54 0, size: 0.508 x 0.635, face: far"), written);
    }

    @Test
    void linesStandOnTheLayersTheLibraryNamesAndTheRestIsSaid() {
        ImportedDocuments imported = imported();
        String written = language.write(named(imported, "FP.RCL:R0402"));
        assertTrue(written.contains("outline(silkscreen,"), written);
        assertTrue(written.contains("outline(fabrication,"), written);
        assertTrue(written.contains("outline(paste, filled,"), written);
        assertTrue(imported.notes().stream().anyMatch(note -> note.contains("Altium layer 56")), imported.notes().toString());
        assertTrue(imported.notes().stream().anyMatch(note -> note.contains("text")), imported.notes().toString());
    }

    @Test
    void theDesignatorStandsWhereAltiumPrintsItAndAnyOtherWordIsSaid() {
        ImportedDocuments imported = imported();
        String footprint = language.write(named(imported, "FP.RCL:R0402"));
        // 37.5 mil left, raised by 25 mil and half its 25 mil letters: y = -(25 + 12.5) mil, upright, on the top overlay.
        assertTrue(footprint.contains("field(reference, at: -0.952 -0.952, size: 0.635, layer: silkscreen, anchor: start)"), footprint);
        assertTrue(imported.notes().contains("R0402: 1 × text not carried"), imported.notes().toString());
    }

    @Test
    void whoseItIsTravelsWithEveryDocument() {
        for (DocumentNode document : imported().library()) {
            if (document.kind().word().equals("category")) {
                continue;
            }
            Provenance provenance = Provenance.of(document);
            assertEquals("Maxime Clement", provenance.authors(), document.slug());
            assertEquals("MIT", provenance.license(), document.slug());
            assertTrue(provenance.source().contains("rcl.PcbLib"), provenance.source());
        }
    }

    @Test
    void aLibraryNobodySaidWhoseIsItClaimsNoneAndSaysSo() {
        ImportedDocuments imported = AltiumPcbLibraryImport.read(library(), "rcl.PcbLib", AltiumLibraries.shipped())
                .whoseAsStated(Provenance.OURS, "rcl.PcbLib");
        assertTrue(imported.library().stream().noneMatch(document -> Provenance.of(document).borrowed()));
        assertTrue(imported.notes().stream().anyMatch(note -> note.contains("said no holder")), imported.notes().toString());
    }

    @Test
    void aModelWithNoNameIsItsIdentifierAndTwoFilesOfOneNameAreTwoModelsUnlessTheyAreOne() {
        byte[] step = "ISO-10303-21;".getBytes(StandardCharsets.US_ASCII);
        byte[] other = "ISO-10303-21; other".getBytes(StandardCharsets.US_ASCII);
        List<Model> models = List.of(new Model("{A06242D6-842D-40C7}", "", new Rotation(0, 0, 0), 0, step),
                new Model("{11111111-0000}", "DSG0008A.stp", new Rotation(0, 0, 0), 0, step),
                new Model("{22222222-0000}", "DSG0008A.stp", new Rotation(0, 0, 0), 0, step),
                new Model("{33333333-0000}", "DSG0008A.stp", new Rotation(0, 0, 0), 0, other));
        ImportedDocuments imported = AltiumPcbLibraryImport.read(new AltiumPcbLibrary(List.of(), models, Map.of()), "chips.PcbLib",
                AltiumLibraries.shipped());
        assertEquals(List.of("MD.CHIPS:A06242D6-842D-40C7", "MD.CHIPS:DSG0008A", "MD.CHIPS:DSG0008A-33333333"),
                imported.library().stream().map(DocumentNode::referenceName).toList());
    }

    private DocumentNode readBack(DocumentNode document) {
        DocumentNode read = language.readOne(language.write(document), document.slug());
        assertEquals("FP.RCL:R0402", DocumentIndex.of(read).code());
        return read;
    }
}
