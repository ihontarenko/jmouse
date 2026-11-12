package org.jmouse.cad.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a part does is its kind's, as the table says; a part that reads the circuit is an instrument whatever it is. */
class PartRoleTest {

    @Test
    void aKindIsWhatTheTableSaysAndAnyOtherDissipates() {
        assertEquals(PartRole.SOURCE, PartRole.of("generator", false));
        assertEquals(PartRole.SOURCE, PartRole.of("current-source", false));
        assertEquals(PartRole.STORING, PartRole.of("capacitor", false));
        assertEquals(PartRole.INSTRUMENT, PartRole.of("meter", false));
        assertEquals(PartRole.DISSIPATING, PartRole.of("resistor", false));
        assertEquals(PartRole.DISSIPATING, PartRole.of(null, false));
    }

    @Test
    void aPartThatReadsTheCircuitIsAnInstrument() {
        assertEquals(PartRole.INSTRUMENT, PartRole.of("resistor", true));
    }

    @Test
    void sourcesAndInstrumentsAreTheBenchs() {
        assertTrue(PartRole.SOURCE.ofTheBench());
        assertTrue(PartRole.INSTRUMENT.ofTheBench());
        assertFalse(PartRole.STORING.ofTheBench());
        assertFalse(PartRole.DISSIPATING.ofTheBench());
    }
}
