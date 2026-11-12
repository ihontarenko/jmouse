package org.jmouse.altium;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An Altium PCB library read as its streams say it, checked against a real one: {@code rcl.PcbLib} of Maxime Clement's
 * MIT-licensed libraries (beside the test, with its licence). The figures are the library's own, read in Altium.
 */
class AltiumPcbLibraryReaderTest {

    private static AltiumPcbLibrary sample() throws IOException {
        try (InputStream input = AltiumPcbLibraryReaderTest.class.getResourceAsStream("pcblib/rcl.PcbLib")) {
            return AltiumPcbLibraryReader.read("rcl.PcbLib", input.readAllBytes());
        }
    }

    private static AltiumPcbLibrary.Footprint footprint(AltiumPcbLibrary library, String name) {
        return library.footprints().stream().filter(found -> found.name().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void everyFootprintIsReadByItsWholeName() throws IOException {
        List<String> names = sample().footprints().stream().map(AltiumPcbLibrary.Footprint::name).toList();
        assertEquals(List.of("C0402", "C0603", "C0805", "C1206", "L0402", "L0603", "R0402", "R0603", "R0805",
                "C0402_DNI", "C0603_DNI", "R0402_DNI", "R0603_DNI"), names);
    }

    @Test
    void aPadStandsWhereAltiumPutsItAsLargeAsItIs() throws IOException {
        AltiumPcbLibrary.Footprint resistor = footprint(sample(), "R0402");
        assertEquals(2, resistor.pads().size());
        AltiumPcbLibrary.Pad first = resistor.pads().getFirst();
        assertEquals("1", first.name());
        assertEquals(1, first.layer());
        // 20 mil left of the middle, 20 × 25 mil: an 0402's land.
        assertEquals(-0.508, AltiumPcbLibrary.millimetres(first.at().x()), 1e-9);
        assertEquals(0.508, AltiumPcbLibrary.millimetres(first.width()), 1e-9);
        assertEquals(0.635, AltiumPcbLibrary.millimetres(first.height()), 1e-9);
        assertEquals(AltiumPcbLibrary.PadShape.RECTANGLE, first.shape());
        assertEquals(0, first.hole());
        assertEquals(AltiumPcbLibrary.Expansion.MANUAL, first.maskExpansion());
        assertEquals(0.0508, AltiumPcbLibrary.millimetres(first.maskMargin()), 1e-9);
    }

    @Test
    void itsLinesAreOnTheLayersAltiumDrawsThem() throws IOException {
        AltiumPcbLibrary.Footprint resistor = footprint(sample(), "R0402");
        assertEquals(8, resistor.tracks().size());
        assertEquals(4, resistor.tracks().stream().filter(track -> track.layer() == 33).count());
        assertEquals(Integer.valueOf(1), sample().mechanicalKind().get(71));
        assertEquals(Integer.valueOf(26), sample().mechanicalKind().get(69));
    }

    @Test
    void aTextIsReadWhereAndAsHighAsAltiumWritesIt() throws IOException {
        AltiumPcbLibrary.Footprint resistor = footprint(sample(), "R0402");
        assertEquals(1, resistor.texts().size());
        AltiumPcbLibrary.Text designator = resistor.texts().getFirst();
        assertEquals(AltiumPcbLibrary.Text.DESIGNATOR, designator.text());
        assertEquals(71, designator.layer());
        // 25 mil high, a 5 mil stroke, in Altium's ten-thousandths of a mil.
        assertEquals(250000, designator.height());
        assertEquals(50000, designator.stroke());
        assertEquals(-295182, designator.at().x());
        assertEquals(-120386, designator.at().y());
    }

    @Test
    void theModelsItEmbedsAreTheirStepFiles() throws IOException {
        AltiumPcbLibrary library = sample();
        assertEquals(8, library.models().size());
        for (AltiumPcbLibrary.Model model : library.models()) {
            assertTrue(new String(model.content(), 0, 13, StandardCharsets.US_ASCII).equals("ISO-10303-21;"), model.fileName());
        }
        AltiumPcbLibrary.Body body = footprint(library, "R0402").bodies().getFirst();
        assertTrue(library.model(body.modelIdentifier().orElseThrow()).isPresent());
    }

    @Test
    void aFileThatIsNoLibraryIsRefused() {
        assertThrows(AltiumReadException.class, () -> AltiumPcbLibraryReader.read("plain.txt", "not a library".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void aLengthIsReadInWhicheverUnitItSays() {
        assertEquals(196_850, AltiumMeasures.units("19.685mil"), 1e-6);
        assertEquals(AltiumPcbLibrary.UNITS_PER_MILLIMETRE * 0.5, AltiumMeasures.units("0.5mm"), 1e-6);
        assertEquals(270, AltiumMeasures.degrees(" 2.70000000000000E+0002"), 1e-9);
    }
}
