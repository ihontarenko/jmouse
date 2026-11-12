package org.jmouse.eagle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EagleReaderTest {

    private static final String LIBRARY = """
            <libraries>
            <library name="passives">
            <packages>
            <package name="0805">
            <description>Chip &lt;b&gt;0805&lt;/b&gt;</description>
            <smd name="1" x="-0.95" y="0" dx="1.1" dy="1.4" layer="1" roundness="25"/>
            <smd name="2" x="0.95" y="0" dx="1.1" dy="1.4" layer="1" rot="R180"/>
            <wire x1="-0.4" y1="0.6" x2="0.4" y2="0.6" width="0.1524" layer="21"/>
            <text x="0" y="1" size="0.6" layer="25">&gt;NAME</text>
            </package>
            </packages>
            <symbols>
            <symbol name="R">
            <wire x1="-2.54" y1="0" x2="2.54" y2="0" width="0.254" layer="94" curve="-90"/>
            <pin name="1" x="-5.08" y="0" visible="off" length="short" direction="pas"/>
            <pin name="2" x="5.08" y="0" visible="off" length="short" direction="pas" rot="R180"/>
            </symbol>
            </symbols>
            <devicesets>
            <deviceset name="RESISTOR" prefix="R" uservalue="yes">
            <gates><gate name="G$1" symbol="R" x="0" y="0"/></gates>
            <devices><device name="0805" package="0805">
            <connects><connect gate="G$1" pin="1" pad="1"/><connect gate="G$1" pin="2" pad="2"/></connects>
            <technologies><technology name=""><attribute name="PROD_ID" value="RES-00001"/></technology></technologies>
            </device></devices>
            </deviceset>
            </devicesets>
            </library>
            </libraries>
            """;

    @Test
    void aSchematicReadsItsLibrariesPartsAndNets() {
        EagleDesign design = EagleReader.read("divider.sch", """
                <?xml version="1.0" encoding="utf-8"?>
                <!DOCTYPE eagle SYSTEM "eagle.dtd">
                <eagle version="9.6.2"><drawing><schematic>
                """ + LIBRARY + """
                <parts>
                <part name="R1" library="passives" deviceset="RESISTOR" device="0805" value="10k"/>
                </parts>
                <sheets><sheet>
                <instances><instance part="R1" gate="G$1" x="10.16" y="20.32" rot="MR90"/></instances>
                <nets><net name="OUT" class="0"><segment>
                <pinref part="R1" gate="G$1" pin="2"/>
                <wire x1="10.16" y1="25.4" x2="15.24" y2="25.4" width="0.1524" layer="91"/>
                <junction x="15.24" y="25.4"/>
                <label x="15.24" y="25.4" size="1.778" layer="95"/>
                </segment></net></nets>
                </sheet></sheets>
                </schematic></drawing></eagle>
                """);
        EagleSchematic schematic = design.schematic().orElseThrow();
        assertEquals("9.6.2", design.version());
        EagleLibrary library = schematic.library("passives").orElseThrow();
        EagleLibrary.Package chip = library.packages().get("0805");
        assertEquals("Chip <b>0805</b>", chip.description());
        assertEquals(2, chip.smds().size());
        assertEquals(25, chip.smds().getFirst().roundness());
        assertEquals(180, chip.smds().get(1).rotation().angle());
        assertTrue(chip.graphics().get(1) instanceof EagleGraphic.Text text && text.text().equals(">NAME"));
        EagleLibrary.Symbol symbol = library.symbols().get("R");
        assertTrue(symbol.graphics().getFirst() instanceof EagleGraphic.Wire wire && wire.curve() == -90);
        assertEquals(EagleLibrary.Pin.Direction.PAS, symbol.pins().getFirst().direction());
        EagleLibrary.DeviceSet resistor = library.deviceSets().get("RESISTOR");
        assertEquals("R", resistor.prefix());
        assertTrue(resistor.userValue());
        EagleLibrary.Device device = resistor.device("0805").orElseThrow();
        assertEquals(List.of("2"), device.connects().get(1).pads());
        assertEquals("RES-00001", device.technologies().get("").get("PROD_ID"));
        assertEquals("10k", schematic.part("R1").orElseThrow().value());
        EagleSchematic.Instance instance = schematic.sheets().getFirst().instances().getFirst();
        assertEquals(new EagleRotation(90, true, false), instance.rotation());
        EagleSchematic.Segment segment = schematic.sheets().getFirst().nets().getFirst().segments().getFirst();
        assertEquals(new EagleSchematic.PinReference("R1", "G$1", "2"), segment.pins().getFirst());
        assertEquals(1, segment.wires().size());
        assertEquals(1, segment.junctions().size());
        assertEquals(1, segment.labels().size());
    }

    @Test
    void aBoardReadsItsOutlineElementsAndSignals() {
        EagleDesign design = EagleReader.read("divider.brd", """
                <?xml version="1.0" encoding="utf-8"?>
                <eagle version="9.6.2"><drawing><board>
                <plain><wire x1="0" y1="0" x2="20" y2="0" width="0" layer="20"/></plain>
                """ + LIBRARY + """
                <elements>
                <element name="R1" library="passives" package="0805" value="10k" x="5" y="5" rot="MR90"/>
                </elements>
                <signals><signal name="OUT">
                <contactref element="R1" pad="2"/>
                <wire x1="5" y1="5.95" x2="10" y2="5.95" width="0.254" layer="16"/>
                <via x="10" y="5.95" extent="1-16" drill="0.4"/>
                <polygon width="0.2" layer="1"><vertex x="0" y="0"/><vertex x="4" y="0" curve="90"/><vertex x="4" y="4"/></polygon>
                </signal></signals>
                </board></drawing></eagle>
                """);
        EagleBoard board = design.board().orElseThrow();
        assertTrue(design.schematic().isEmpty());
        assertEquals(EagleBoard.DIMENSION, board.plain().getFirst().layer());
        EagleBoard.Element element = board.elements().getFirst();
        assertTrue(element.rotation().mirrored());
        EagleBoard.Signal signal = board.signals().getFirst();
        assertEquals(new EagleBoard.ContactReference("R1", "2"), signal.contacts().getFirst());
        assertEquals(EagleBoard.BOTTOM, signal.wires().getFirst().layer());
        assertEquals(0, signal.vias().getFirst().diameter());
        assertEquals(90, signal.polygons().getFirst().vertices().get(1).curve());
    }

    @Test
    void aRotationReadsAsEagleWritesIt() {
        assertEquals(new EagleRotation(270, false, false), EagleRotation.of("R270"));
        assertEquals(new EagleRotation(45, true, true), EagleRotation.of("SMR45"));
        assertEquals(EagleRotation.NONE, EagleRotation.of(""));
        assertThrows(EagleReadException.class, () -> EagleRotation.of("90"));
    }

    @Test
    void anotherFileIsRefusedByName() {
        EagleReadException refused = assertThrows(EagleReadException.class,
                () -> EagleReader.read("board.kicad_pcb", "<kicad_pcb/>"));
        assertTrue(refused.getMessage().contains("board.kicad_pcb"));
        assertThrows(EagleReadException.class, () -> EagleReader.read("broken.sch", "(kicad_sch"));
    }
}
