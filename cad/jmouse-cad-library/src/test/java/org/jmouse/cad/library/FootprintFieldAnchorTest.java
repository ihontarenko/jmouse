package org.jmouse.cad.library;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.pcb.PcbModule;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A footprint's field says which part of its letters stands on its point, and the language keeps it through a save. */
class FootprintFieldAnchorTest {

    private final DocumentLanguage language = CadLanguage.shipped().language();

    @Test
    void aFieldsAnchorIsReadAndWrittenAndTheMiddleGoesUnsaid() {
        DocumentNode footprint = language.readOne("""
                footprint:anchored("Anchored") {
                    field(reference, at: -1 -2, size: 1, anchor: start)
                    field(value, at: 0 2, size: 1, layer: fabrication, anchor: middle)
                }
                """, "anchored");
        FootprintDrawing drawing = FootprintDrawing.of(footprint);
        assertEquals(List.of(PcbVocabulary.TEXT_START, PcbVocabulary.TEXT_MIDDLE),
                drawing.fields().stream().map(FootprintDrawing.Field::anchor).toList());
        // Written again from the drawing, as a save writes it: the start said, the middle — the usual — left unsaid.
        DocumentNode again = new DocumentNode(PcbModule.FOOTPRINT, "Anchored", "anchored", null, 1);
        drawing.lines().forEach(again::addExpression);
        String written = language.write(again);
        assertTrue(written.contains("field(reference, at: -1 -2, size: 1, anchor: start)"), written);
        assertEquals(1, written.lines().filter(line -> line.contains("anchor:")).count(), written);
    }
}
