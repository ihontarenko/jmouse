package org.jmouse.cad.eagle;

import org.jmouse.cad.exchange.imported.SheetMarks;
import org.jmouse.cad.library.CadLibrary;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.DocumentIndex;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.eagle.EagleLibrary;
import org.jmouse.eagle.EagleReader;
import org.jmouse.eagle.EagleSchematic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("an Eagle schematic read as jM documents")
class EagleSchematicImportTest {

    /** The marks as the shipped library draws them. */
    private static final SheetMarks MARKS = SheetMarks.of(CadLibrary.library(), EagleLibraries.standard().markSymbols());

    /**
     * A divider and a dual amplifier: a resistor turned a quarter, one mirrored, a ground supply, a net with a label,
     * a part of three gates of which two have signal pins and one power.
     */
    static final String SCHEMATIC = """
            <?xml version="1.0" encoding="utf-8"?>
            <eagle version="9.6.2"><drawing><schematic>
            <libraries><library name="test">
            <packages>
            <package name="0805">
            <smd name="1" x="-0.95" y="0" dx="1.1" dy="1.4" layer="1" roundness="50"/>
            <smd name="2" x="0.95" y="0" dx="1.1" dy="1.4" layer="1"/>
            <wire x1="-0.4" y1="0.6" x2="0.4" y2="0.6" width="0.1524" layer="21"/>
            <text x="-1.27" y="1.27" size="1.27" layer="25">&gt;NAME</text>
            <text x="-1.27" y="-2.54" size="1.27" layer="27">&gt;VALUE</text>
            </package>
            <package name="SO8">
            <smd name="1" x="-2.7" y="1.905" dx="1.5" dy="0.6" layer="1"/>
            <smd name="2" x="-2.7" y="0.635" dx="1.5" dy="0.6" layer="1"/>
            <smd name="3" x="-2.7" y="-0.635" dx="1.5" dy="0.6" layer="1"/>
            <smd name="4" x="-2.7" y="-1.905" dx="1.5" dy="0.6" layer="1"/>
            <smd name="5" x="2.7" y="-1.905" dx="1.5" dy="0.6" layer="1"/>
            <smd name="6" x="2.7" y="-0.635" dx="1.5" dy="0.6" layer="1"/>
            <smd name="7" x="2.7" y="0.635" dx="1.5" dy="0.6" layer="1"/>
            <smd name="8" x="2.7" y="1.905" dx="1.5" dy="0.6" layer="1"/>
            </package>
            <package name="SOT23">
            <smd name="P$1" x="-0.95" y="-1" dx="0.6" dy="0.7" layer="1"/>
            <smd name="P$2" x="0.95" y="-1" dx="0.6" dy="0.7" layer="1"/>
            <smd name="3" x="0" y="1" dx="0.6" dy="0.7" layer="1"/>
            </package>
            </packages>
            <symbols>
            <symbol name="R">
            <wire x1="-2.54" y1="-0.889" x2="2.54" y2="-0.889" width="0.254" layer="94"/>
            <wire x1="2.54" y1="0.889" x2="-2.54" y2="0.889" width="0.254" layer="94"/>
            <text x="0" y="1.524" size="1.778" layer="95">&gt;NAME</text>
            <pin name="1" x="-5.08" y="0" visible="off" length="short" direction="pas"/>
            <pin name="2" x="5.08" y="0" visible="off" length="short" direction="pas" rot="R180"/>
            </symbol>
            <symbol name="GND">
            <wire x1="-1.905" y1="0" x2="1.905" y2="0" width="0.254" layer="94"/>
            <pin name="GND" x="0" y="0" visible="off" length="point" direction="sup" rot="R270"/>
            </symbol>
            <symbol name="HALF">
            <wire x1="-5.08" y1="5.08" x2="5.08" y2="0" width="0.4064" layer="94"/>
            <pin name="IN+" x="-7.62" y="2.54" length="short" direction="in"/>
            <pin name="IN-" x="-7.62" y="-2.54" length="short" direction="in" function="dot"/>
            <pin name="OUT" x="7.62" y="0" length="short" direction="out" rot="R180"/>
            </symbol>
            <symbol name="SUPPLY">
            <pin name="V+" x="0" y="5.08" length="short" direction="pwr" rot="R270"/>
            <pin name="V-" x="0" y="-5.08" length="short" direction="pwr" rot="R90"/>
            </symbol>
            </symbols>
            <devicesets>
            <deviceset name="RESISTOR" prefix="R">
            <gates><gate name="G$1" symbol="R" x="0" y="0"/></gates>
            <devices><device name="-0805" package="0805">
            <connects><connect gate="G$1" pin="1" pad="1"/><connect gate="G$1" pin="2" pad="2"/></connects>
            </device></devices>
            </deviceset>
            <deviceset name="GND" prefix="">
            <gates><gate name="1" symbol="GND" x="0" y="0"/></gates>
            <devices><device name=""/></devices>
            </deviceset>
            <deviceset name="DUAL" prefix="U">
            <gates><gate name="A" symbol="HALF" x="0" y="10"/><gate name="B" symbol="HALF" x="0" y="-10"/>
            <gate name="P" symbol="SUPPLY" x="20" y="0"/></gates>
            <devices><device name="" package="SO8">
            <connects>
            <connect gate="A" pin="OUT" pad="1"/><connect gate="A" pin="IN-" pad="2"/><connect gate="A" pin="IN+" pad="3"/>
            <connect gate="P" pin="V-" pad="4"/><connect gate="B" pin="IN+" pad="5"/><connect gate="B" pin="IN-" pad="6"/>
            <connect gate="B" pin="OUT" pad="7"/><connect gate="P" pin="V+" pad="8"/>
            </connects>
            </device></devices>
            </deviceset>
            </devicesets>
            </library></libraries>
            <parts>
            <part name="R1" library="test" deviceset="RESISTOR" device="-0805" value="10k"/>
            <part name="R2" library="test" deviceset="RESISTOR" device="-0805" value="10k"/>
            <part name="GND1" library="test" deviceset="GND" device=""/>
            <part name="U1" library="test" deviceset="DUAL" device=""/>
            </parts>
            <sheets><sheet>
            <instances>
            <instance part="R1" gate="G$1" x="10.16" y="20.32" rot="R90"/>
            <instance part="R2" gate="G$1" x="20.32" y="25.4" rot="MR0"/>
            <instance part="GND1" gate="1" x="10.16" y="10.16"/>
            <instance part="U1" gate="A" x="50.8" y="25.4"/>
            <instance part="U1" gate="B" x="50.8" y="10.16"/>
            <instance part="U1" gate="P" x="60.96" y="17.78"/>
            </instances>
            <nets>
            <net name="GND" class="0"><segment>
            <pinref part="R1" gate="G$1" pin="1"/><pinref part="GND1" gate="1" pin="GND"/>
            <wire x1="10.16" y1="15.24" x2="10.16" y2="10.16" width="0.1524" layer="91"/>
            </segment></net>
            <net name="OUT" class="0"><segment>
            <pinref part="R1" gate="G$1" pin="2"/><pinref part="R2" gate="G$1" pin="2"/>
            <wire x1="10.16" y1="25.4" x2="15.24" y2="25.4" width="0.1524" layer="91"/>
            </segment><segment>
            <pinref part="U1" gate="A" pin="IN+"/>
            <wire x1="43.18" y1="27.94" x2="38.1" y2="27.94" width="0.1524" layer="91"/>
            <label x="38.1" y="27.94" size="1.778" layer="95"/>
            </segment></net>
            <net name="IN" class="0"><segment>
            <pinref part="R2" gate="G$1" pin="1"/>
            <wire x1="25.4" y1="25.4" x2="30.48" y2="25.4" width="0.1524" layer="91"/>
            <label x="30.48" y="25.4" size="1.778" layer="95"/>
            </segment></net>
            </nets>
            </sheet></sheets>
            </schematic></drawing></eagle>
            """;

