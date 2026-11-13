package org.jmouse.cad.altium;

import org.jmouse.cad.exchange.imported.SheetMarks;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.simulation.CatalogueDocuments;

import org.jmouse.altium.AltiumSchematic;
import org.jmouse.altium.AltiumSchematicReader;
import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.library.CadLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("an Altium sheet read as jM documents")
class AltiumSchematicImportTest {

    /** The marks as the shipped library draws them. */
    private static final SheetMarks MARKS = SheetMarks.of(CadLibrary.library(), AltiumLibraries.shipped().markSymbols());

    /**
     * A resistor turned upright: its pin 1 under a label IN, its pin 2 wired down to a ground port; a second resistor
     * left of it whose pin touches the label's net through a wire to a label of the same name.
     */
    private static final String SHEET = String.join("\n",
            "|HEADER=Protel for Windows - Schematic Capture Ascii File Version 5.0|WEIGHT=9",
            "|RECORD=31|FONTIDCOUNT=1",
            "|RECORD=1|LIBREFERENCE=RES|COMPONENTDESCRIPTION=Resistor|PARTCOUNT=2|LOCATION.X=100|LOCATION.Y=200|ORIENTATION=1|CURRENTPARTID=1",
            "|RECORD=14|OWNERINDEX=1|OWNERPARTID=1|LOCATION.X=96|LOCATION.Y=190|CORNER.X=104|CORNER.Y=210|ISSOLID=T",
            "|RECORD=2|OWNERINDEX=1|OWNERPARTID=1|PINCONGLOMERATE=1|PINLENGTH=10|LOCATION.X=100|LOCATION.Y=210|NAME=1|DESIGNATOR=1|ELECTRICAL=4",
            "|RECORD=2|OWNERINDEX=1|OWNERPARTID=1|PINCONGLOMERATE=3|PINLENGTH=10|LOCATION.X=100|LOCATION.Y=190|NAME=2|DESIGNATOR=2|ELECTRICAL=4",
            "|RECORD=34|OWNERINDEX=1|LOCATION.X=110|LOCATION.Y=205|TEXT=R1|NAME=Designator",
            "|RECORD=27|LOCATIONCOUNT=2|X1=100|Y1=180|X2=100|Y2=160",
            "|RECORD=17|STYLE=4|ORIENTATION=3|LOCATION.X=100|LOCATION.Y=160|TEXT=GND",
            "|RECORD=25|LOCATION.X=100|LOCATION.Y=220|TEXT=IN",
            "|RECORD=1|LIBREFERENCE=RES|COMPONENTDESCRIPTION=Resistor|PARTCOUNT=2|LOCATION.X=40|LOCATION.Y=220|CURRENTPARTID=1",
            "|RECORD=14|OWNERINDEX=9|OWNERPARTID=1|LOCATION.X=30|LOCATION.Y=216|CORNER.X=50|CORNER.Y=224|ISSOLID=T",
            "|RECORD=2|OWNERINDEX=9|OWNERPARTID=1|PINCONGLOMERATE=2|PINLENGTH=10|LOCATION.X=30|LOCATION.Y=220|NAME=1|DESIGNATOR=1|ELECTRICAL=4",
            "|RECORD=2|OWNERINDEX=9|OWNERPARTID=1|PINCONGLOMERATE=0|PINLENGTH=10|LOCATION.X=50|LOCATION.Y=220|NAME=2|DESIGNATOR=2|ELECTRICAL=4",
            "|RECORD=34|OWNERINDEX=9|LOCATION.X=40|LOCATION.Y=228|TEXT=R2|NAME=Designator",
            "|RECORD=27|LOCATIONCOUNT=2|X1=60|Y1=220|X2=80|Y2=220",
            "|RECORD=25|LOCATION.X=80|LOCATION.Y=220|TEXT=IN",
            "");

    /** The divider with a bus line beside it, drawn in two stretches that meet, named by the label D[0..3] standing on it. */
    private static final String WITH_BUS = SHEET + String.join("\n",
            "|RECORD=26|LOCATIONCOUNT=2|X1=200|Y1=100|X2=200|Y2=150",
            "|RECORD=26|LOCATIONCOUNT=2|X1=200|Y1=150|X2=260|Y2=150",
            "|RECORD=25|LOCATION.X=230|LOCATION.Y=150|TEXT=D[0..3]",
            "");

    private final DocumentLanguage language = CadLanguage.standard().language();

    private ImportedDocuments imported() {
        AltiumSchematic sheet = AltiumSchematicReader.read("divider.SchDoc", SHEET.getBytes(StandardCharsets.US_ASCII));
        return AltiumSchematicImport.read(sheet, "divider.SchDoc", "Divider", AltiumLibraries.shipped(), MARKS, CatalogueDocuments.empty());
    }

    /** The upright resistor's footprint, as Altium names it: its implementation list holding R0805 current, no library said. */
    private static String withFootprint(String footprint) {
        int records = SHEET.split("\n").length - 1;
        return SHEET + String.join("\n",
                "|RECORD=44|OWNERINDEX=1",
                "|RECORD=45|OWNERINDEX=" + records + "|MODELNAME=" + footprint + "|MODELTYPE=PCBLIB|ISCURRENT=T",
                "");
    }

