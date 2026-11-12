package org.jmouse.libre.pcb;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A component's value as a reader is told it, its places filled in. */
class LibrePcbPartValueTest {

    private static LibrePcbCircuit.Part part(String value, Map<String, String> attributes) {
        return new LibrePcbCircuit.Part("id", "component", "variant", "R1", value, attributes, List.of(), Map.of());
    }

    @Test
    void aPlaceIsFilledFromAnAttribute() {
        assertEquals("100 nF", part("{{CAPACITANCE}}", Map.of("CAPACITANCE", "100 nF")).valueInWords(Map.of()));
        assertEquals("10 kΩ 1%", part("{{RESISTANCE}} {{TOLERANCE}}", Map.of("RESISTANCE", "10 kΩ", "TOLERANCE", "1%")).valueInWords(Map.of()));
    }

    @Test
    void theFirstNameThatHasWordsIsTaken() {
        assertEquals("STM32", part("{{MPN or DEVICE}}", Map.of("MPN", "STM32")).valueInWords(Map.of("DEVICE", "LQFP")));
        assertEquals("LQFP", part("{{MPN or DEVICE or COMPONENT}}", Map.of("MPN", " ")).valueInWords(Map.of("DEVICE", "LQFP")));
    }

    @Test
    void wordsOfItsOwnStandAndAPlaceNothingFillsIsLeftOut() {
        assertEquals("Made With LibrePCB", part("Made With\nLibrePCB", Map.of()).valueInWords(Map.of()));
        assertEquals("", part("{{UNKNOWN}}", Map.of()).valueInWords(Map.of()));
        assertEquals("$1 \\ ok", part("{{A}}", Map.of("A", "$1 \\ ok")).valueInWords(Map.of()), "words are not read as a pattern");
    }

    @Test
    void aUnitIsKnownByItsWord() {
        assertEquals("µF", LibrePcbUnit.ofWord("microfarad").orElseThrow().symbol());
        assertEquals("", LibrePcbUnit.ofWord("none").orElseThrow().symbol());
        assertTrue(LibrePcbUnit.ofWord("parsec").isEmpty());
    }
}
