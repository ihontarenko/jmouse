package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.exchange.imported.ImportedBuses;
import org.jmouse.cad.exchange.imported.ImportedCircuit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportedBusesTest {

    private static final List<double[]> LINE = List.of(new double[] {0, 0}, new double[] {10, 0});

    @Test
    void aRangeNamesTheBusAndItsNets() {
        ImportedCircuit.Bus bus = ImportedBuses.bus("B1", "D[0..2]", LINE, UnaryOperator.identity());

        assertEquals("D", bus.name());
        assertEquals(List.of("D0", "D1", "D2"), bus.members());
    }

    @Test
    void aGroupAndAnEagleNameAreRead() {
        assertEquals(List.of("SDA", "SCL"), ImportedBuses.bus("B1", "{SDA SCL}", LINE, UnaryOperator.identity()).members());
        ImportedCircuit.Bus eagle = ImportedBuses.bus("B2", "B$1:D[1..0],CLK", LINE, UnaryOperator.identity());
        assertEquals("D", eagle.name());
        assertEquals(List.of("D1", "D0", "CLK"), eagle.members());
    }

    @Test
    void aWordAloneOrNothingNamesABusOfNoNets() {
        assertTrue(ImportedBuses.bus("B1", "DATA", LINE, UnaryOperator.identity()).members().isEmpty());
        assertEquals("DATA", ImportedBuses.bus("B1", "DATA", LINE, UnaryOperator.identity()).name());
        assertEquals("BUS", ImportedBuses.bus("B1", null, LINE, UnaryOperator.identity()).name());
    }

    @Test
    void stretchesMeetingEndToEndAreOneLine() {
        List<List<double[]>> lines = ImportedBuses.joined(List.of(
                List.of(new double[] {0, 0}, new double[] {10, 0}),
                List.of(new double[] {10, 10}, new double[] {10, 0}),
                List.of(new double[] {50, 50}, new double[] {60, 50})));

        assertEquals(2, lines.size());
        assertEquals(3, lines.getFirst().size());
    }

    @Test
    void theNetsAreNamedAsTheImportNamesThem() {
        assertEquals(List.of("n_A0"), ImportedBuses.bus("B1", "A[0..0]", LINE, member -> "n_" + member).members());
    }
}
