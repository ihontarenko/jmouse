package org.jmouse.cad.plugin.ipc2581;

import org.jmouse.cad.settings.SettingsSchemas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The ipc2581 plugin's settings sections: each file read into its record, and its schema saying what the record reads. */
class Ipc2581PluginSettingsTest {

    @Test
    void everySectionIsReadAndDescribed() {
        assertEquals(List.of(), SettingsSchemas.mismatches(List.of(Ipc2581Plugin.EXPORT)));
    }
}
