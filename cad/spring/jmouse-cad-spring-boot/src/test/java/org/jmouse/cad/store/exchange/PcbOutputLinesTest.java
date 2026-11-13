package org.jmouse.cad.store.exchange;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.OutputJob;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A pcb's own outputs stay with it through the language; a job with no format cannot exist. */
class PcbOutputLinesTest {

    static final DocumentLanguage LANGUAGE = CadLanguage.of(CadLanguage.engineModules()).language();

    private static final String BOARD = """
            pcb:with-outputs("With outputs") {
                output(factory, formats: [gerber, step], "For the factory")
                output(order, formats: jlcpcb, "The order")
            }
            """;

    @Test
    void outputsAreReadAndWrittenBack() {
        DocumentNode read = LANGUAGE.readOne(BOARD, "with-outputs");
        List<PcbDrawing.Output> expected = List.of(new PcbDrawing.Output("factory", List.of("gerber", "step"), "For the factory"),
                new PcbDrawing.Output("order", List.of("jlcpcb"), "The order"));
        assertEquals(expected, PcbDrawing.of(read).outputs());
        assertEquals(expected, PcbDrawing.of(LANGUAGE.readOne(LANGUAGE.write(read), "with-outputs")).outputs());
    }

    @Test
    void anAbsentListIsAnEmptyOne() {
        PcbDrawing pcb = new PcbDrawing("", Optional.empty(), null, null, null, null, 2, "", null, null, null, null, null, null,
                null, null, null, null, null, List.of());
        assertEquals(List.of(), pcb.placements());
        assertEquals(List.of(), pcb.shapes());
        assertEquals(List.of(), pcb.outputs());
    }

    @Test
    void aJobNamesAtLeastOneFormat() {
        assertThrows(IllegalArgumentException.class, () -> new OutputJob("empty", "Nothing", List.of()));
    }
}
