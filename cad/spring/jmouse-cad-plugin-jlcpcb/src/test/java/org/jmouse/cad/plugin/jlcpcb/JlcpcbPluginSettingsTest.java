package org.jmouse.cad.plugin.jlcpcb;

import org.jmouse.cad.settings.SettingsSchemas;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The jlcpcb plugin's settings sections: each file read into its record, and its schema saying what the record reads. */
class JlcpcbPluginSettingsTest {

    @Test
    void everySectionIsReadAndDescribed() {
        assertEquals(List.of(), SettingsSchemas.mismatches(List.of(JlcpcbPlugin.ASSEMBLY)));
    }
}