    private final DocumentLanguage language = CadLanguage.standard().language();

    @Test
    void aBusLineComesAcrossNamed() {
        String withBus = SCHEMATIC.replace("<nets>", """
                <busses><bus name="B$1:D[0..1],CLK"><segment>
                <wire x1="0" y1="50" x2="20" y2="50" width="0.762" layer="92"/>
                <wire x1="20" y1="50" x2="20" y2="70" width="0.762" layer="92"/>
                </segment></bus></busses>
                <nets>""");
        EagleSchematic schematic = EagleReader.read("divider.sch", withBus).schematic().orElseThrow();
        String written = language.write(EagleSchematicImport.read(schematic, "divider.sch", "Divider", EagleLibraries.standard(), MARKS).documents().designs().getFirst());
        assertTrue(written.contains("bus:B1(D"), written);
        assertTrue(written.contains("D0") && written.contains("D1") && written.contains("CLK"), written);
    }

    @Test
    @DisplayName("files each Eagle library as a category of its own, under Eagle's, standing before what it holds")
    void eachLibraryIsACategory() {
        EagleSchematic schematic = EagleReader.read("divider.sch", SCHEMATIC).schematic().orElseThrow();
        EagleLibraries filing = EagleLibraries.standard();
        ImportedDocuments read = EagleDesignImport.read(schematic, "divider.sch", Optional.empty(), "Divider", filing, MARKS);
        String category = filing.libraryCategory() + "-test-library";
        List<DocumentNode> library = read.library();
        assertEquals(category, library.getFirst().slug(), library.stream().map(DocumentNode::slug).toList().toString());
        assertTrue(library.stream().filter(document -> !document.slug().equals(category))
                .allMatch(document -> text(document).contains("category: " + category)), library.stream().map(this::text).toList().toString());
        assertEquals(Optional.of("SparkFun-Resistors"), EagleNames.libraryOf("Eagle package SparkFun-Resistors:AXIAL-0.3"));
        assertEquals(Optional.empty(), EagleNames.libraryOf("KiCad footprint Package_SO:SOIC-8"));
    }

