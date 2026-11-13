package org.jmouse.cad.library;

import org.jmouse.cad.component.BenchRole;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a component is to a bench is a field of its head, read by the index; a word no role has is refused. */
class BenchRoleTest {

    @Test
    void theShippedBenchDevicesSayWhatTheyAre() {
        Map<String, BenchRole> shipped = Map.of("mains-socket", BenchRole.SOURCE, "bench-supply", BenchRole.SOURCE,
                "transformer-secondary", BenchRole.SOURCE, "dummy-load", BenchRole.LOAD, "potentiometer", BenchRole.CONTROL,
                "ammeter", BenchRole.INSTRUMENT, "voltmeter", BenchRole.INSTRUMENT);
        shipped.forEach((slug, role) -> assertEquals(Optional.of(role),
                DocumentIndex.of(CadLibrary.library().find(slug).orElseThrow()).benchRole(), slug));
        assertTrue(DocumentIndex.of(CadLibrary.library().find("resistor").orElseThrow()).benchRole().isEmpty());
    }

    @Test
    void aWordNoRoleHasIsRefusedNamingTheRoles() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class, () -> DocumentIndex.of(CadLanguage.shipped().language()
                .readOne("component:odd(\"Odd\") {\n    meta {\n        bench: heater\n    }\n}\n", "odd")));
        assertEquals("A bench device is a source, a load, a control or an instrument, not 'heater'", refused.getMessage());
    }
}
