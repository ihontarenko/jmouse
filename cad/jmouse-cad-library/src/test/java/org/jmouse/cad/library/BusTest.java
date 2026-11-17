package org.jmouse.cad.library;

import org.jmouse.cad.Index;
import org.jmouse.cad.circuit.Bus;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Connection;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.circuit.Placement;
import org.jmouse.cad.circuit.SheetBoundary;
import org.jmouse.cad.document.DocumentEditing;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.CircuitLines;
import org.jmouse.cad.simulation.CircuitSheet;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.cad.simulation.elaboration.SheetUnfolding;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A bus is an entity of the circuit: declared once — a vector, its nets the language's indexed names, or a group of the nets it
 * names —, drawn by lines that name it, left by entries, and handed across a sheet's edge whole, bound member by member.
 */
class BusTest {

    /** A sheet handing the bus D across: a resistor between its two nets, an entry where a wire leaves its line. */
    private static final String MEMORY = """
            circuit:bus-memory("Bus memory") {
                ports {
                    bus(D)
                }
                buses {
                    bus(D[0..1])
                }
                part:R1(resistor, [terminal_one: D_0, terminal_two: D_1], resistance: 1000)
                bus:B1(D, via: [100 100, 300 100])
                mark:E1(entry, D_0, at: 120 100, rotate: 45)
            }
            """;

    /** A sheet placing it, its own bus DATA bound to the block's bus pin; a group bus beside it. */
    private static final String BOARD = """
            circuit:bus-board("Bus board") {
                buses {
                    bus(DATA[0..1])
                    bus(I2C, members: [SDA, SCL])
                }
                part:S1(bus-memory, [D: DATA])
                part:R9(resistor, [terminal_one: DATA_0, terminal_two: 0], resistance: 1000)
            }
            """;

    private static DocumentNode document(String text, String slug) {
        return CadLanguage.shipped().language().readOne(text, slug);
    }

    private static Circuit circuit(String text, String slug) {
        return (Circuit) CadLanguage.shipped().modelOf(document(text, slug)).orElseThrow();
    }

    private static CatalogueDocuments holding(String text, String slug) {
        DocumentNode held = document(text, slug);
        return wanted -> wanted.equals(slug) ? Optional.of(held) : Optional.empty();
    }

    @Test
    void aVectorCarriesTheIndexedNamesAndAGroupTheNetsItNames() {
        Circuit board = circuit(BOARD, "bus-board");
        assertEquals(List.of(Bus.vector("DATA", new Index(0, 1)), Bus.group("I2C", List.of("SDA", "SCL"))), board.buses());
        assertEquals(List.of("DATA_0", "DATA_1"), board.bus("DATA").orElseThrow().members());
    }

    @Test
    void aLineNamesItsBusAnEntryIsAMarkOfAMemberAndTheSheetHandsTheBusAcross() {
        Circuit memory = circuit(MEMORY, "bus-memory");
        assertEquals(List.of(new SheetBoundary.Port("D", SimulationVocabulary.BUS)), memory.boundary().ports());
        Placement.BusLine line = memory.layout().stream().filter(Placement.BusLine.class::isInstance).map(Placement.BusLine.class::cast)
                .findFirst().orElseThrow();
        assertEquals("D", line.bus());
        Placement.Mark entry = memory.layout().stream().filter(Placement.Mark.class::isInstance).map(Placement.Mark.class::cast)
                .findFirst().orElseThrow();
        assertEquals(SimulationVocabulary.ENTRY, entry.symbol());
        assertEquals("D_0", entry.net());
    }

    @Test
    void aSheetWrittenAnewKeepsItsBusesTheVectorStillAVector() {
        Circuit before = circuit(BOARD, "bus-board");
        List<Bus> buses = List.of(Bus.vector("DATA", new Index(0, 3)), Bus.group("I2C", List.of("SDA", "SCL")));
        CircuitSheet drawn = new CircuitSheet(before.parts(), List.of(new Placement.BusLine("B1", "DATA",
                List.of(new org.jmouse.cad.Point(0, 0), new org.jmouse.cad.Point(200, 0)), null)), null, List.of(), null, List.of(), buses);
        String written = new DocumentEditing(CadLanguage.shipped().language(), CadLanguage.shipped().modules())
                .redraw(document(BOARD, "bus-board"), drawn.redrawing(CircuitSheet.of(before), new CircuitLines(CadLanguage.shipped().language(), "bus-board")),
                        new DocumentEditing.Head("Bus board", null, null));
        assertTrue(written.contains("bus(DATA[0..3])"), written);
        assertTrue(written.contains("bus(I2C, members: [SDA, SCL])"), written);
        Circuit after = circuit(written, "bus-board");
        assertEquals(buses, after.buses());
        assertEquals("DATA", ((Placement.BusLine) after.layout().getFirst()).bus());
    }

    @Test
    void aBlocksBusPinBindsTheSheetsBusToTheParentsMemberByMember() {
        Circuit unfolded = SheetUnfolding.of(circuit(BOARD, "bus-board"), holding(MEMORY, "bus-memory"));
        Part resistor = unfolded.part("S1_R1").orElseThrow();
        assertEquals(List.of("DATA_0", "DATA_1"), resistor.connections().stream().map(Connection::net).toList());
        Map<String, String> wired = SheetUnfolding.portsOf(circuit(BOARD, "bus-board").part("S1").orElseThrow(), circuit(MEMORY, "bus-memory"));
        assertEquals(Map.of("D", "DATA"), wired, "a block is drawn with its bus pin whole");
    }

    @Test
    void aBusPinBoundToNoBusOrToABusOfAnotherSizeIsRefusedNamingBoth() {
        String toANet = BOARD.replace("[D: DATA]", "[D: DATA_0]");
        IllegalArgumentException net = assertThrows(IllegalArgumentException.class,
                () -> SheetUnfolding.of(circuit(toANet, "bus-board"), holding(MEMORY, "bus-memory")));
        assertTrue(net.getMessage().contains("DATA_0") && net.getMessage().contains("no bus"), net.getMessage());
        String wider = BOARD.replace("bus(DATA[0..1])", "bus(DATA[0..7])");
        IllegalArgumentException size = assertThrows(IllegalArgumentException.class,
                () -> SheetUnfolding.of(circuit(wider, "bus-board"), holding(MEMORY, "bus-memory")));
        assertTrue(size.getMessage().contains("2 nets") && size.getMessage().contains("8"), size.getMessage());
    }

    @Test
    void aVectorThatNamesMembersTooAndABusOfNoNetAreRefused() {
        String both = BOARD.replace("bus(DATA[0..1])", "bus(DATA[0..1], members: [X])");
        assertThrows(RuntimeException.class, () -> circuit(both, "bus-board"));
        assertThrows(IllegalArgumentException.class, () -> Bus.group("EMPTY", List.of()));
        assertThrows(IllegalArgumentException.class, () -> Bus.group("TWICE", List.of("A", "A")));
        assertEquals("D_0, D_1", Bus.vector("D", new Index(0, 1)).members().stream().collect(Collectors.joining(", ")));
    }
}
