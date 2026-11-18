package org.jmouse.cad.pcb.drawing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.drawing.DrawnGroup;
import org.jmouse.cad.drawing.DrawnState;
import org.jmouse.el.grammar.ClauseLineNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every line a pcb writes is one its editor draws: a line of no class refused the whole layout of a board whose parts were grouped —
 * the CM5 demo imported from KiCad, «a canvas drew a statement of no class it draws», 2026-11-16.
 */
class PcbDrawingClassesTest {

    @Test
    void everyLineAPcbWritesHasAClassItsGroupsTooAsTheGroupsLine() {
        List<Track> tracks = List.of(new Track(PcbVocabulary.TOP, 0.25, 0, List.of(new Point(0, 0), new Point(5, 0))));
        PcbDrawing pcb = new PcbDrawing("circuit", Optional.empty(), List.of(), null, List.of(), List.of(), 2, null, List.of(), tracks, List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new DrawnGroup("Power", DrawnState.LOCKED), new DrawnGroup("Analogue", DrawnState.FREE)));
        List<ClauseLineNode> lines = pcb.lines();
        assertEquals(List.of(), lines.stream().filter(line -> PcbDocumentReading.classOf(line).isEmpty()).map(ClauseLineNode::toSource).toList());
        assertEquals(2, lines.stream().filter(line -> PcbDocumentReading.classOf(line).equals(Optional.of("group"))).count());
    }
}
