package org.jmouse.cad.store.exchange;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.document.DocumentNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What a board's groups and its 0 Ω links say comes back as it was written. */
class PcbGroupsAndLinksTest {

    private static final String BOARD = """
            pcb:with-groups("With groups") {
                place(R1, at: 1 2, group: G1)
                place(JP1, footprint: chip-resistor-0805-footprint, nets: [N = 1, N = 2], at: 3 4, role: link)
                track(top, width: 0.25, from: 0 0, to: 5 0, group: G1, finish: bare)
                via(at: 5 0, drill: 0.3, diameter: 0.6, group: G1)
            }
            """;

    @Test
    void theyAreReadAndWrittenBack() {
        DocumentNode read = PcbOutputLinesTest.LANGUAGE.readOne(BOARD, "with-groups");
        for (PcbDrawing pcb : new PcbDrawing[] {PcbDrawing.of(read), PcbDrawing.of(PcbOutputLinesTest.LANGUAGE.readOne(PcbOutputLinesTest.LANGUAGE.write(read), "with-groups"))}) {
            assertEquals("G1", pcb.placements().getFirst().group());
            assertEquals("", pcb.placements().getFirst().role());
            assertEquals("link", pcb.placements().get(1).role());
            assertEquals("G1", pcb.tracks().getFirst().group());
            assertEquals(true, pcb.tracks().getFirst().bare());
            assertEquals("G1", pcb.vias().getFirst().group());
        }
    }
}