    /** Where an import is read, holding these footprints by their codes. */
    private static CatalogueDocuments holding(String... codes) {
        return new CatalogueDocuments() {
            @Override
            public Optional<DocumentNode> find(String slug) {
                return Optional.empty();
            }

            @Override
            public List<String> codesNamed(TechnicalClass kind, String name) {
                return List.of(codes).stream().filter(code -> kind.nameIn(code).filter(name::equals).isPresent()).toList();
            }
        };
    }

    private ImportedDocuments importedWith(String footprint, CatalogueDocuments catalogue) {
        AltiumSchematic sheet = AltiumSchematicReader.read("divider.SchDoc", withFootprint(footprint).getBytes(StandardCharsets.US_ASCII));
        return AltiumSchematicImport.read(sheet, "divider.SchDoc", "Divider", AltiumLibraries.shipped(), MARKS, catalogue);
    }

    @Test
    @DisplayName("names a part's footprint by the code of the one footprint of that name a PCB library imported before brought")
    void aFootprintNamedBareIsTheOneOfThatName() {
        ImportedDocuments imported = importedWith("R0805", holding("FP.RCL:R0805", "FP.RCL:R0603", "FP.CHIPS:SOT-23"));
        String component = language.write(imported.library().get(1));
        assertTrue(component.contains("footprint(\"FP.RCL:R0805\")"), component);
        assertTrue(component.contains("package: \"R0805\""), component);
        assertTrue(imported.notes().stream().noneMatch(note -> note.contains("R0805")), imported.notes().toString());
    }

    @Test
    @DisplayName("keeps a footprint's name as written, and says so, where none or several of that name are held")
    void aFootprintNoneOrSeveralHoldIsSaid() {
        ImportedDocuments none = importedWith("R0805", holding("FP.RCL:R0603"));
        assertTrue(!language.write(none.library().get(1)).contains("footprint("), language.write(none.library().get(1)));
        assertTrue(none.notes().stream().anyMatch(note -> note.startsWith("Footprint R0805: none")), none.notes().toString());
        ImportedDocuments several = importedWith("R0805", holding("FP.RCL:R0805", "FP.CHIP:R0805"));
        assertTrue(several.notes().stream().anyMatch(note -> note.contains("FP.RCL:R0805, FP.CHIP:R0805")), several.notes().toString());
    }

    @Test
    @DisplayName("files a part by the library it was placed from, a category of its own under Altium's, standing before it")
    void aPartIsFiledByItsLibrary() {
        String placed = SHEET.replace("|RECORD=1|LIBREFERENCE=RES|", "|RECORD=1|LIBREFERENCE=RES|SOURCELIBRARYNAME=Miscellaneous Devices.IntLib|");
        AltiumSchematic sheet = AltiumSchematicReader.read("divider.SchDoc", placed.getBytes(StandardCharsets.US_ASCII));
        AltiumLibraries filing = AltiumLibraries.shipped();
        ImportedDocuments imported = AltiumSchematicImport.read(sheet, "divider.SchDoc", "Divider", filing, MARKS, CatalogueDocuments.empty());
        String category = filing.libraryCategory() + "-miscellaneous-devices-library";
        assertEquals(List.of(category, "altium-res-symbol", "altium-res"), imported.library().stream().map(DocumentNode::slug).toList());
        assertTrue(imported.library().stream().skip(1).allMatch(document -> language.write(document).contains("category: " + category)));
        assertTrue(language.write(imported.library().get(2)).contains("Altium part Miscellaneous Devices.IntLib:RES"));
    }

    @Test
    @DisplayName("every document it makes is one the language reads back; a library part is made once")
    void everyDocumentReadsBack() {
        ImportedDocuments imported = imported();
        assertEquals(List.of("altium-res-symbol", "altium-res"), imported.library().stream().map(DocumentNode::slug).toList());
        for (DocumentNode document : imported.library()) {
            assertDoesNotThrow(() -> language.readOne(language.write(document), document.slug()), document.slug());
        }
        assertDoesNotThrow(() -> language.readOne(language.write(imported.root()), imported.root().slug()));
    }

    @Test
    @DisplayName("a bus line comes across whole, named by the label on it with the nets it carries; that label joins no net")
    void aBusLineComesAcrossNamed() {
        AltiumSchematic sheet = AltiumSchematicReader.read("divider.SchDoc", WITH_BUS.getBytes(StandardCharsets.US_ASCII));
        String circuit = language.write(AltiumSchematicImport.read(sheet, "divider.SchDoc", "Divider", AltiumLibraries.shipped(), MARKS, CatalogueDocuments.empty()).root());
        assertTrue(circuit.contains("bus:B1(D"), circuit);
        assertTrue(circuit.contains("D0") && circuit.contains("D3"), circuit);
        assertEquals(1, circuit.split("bus:", -1).length - 1, circuit);
        assertTrue(!circuit.contains("D[0..3]"), circuit);
        assertDoesNotThrow(() -> language.readOne(circuit, "divider"));
    }

    @Test
    @DisplayName("a net is what wires and shared points join, named by its label or port; labels of one name are one net")
    void netsAreTraced() {
        String circuit = language.write(imported().root());
        assertTrue(circuit.contains("part:R1(altium-res, [pin1: IN, pin2: 0])"), circuit);
        assertTrue(circuit.contains("part:R2(altium-res, [pin2: IN])"), circuit);
        assertTrue(circuit.contains("rotate: 270"), circuit);
        assertTrue(circuit.contains("(ground, at:"), circuit);
    }
}
