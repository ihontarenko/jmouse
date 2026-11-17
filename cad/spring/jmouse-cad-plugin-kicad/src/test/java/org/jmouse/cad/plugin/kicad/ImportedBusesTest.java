package org.jmouse.cad.plugin.kicad;

import org.jmouse.cad.Index;
import org.jmouse.cad.circuit.Bus;
import org.jmouse.cad.exchange.imported.ImportedBuses;
import org.jmouse.cad.exchange.imported.ImportedCircuit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How another product's bus lines become the sheet's buses: each line naming its bus, each bus declared once by its name. */
class ImportedBusesTest {

    private static final List<double[]> LINE = List.of(new double[] {0, 0}, new double[] {10, 0});

    @Test
    void aRangeNamesTheBusAndItsNetsAndTheLineNamesTheBus() {
        ImportedBuses buses = new ImportedBuses();
        ImportedCircuit.BusLine line = buses.line("B1", "D[0..2]", LINE, UnaryOperator.identity());

        assertEquals("D", line.bus());
        assertEquals(List.of(Bus.group("D", List.of("D0", "D1", "D2"))), buses.buses());
    }

    @Test
    void netsNamedAsTheLanguageNamesABussBitsMakeAVector() {
        ImportedBuses buses = new ImportedBuses();
        buses.line("B1", "D[0..2]", LINE, member -> member.replaceFirst("^D", "D_"));

        assertEquals(List.of(Bus.vector("D", new Index(0, 2))), buses.buses());
    }

    @Test
    void aGroupAndAnEagleNameAreRead() {
        ImportedBuses buses = new ImportedBuses();
        buses.line("B1", "{SDA SCL}", LINE, UnaryOperator.identity());
        ImportedCircuit.BusLine eagle = buses.line("B2", "B$1:D[1..0],CLK", LINE, UnaryOperator.identity());

        assertEquals("D", eagle.bus());
        assertEquals(List.of("SDA", "SCL"), buses.buses().getFirst().members());
        assertEquals(List.of("D1", "D0", "CLK"), buses.buses().get(1).members());
    }

    @Test
    void twoLinesOfOneBusAreOneBusAndOneNameOverOtherNetsIsAnother() {
        ImportedBuses buses = new ImportedBuses();
        assertEquals("D", buses.line("B1", "D[0..1]", LINE, UnaryOperator.identity()).bus());
        assertEquals("D", buses.line("B2", "D[0..1]", LINE, UnaryOperator.identity()).bus());
        assertEquals("D_2", buses.line("B3", "D[0..3]", LINE, UnaryOperator.identity()).bus());

        assertEquals(List.of(Bus.group("D", List.of("D0", "D1")), Bus.group("D_2", List.of("D0", "D1", "D2", "D3"))), buses.buses());
    }

    @Test
    void aWordAloneOrNothingNamesABusAndDeclaresNone() {
        ImportedBuses buses = new ImportedBuses();
        assertEquals("DATA", buses.line("B1", "DATA", LINE, UnaryOperator.identity()).bus());
        assertEquals("BUS", buses.line("B2", null, LINE, UnaryOperator.identity()).bus());
        assertTrue(buses.buses().isEmpty());
    }

    @Test
    void aNameThatIsNoWordOfTheLanguageIsMadeOne() {
        assertEquals("IRQ", ImportedBuses.wordOf("IRQ-"));
        assertEquals("BUS_3V3", ImportedBuses.wordOf("3V3"));
        assertEquals("BUS", ImportedBuses.wordOf(""));
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
        ImportedBuses buses = new ImportedBuses();
        buses.line("B1", "A[0..0]", LINE, member -> "n_" + member);
        assertEquals(List.of("n_A0"), buses.buses().getFirst().members());
    }
}
