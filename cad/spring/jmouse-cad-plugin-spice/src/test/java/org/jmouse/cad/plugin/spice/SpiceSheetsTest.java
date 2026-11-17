package org.jmouse.cad.plugin.spice;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.library.CadLibrary;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.spice.SpiceConverter;
import org.jmouse.cad.spice.SpiceDevices;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A deck of a design made of sheets: SPICE has none, so a block is written as the sheet's own parts, named after it, and a bus it
 * is bound by joins member by member — the parent's {@code DATA_0} is the sheet's {@code D_0}.
 */
class SpiceSheetsTest {

    private static final String MEMORY = """
            circuit:spice-bus-memory("Bus memory") {
                ports {
                    bus(D)
                }
                buses {
                    bus(D[0..1])
                }
                part:R1(resistor, [terminal_one: D_0, terminal_two: D_1], resistance: 1000)
            }
            """;

    private static final String BOARD = """
            circuit:spice-bus-board("Bus board") {
                buses {
                    bus(DATA[0..1])
                }
                part:S1(spice-bus-memory, [D: DATA])
                part:V1(voltage-source, [positive: DATA_0, negative: 0], voltage: 5)
                part:R9(resistor, [terminal_one: DATA_1, terminal_two: 0], resistance: 1000)
            }
            """;

    @Test
    void aBlockIsItsSheetsPartsAndItsBusJoinsMemberByMember() {
        DocumentNode memory = CadLanguage.shipped().language().readOne(MEMORY, "spice-bus-memory");
        DocumentNode board = CadLanguage.shipped().language().readOne(BOARD, "spice-bus-board");
        CatalogueDocuments documents = slug -> slug.equals("spice-bus-memory") ? Optional.of(memory) : CadLibrary.library().find(slug);
        String deck = new SpiceConverter(documents, CadLanguage.standard().language(), SpiceDevices.shipped()).exportDocument(board).text();
        List<String> resistor = deck.lines().filter(line -> line.startsWith("RS1_R1")).toList();
        assertEquals(1, resistor.size(), deck);
        assertTrue(resistor.getFirst().matches("(?i)RS1_R1 DATA_0 DATA_1 .*"), deck);
    }
}