    private ImportedDocuments imported() {
        EagleSchematic schematic = EagleReader.read("divider.sch", SCHEMATIC).schematic().orElseThrow();
        return EagleSchematicImport.read(schematic, "divider.sch", "Divider", EagleLibraries.standard(), MARKS).documents();
    }

    private String text(DocumentNode document) {
        return language.write(document);
    }

    @Test
    @DisplayName("every document it makes is one the language reads back")
    void everyDocumentReadsBack() {
        ImportedDocuments imported = imported();
        List<String> slugs = imported.library().stream().map(DocumentNode::slug).toList();
        assertEquals(List.of("eagle-test-0805-footprint", "eagle-test-so8-footprint", "eagle-test-r-symbol",
                "eagle-test-resistor-0805", "eagle-test-dual"), slugs);
        for (DocumentNode document : imported.library()) {
            assertDoesNotThrow(() -> language.readOne(text(document), document.slug()), document.slug());
        }
        assertDoesNotThrow(() -> language.readOne(text(imported.root()), imported.root().slug()));
    }

    @Test
    @DisplayName("a component names its symbol and its footprint by their codes, and the import holds a document of each code")
    void referencesAreCodesTheImportHolds() {
        ImportedDocuments imported = imported();
        List<DocumentIndex> indexes = imported.library().stream()
                .map(document -> DocumentIndex.of(language.readOne(text(document), document.slug()))).toList();
        List<String> codes = indexes.stream().map(DocumentIndex::code).filter(java.util.Objects::nonNull).toList();
        assertEquals(List.of("FP.TEST:0805", "FP.TEST:SO8", "SY.TEST:R"), codes);
        DocumentIndex resistor = indexes.stream().filter(index -> index.slug().equals("eagle-test-resistor-0805")).findFirst().orElseThrow();
        assertEquals(List.of("SY.TEST:R", "FP.TEST:0805"), resistor.references().stream()
                .filter(reference -> !reference.relation().equals("extends") && !reference.relation().equals("use"))
                .map(DocumentIndex.Reference::slug).toList());
        for (DocumentIndex index : indexes) {
            for (DocumentIndex.Reference reference : index.references()) {
                if (TechnicalClass.ofCode(reference.slug()).isPresent()) {
                    assertTrue(codes.contains(reference.slug()), index.slug() + " names " + reference.slug());
                }
            }
        }
    }

