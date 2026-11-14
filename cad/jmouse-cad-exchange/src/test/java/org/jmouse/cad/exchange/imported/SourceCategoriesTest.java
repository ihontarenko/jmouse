package org.jmouse.cad.exchange.imported;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A part another product brings is filed by what the product calls it, the most specific word first, the table's order deciding. */
class SourceCategoriesTest {

    private static final SourceCategories KICAD = new SourceCategories(rules(), "general");

    private static Map<String, String> rules() {
        Map<String, String> rules = new LinkedHashMap<>();
        rules.put("Resistor_SMD", "resistors-surface");
        rules.put("Device:R", "resistors");
        rules.put("Resistor_*", "resistors");
        rules.put("Device", "passive");
        return rules;
    }

    @Test
    void theMostSpecificWordFilesItFirst() {
        // A library's pattern never takes a part's LIBRARY:NAME: the SMD resistor is filed by its library, not by «Resistor_*».
        assertEquals("resistors-surface", KICAD.categoryOf(List.of("Resistor_SMD:R_0805_2012Metric", "Resistor_SMD")));
        assertEquals("resistors", KICAD.categoryOf(List.of("Device:R", "Device")));
        assertEquals("passive", KICAD.categoryOf(List.of("Device:C", "Device")));
        assertEquals("resistors", KICAD.categoryOf(List.of("Resistor_THT:R_Axial", "Resistor_THT")));
    }

    @Test
    void whatNothingNamesIsUnfiledAndCaseDoesNotDecide() {
        assertEquals("general", KICAD.categoryOf(List.of("Sensor_Gas:MQ-2", "Sensor_Gas")));
        assertEquals("general", KICAD.categoryOf(List.of()));
        assertEquals("resistors-surface", KICAD.categoryOf(List.of("resistor_smd")));
    }
}
