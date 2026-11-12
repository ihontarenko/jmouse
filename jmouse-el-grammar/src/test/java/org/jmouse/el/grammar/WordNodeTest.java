package org.jmouse.el.grammar;

import org.jmouse.el.writer.SourceLayout;
import org.jmouse.el.writer.TextSourceWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A word is written so it reads back as the same word: bare where it can be, in quotes where a bare word would end early. */
class WordNodeTest {

    private static String written(String word) {
        return TextSourceWriter.render(Nodes.word(word), SourceLayout.GLUED);
    }

    @Test
    void aWordAReaderReadsWholeIsWrittenBare() {
        assertEquals("through-hole", written("through-hole"));
        assertEquals("chip-resistor-0805-footprint", written("chip-resistor-0805-footprint"));
        assertEquals("1.27", written("1.27"));
    }

    @Test
    void aWordABareWordWouldEndInIsWrittenInQuotes() {
        assertEquals("\"FP.CHIP:R0805\"", written("FP.CHIP:R0805"));
        assertEquals("\"Bat 1.25 mm\"", written("Bat 1.25 mm"));
        assertEquals("\"a,b\"", written("a,b"));
    }
}
