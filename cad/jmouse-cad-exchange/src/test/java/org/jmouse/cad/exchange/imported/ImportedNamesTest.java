package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.document.TechnicalClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** What another product brings is named by a code in the namespace of the library it came from. */
class ImportedNamesTest {

    @Test
    void aCodeIsTheLibrarysNamespaceAndTheNameInCapitals() {
        assertEquals("FP.RCL:R0402", ImportedNames.code(TechnicalClass.FOOTPRINT, "rcl", "R0402"));
        assertEquals("MD.CONNECTOR-GENERIC:BAT-1.25-MM", ImportedNames.code(TechnicalClass.MODEL, "Connector_Generic", "Bat 1.25 mm"));
        assertEquals("FP.CONNECTORS:S2B-PH-K-S-LF-SN", ImportedNames.code(TechnicalClass.FOOTPRINT, "connectors", "S2B-PH-K-S(LF)(SN)"));
    }

    @Test
    void aNamespaceWritesNoDotForTheDotEndsTheClass() {
        assertEquals("SY.MY-LIB:OPA2134", ImportedNames.code(TechnicalClass.SYMBOL, "my.lib", "OPA2134"));
    }

    @Test
    void aNameWithNothingACodeWritesIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> ImportedNames.code(TechnicalClass.FOOTPRINT, "rcl", "()"));
    }
}
