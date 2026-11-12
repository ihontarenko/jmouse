package org.jmouse.cad.plugin.odb;

import org.jmouse.cad.settings.SettingsSchemas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The odb plugin's settings sections: each file read into its record, and its schema saying what the record reads. */
class OdbPluginSettingsTest {

    @Test
    void everySectionIsReadAndDescribed() {
        assertEquals(List.of(), SettingsSchemas.mismatches(List.of(OdbPlugin.EXPORT)));
    }
}
