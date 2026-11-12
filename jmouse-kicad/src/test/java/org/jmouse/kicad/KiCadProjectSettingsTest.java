package org.jmouse.kicad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

class KiCadProjectSettingsTest {

    private static final String PROJECT = """
            {"board": {"design_settings": {
                "rules": {"min_track_width": 0.15, "min_hole_clearance": 0.25, "use_height_for_length_calcs": true},
                "track_widths": [0, 0.2, 0.4],
                "via_dimensions": [{"diameter": 0, "drill": 0}, {"diameter": 0.8, "drill": 0.4}]}},
             "net_settings": {
                "classes": [
                    {"name": "Default", "track_width": 0.25, "clearance": 0.2, "via_diameter": 0.8, "via_drill": 0.4},
                    {"name": "Power", "track_width": 1.0, "clearance": 0.3, "via_diameter": 1.2, "via_drill": 0.6}],
                "netclass_assignments": {"/MAINS": ["Power"], "/OLD": "Power"},
                "netclass_patterns": [{"netclass": "Power", "pattern": "+*V"}]}}
            """;

    private final KiCadProjectSettings settings = KiCadProjectSettings.of(PROJECT);

    @Test
    void readsTheClassesAndKeepsTheUsualApart() {
        assertEquals(0.25, settings.usual().orElseThrow().trackWidth());
        assertEquals(1, settings.classes().size());
        assertEquals("Power", settings.classes().getFirst().name());
    }

    @Test
    void aNetIsInAClassByNameOrByPattern() {
        assertEquals(Optional.of("Power"), settings.classOf("/MAINS"));
        assertEquals(Optional.of("Power"), settings.classOf("/OLD"));
        assertEquals(Optional.of("Power"), settings.classOf("+12V"));
        assertEquals(Optional.empty(), settings.classOf("GND"));
    }

    @Test
    void readsTheRulesAndTheSizesItOffers() {
        assertEquals(0.15, settings.rules().get("min_track_width"));
        assertEquals(List.of(0.2, 0.4), settings.trackWidths());
        assertEquals(1, settings.viaSizes().size());
    }

    @Test
    void aFileThatSaysNothingIsNoFailure() {
        assertTrue(KiCadProjectSettings.of("not json").usual().isEmpty());
        assertTrue(KiCadProjectSettings.of("{}").classes().isEmpty());
    }
}
