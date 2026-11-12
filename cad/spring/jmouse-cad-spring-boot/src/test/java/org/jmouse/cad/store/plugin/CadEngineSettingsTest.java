package org.jmouse.cad.store.plugin;

import org.jmouse.cad.settings.SettingsSchemas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The core's settings sections: each file read into its record, and its schema saying what the record reads. */
class CadEngineSettingsTest {

    @Test
    void everySectionIsReadAndDescribed() {
        assertEquals(List.of(), SettingsSchemas.mismatches(CadEnginePlugin.CORE_SECTIONS));
    }
}
