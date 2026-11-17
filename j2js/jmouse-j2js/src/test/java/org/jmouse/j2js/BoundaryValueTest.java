package org.jmouse.j2js;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundaryValueTest {

    @Test
    void aTypeJavaScriptHoldsTheSameCrosses() {
        assertEquals(Optional.of(BoundaryValue.NUMBER), BoundaryValue.ofJavaType("double"));
        assertEquals(Optional.of(BoundaryValue.TEXTS), BoundaryValue.ofJavaType("java.lang.String[]"));
    }

    @Test
    void aLongDoesNotCrossJavaScriptCannotHoldItWhole() {
        assertEquals(Optional.empty(), BoundaryValue.ofJavaType("long"));
        assertEquals(Optional.empty(), BoundaryValue.ofJavaType("java.util.List<java.lang.String>"));
    }

    @Test
    void nothingIsOnlyAResult() {
        assertTrue(BoundaryValue.NOTHING.crossesAt(BoundaryValue.Position.RESULT));
        assertFalse(BoundaryValue.NOTHING.crossesAt(BoundaryValue.Position.PARAMETER));
        assertTrue(BoundaryValue.TEXT.crossesAt(BoundaryValue.Position.PARAMETER));
    }
}
