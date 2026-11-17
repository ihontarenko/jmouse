package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.document.DocumentParser;
import org.jmouse.cad.document.TechnicalClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** What another product brings is named by a code in the namespace of the library it came from. */
class ImportedNamesTest {

    @Test
    void aCodeIsTheLibrarysNamespaceAndTheNameInCapitals() {
        assertEquals("RCL:R0402:F", ImportedNames.code(TechnicalClass.FOOTPRINT, "rcl", "R0402"));
        assertEquals("CONNECTOR-GENERIC:BAT-1.25-MM:M3D", ImportedNames.code(TechnicalClass.MODEL, "Connector_Generic", "Bat 1.25 mm"));
        assertEquals("CONNECTORS:S2B-PH-K-S-LF-SN:F", ImportedNames.code(TechnicalClass.FOOTPRINT, "connectors", "S2B-PH-K-S(LF)(SN)"));
    }

    @Test
    void aNamespaceWritesNoDotForTheDotEndsTheClass() {
        assertEquals("MY-LIB:OPA2134:S", ImportedNames.code(TechnicalClass.SYMBOL, "my.lib", "OPA2134"));
    }

    @Test
    void aNameWithNothingACodeWritesIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> ImportedNames.code(TechnicalClass.FOOTPRINT, "rcl", "()"));
    }

    @Test
    void aSlugLongerThanAnAddressMayBeKeepsItsStartItsKindAndAHashOfTheWhole() {
        String library = "Connector_Dsub";
        String name = "DSUB-9_Female_Horizontal_P2.77x2.84mm_EdgePinOffset7.70mm_Housed_MountingHolesOffset9.12mm";
        String slug = ImportedNames.slug("kicad", "kit-dev-coldfire", name, "footprint");
        assertTrue(slug.length() <= DocumentParser.LONGEST_SLUG, slug);
        assertTrue(slug.startsWith("kicad-kit-dev-coldfire-dsub-9-female") && slug.endsWith("-footprint"), slug);
        assertEquals(slug, ImportedNames.slug("kicad", "kit-dev-coldfire", name, "footprint"));
        assertNotEquals(slug, ImportedNames.slug("kicad", "kit-dev-coldfire", name + "_B", "footprint"));
        assertEquals("kicad-device-r-symbol", ImportedNames.slug("kicad", "Device:R", "symbol"));
    }
}