    @Test
    @DisplayName("a part stands so its pins are where Eagle connects them, turned and mirrored as Eagle turns it")
    void partsStandWhereEagleConnectsThem() {
        String circuit = text(imported().root());
        // R1 at (10.16, 20.32) turned R90: our sheet's left is 10.16 and its top 27.94, so it stands at 0, 120, turned
        // back the other way round on a sheet whose Y goes down — its pin 1 then at 0, 200, Eagle's (10.16, 15.24).
        assertTrue(circuit.contains("place:R1(at: 0 120, rotate: 270)"), circuit);
        assertTrue(circuit.contains("place:R2(at: 160 40, mirror)"), circuit);
        assertTrue(circuit.contains("wire:W1(R1.pin1, to: ground"), circuit);
        assertTrue(circuit.contains("R1.pin2, to: R2.pin2"), circuit);
    }

    @Test
    @DisplayName("a supply is a mark, a label a label mark, and a part of several gates our box")
    void suppliesLabelsAndBoxes() {
        ImportedDocuments imported = imported();
        String circuit = text(imported.root());
        assertTrue(circuit.contains("part:R1(eagle-test-resistor-0805, [pin1: 0, pin2: OUT], value: \"10k\")"), circuit);
        assertTrue(circuit.contains("part:R2(eagle-test-resistor-0805, [pin1: IN, pin2: OUT], value: \"10k\")"), circuit);
        // The ground's pin stands on the supply's point, 40 above the mark's origin, as ground-symbol draws it.
        assertTrue(circuit.contains("(ground, at: 0 320)"), circuit);
        assertTrue(circuit.contains("label, IN,"), circuit);
        assertTrue(circuit.contains("part:U1(eagle-test-dual, [A_IN_plus: OUT])"), circuit);
        String dual = text(imported.library().getLast());
        assertTrue(dual.contains("symbol(box)"), dual);
        assertTrue(dual.contains("A_IN_minus"), dual);
        assertEquals(1, imported.notes().size(), imported.notes().toString());
    }

    @Test
    @DisplayName("a pad Eagle names otherwise than by a number is numbered after the package's own numbers")
    void padsAreNumbered() {
        EagleLibrary library = EagleReader.read("divider.sch", SCHEMATIC).schematic().orElseThrow().library("test").orElseThrow();
        assertEquals(Map.of("P$1", "4", "P$2", "5", "3", "3"), EagleNames.pads(library.packages().get("SOT23")));
    }

    @Test
    @DisplayName("a package's >NAME and >VALUE become its footprint's fields, their middle raised half their height from where Eagle begins them")
    void nameAndValueAreFields() {
        String footprint = language.write(imported().library().stream().filter(document -> document.slug().equals("eagle-test-0805-footprint"))
                .findFirst().orElseThrow());
        assertTrue(footprint.contains("field(reference, at: -1.27 -1.905, size: 1.27, layer: silkscreen, anchor: start)"), footprint);
        assertTrue(footprint.contains("field(value, at: -1.27 1.905, size: 1.27, layer: fabrication, anchor: start)"), footprint);
    }
}
