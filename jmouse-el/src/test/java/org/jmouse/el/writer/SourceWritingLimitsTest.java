package org.jmouse.el.writer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SourceWritingLimitsTest {
    @Test
    void writingLimitsCountSeparatorsIndentationAndLineBreaksBeforeAppending() {
        TextSourceWriter line = new TextSourceWriter(SourceLayout.WORDS, 11);
        line.word("class").word("Test").join(";");
        assertEquals(11, line.length());
        assertThrows(SourceWritingLimitException.class, () -> line.word("x"));
        assertEquals("class Test;", line.text());

        TextSourceWriter block = new TextSourceWriter(SourceLayout.WORDS, 14);
        block.word("root").block("{", "}", body -> body.word("x"));
        assertEquals("root {\n    x\n}", block.text());
        assertEquals(14, block.length());
        assertThrows(SourceWritingLimitException.class, block::endLine);
        assertEquals("root {\n    x\n}", block.text());
    }
}
