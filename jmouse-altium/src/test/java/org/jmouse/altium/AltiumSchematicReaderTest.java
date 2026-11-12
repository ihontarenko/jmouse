package org.jmouse.altium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("an Altium schematic read from its records")
class AltiumSchematicReaderTest {

    /** A resistor turned upright, its body, its two pins, a wire to a ground port and a labelled net, as ASCII records. */
    private static final String SHEET = String.join("\n",
            "|HEADER=Protel for Windows - Schematic Capture Ascii File Version 5.0|WEIGHT=9",
            "|RECORD=31|FONTIDCOUNT=1|SIZE1=10|FONTNAME1=Arial",
            "|RECORD=1|LIBREFERENCE=RES|COMPONENTDESCRIPTION=Resistor|PARTCOUNT=2|LOCATION.X=100|LOCATION.Y=200|ORIENTATION=1|CURRENTPARTID=1",
            "|RECORD=14|OWNERINDEX=1|OWNERPARTID=1|LOCATION.X=96|LOCATION.Y=190|CORNER.X=104|CORNER.Y=210|ISSOLID=T",
            "|RECORD=2|OWNERINDEX=1|OWNERPARTID=1|PINCONGLOMERATE=1|PINLENGTH=10|LOCATION.X=100|LOCATION.Y=210|NAME=1|DESIGNATOR=1|ELECTRICAL=4",
            "|RECORD=2|OWNERINDEX=1|OWNERPARTID=1|PINCONGLOMERATE=27|PINLENGTH=10|LOCATION.X=100|LOCATION.Y=190|NAME=2|DESIGNATOR=2|ELECTRICAL=4|SYMBOL_OUTEREDGE=1",
            "|RECORD=2|OWNERINDEX=1|OWNERPARTID=1|OWNERPARTDISPLAYMODE=1|PINCONGLOMERATE=1|PINLENGTH=10|LOCATION.X=100|LOCATION.Y=210|NAME=1|DESIGNATOR=1",
            "|RECORD=34|OWNERINDEX=1|LOCATION.X=110|LOCATION.Y=205|TEXT=R1|NAME=Designator",
            "|RECORD=41|OWNERINDEX=1|LOCATION.X=110|LOCATION.Y=195|TEXT==Value|NAME=Comment",
            "|RECORD=41|OWNERINDEX=1|LOCATION.X=110|LOCATION.Y=195|TEXT=10k|NAME=Value|ISHIDDEN=T",
            "|RECORD=44|OWNERINDEX=1",
            "|RECORD=45|OWNERINDEX=9|MODELNAME=R0603|MODELTYPE=PCBLIB|ISCURRENT=T",
            "|RECORD=27|LOCATIONCOUNT=2|X1=100|Y1=180|X2=100|Y2=160",
            "|RECORD=17|STYLE=4|ORIENTATION=3|LOCATION.X=100|LOCATION.Y=160|TEXT=GND",
            "|RECORD=25|LOCATION.X=100|LOCATION.Y=220|TEXT=IN",
            "|RECORD=15|LOCATION.X=300|LOCATION.Y=300",
            "|RECORD=26|LOCATIONCOUNT=3|X1=200|Y1=100|X2=200|Y2=150|X3=260|Y3=150",
            "");

    private static AltiumSchematic read() {
        return AltiumSchematicReader.read("divider.SchDoc", SHEET.getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    @DisplayName("a component gathers what it owns, of the drawing it shows")
    void aComponentGathersWhatItOwns() {
        AltiumSchematic.Component resistor = read().components().getFirst();
        assertEquals("R1", resistor.designator());
        assertEquals("10k", resistor.comment());
        assertEquals("R0603", resistor.footprint().orElseThrow());
        assertEquals(AltiumSchematic.Orientation.UP, resistor.orientation());
        assertEquals(2, resistor.pins().size());
        assertEquals(1, resistor.graphics().size());
        assertTrue(resistor.graphics().getFirst() instanceof AltiumSchematic.Graphic.Rectangle rectangle && rectangle.solid());
    }

    @Test
    @DisplayName("a pin's hot end is its length away from the body, the way it points")
    void aPinEndsAway() {
        List<AltiumSchematic.Pin> pins = read().components().getFirst().pins();
        assertEquals(new AltiumSchematic.Point(100, 220), pins.get(0).end());
        assertEquals(new AltiumSchematic.Point(100, 180), pins.get(1).end());
        assertTrue(pins.get(1).inverted());
        assertTrue(pins.get(1).nameShown() && pins.get(1).designatorShown());
    }

    @Test
    @DisplayName("wires, ports and labels stand where the sheet puts them; what is not read is counted")
    void theSheetsOwnRecords() {
        AltiumSchematic sheet = read();
        assertEquals(1, sheet.wires().size());
        assertEquals(List.of(new AltiumSchematic.Point(200, 100), new AltiumSchematic.Point(200, 150), new AltiumSchematic.Point(260, 150)),
                sheet.buses().getFirst().points());
        AltiumSchematic.PowerPort ground = sheet.powerPorts().getFirst();
        assertTrue(ground.style().ground());
        assertEquals(AltiumSchematic.Orientation.DOWN, ground.orientation());
        assertEquals("IN", sheet.netLabels().getFirst().text());
        assertEquals(List.of("1 × sheet symbol"), sheet.leftOut());
    }

    @Test
    @DisplayName("what is no Altium schematic is refused")
    void notOneIsRefused() {
        assertThrows(AltiumReadException.class,
                () -> AltiumSchematicReader.read("notes.txt", "just text".getBytes(StandardCharsets.US_ASCII)));
    }
}
