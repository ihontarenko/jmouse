package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.settings.SettingsSchemas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The gerber plugin's settings sections: each file read into its record, and its schema saying what the record reads. */
class GerberPluginSettingsTest {

    @Test
    void everySectionIsReadAndDescribed() {
        assertEquals(List.of(), SettingsSchemas.mismatches(List.of(GerberPlugin.ASSEMBLY, GerberPlugin.FABRICATION, GerberPlugin.FILES)));
    }
}
