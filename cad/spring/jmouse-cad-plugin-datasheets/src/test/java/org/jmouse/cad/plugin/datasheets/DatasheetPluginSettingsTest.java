package org.jmouse.cad.plugin.datasheets;

import org.jmouse.cad.settings.SettingsSchemas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The datasheets plugin's settings sections: each file read into its record, and its schema saying what the record reads. */
class DatasheetPluginSettingsTest {

    @Test
    void everySectionIsReadAndDescribed() {
        assertEquals(List.of(), SettingsSchemas.mismatches(List.of(DatasheetPlugin.FORMAT)));
    }
}
