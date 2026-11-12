package org.jmouse.cad.eagle;

import org.jmouse.cad.library.CadLibrary;

import org.jmouse.cad.exchange.imported.SheetMarks;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.ImportedDocuments;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.eagle.EagleBoard;
import org.jmouse.eagle.EagleReader;
import org.jmouse.eagle.EagleSchematic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("an Eagle pcb read with its schematic")
class EaglePcbImportTest {

    /** The schematic's library cut out of it: a pcb carries the same. */
    private static final String LIBRARY = EagleSchematicImportTest.SCHEMATIC.substring(
            EagleSchematicImportTest.SCHEMATIC.indexOf("<libraries>"),
            EagleSchematicImportTest.SCHEMATIC.indexOf("</libraries>") + "</libraries>".length());

    /**
     * A 30 by 20 pcb, one corner rounded; R1 on the bottom turned a quarter, R2 on the top, a logo the circuit does
     * not hold; the net OUT a track on the bottom ending in a via the rules size, a ground pour on the bottom.
     */
    private static final String PCB = """
            <?xml version="1.0" encoding="utf-8"?>
            <eagle version="9.6.2"><drawing><board>
            <plain>
            <wire x1="0" y1="0" x2="30" y2="0" width="0" layer="20"/>
            <wire x1="0" y1="20" x2="0" y2="0" width="0" layer="20"/>
            <wire x1="30" y1="0" x2="30" y2="15" width="0" layer="20"/>
            <wire x1="25" y1="20" x2="0" y2="20" width="0" layer="20"/>
            <wire x1="30" y1="15" x2="25" y2="20" width="0" layer="20" curve="90"/>
            <wire x1="10" y1="10.95" x2="13" y2="10.95" width="0.5" layer="30"/>
            <polygon width="0" layer="29"><vertex x="2" y="2"/><vertex x="4" y="2"/><vertex x="4" y="4"/></polygon>
            <wire x1="1" y1="1" x2="2" y2="1" width="0.1" layer="99"/>
            </plain>
            """ + LIBRARY + """
            <elements>
            <element name="R1" library="test" package="0805" value="10k" x="10" y="10" rot="MR90"/>
            <element name="R2" library="test" package="0805" value="10k" x="20" y="10"/>
            <element name="LOGO1" library="test" package="0805" value="" x="25" y="5"/>
            </elements>
            <signals>
            <signal name="OUT">
            <contactref element="R1" pad="2"/>
            <wire x1="10" y1="10.95" x2="15" y2="10.95" width="0.254" layer="16"/>
            <via x="15" y="10.95" extent="1-16" drill="0.4"/>
            </signal>
            <signal name="GND">
            <polygon width="0.2" layer="16"><vertex x="1" y="1"/><vertex x="29" y="1"/><vertex x="29" y="5"/><vertex x="1" y="5"/></polygon>
            </signal>
            </signals>
            </board></drawing></eagle>
            """;

    private final DocumentLanguage language = CadLanguage.standard().language();

    private ImportedDocuments imported() {
        EagleSchematic schematic = EagleReader.read("divider.sch", EagleSchematicImportTest.SCHEMATIC).schematic().orElseThrow();
        EagleBoard pcb = EagleReader.read("divider.brd", PCB).board().orElseThrow();
        return EagleDesignImport.read(schematic, "divider.sch", Optional.of(new EagleDesignImport.Read(pcb, "divider.brd")),
                "Divider", EagleLibraries.standard(), SheetMarks.of(CadLibrary.library(), EagleLibraries.standard().markSymbols()));
    }

    @Test
    @DisplayName("the pcb is what the import stands for, after the circuit it names, and the language reads it back")
    void thePcbFollowsItsCircuit() {
        ImportedDocuments imported = imported();
        assertEquals("eagle-divider-pcb", imported.root().slug());
        assertEquals("eagle-divider", imported.designs().getFirst().slug());
        DocumentNode pcb = imported.root();
        assertDoesNotThrow(() -> language.readOne(language.write(pcb), pcb.slug()));
    }

    @Test
    @DisplayName("a window Eagle draws on tStop or bStop is a window in that side's mask, a stretch of bare track as much as an area; a layer we do not file stays out")
    void windowsInTheMask() {
        String written = language.write(imported().root());
        assertTrue(written.contains("shape(bottom-mask, width: 0.5"), written);
        assertTrue(written.contains("shape(top-mask, filled"), written);
        assertEquals(2, written.lines().filter(line -> line.strip().startsWith("shape(")).count(), written);
    }

    @Test
    @DisplayName("parts stand where Eagle places them, a bottom one turned the other way round; the outline is one closed loop")
    void partsAndOutline() {
        ImportedDocuments imported = imported();
        String pcb = language.write(imported.root());
        assertTrue(pcb.contains("circuit(eagle-divider)"), pcb);
        assertTrue(pcb.contains("place(R1, at: 10 10, rotate: 270, side: bottom)"), pcb);
        assertTrue(pcb.contains("place(R2, at: 20 10)"), pcb);
        String edge = pcb.substring(pcb.indexOf("edge(\""), pcb.indexOf("\")", pcb.indexOf("edge(\"")));
        assertEquals(1, edge.chars().filter(character -> character == 'M').count(), edge);
        assertTrue(edge.contains("A") && edge.endsWith("Z"), edge);
        assertTrue(imported.notes().stream().anyMatch(note -> note.startsWith("LOGO1")), imported.notes().toString());
    }

    @Test
    @DisplayName("copper is the signals': a track on its layer, a via the rules size, a pour of its net")
    void copper() {
        String pcb = language.write(imported().root());
        assertTrue(pcb.contains("track(bottom, width: 0.254, from: 10 9.05, to: 15 9.05)"), pcb);
        assertTrue(pcb.contains("via(at: 15 9.05, drill: 0.4, diameter: 0.908)"), pcb);
        assertTrue(pcb.contains("zone(bottom, net: 0"), pcb);
    }
}
