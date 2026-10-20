package org.jmouse.el.language.line;

import org.jmouse.el.lexer.BasicToken;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineReaderTest {

    @Test
    void wordsAreWhatAPersonReads() {
        LineReader reader = new LineReader("probe", "row link-a \"supply voltage\" see 74hc595.jmd#link-a\n");
        reader.skipBlank();
        TokenLine line = reader.line();
        assertEquals("row", line.joinedWord(0));
        assertEquals("link-a", line.joinedWord(1));
        int title = line.joinedEnd(1);
        assertTrue(line.isString(title));
        assertEquals("supply voltage", line.literal(title));
        assertEquals("see", line.joinedWord(title + 1));
        assertEquals("74hc595.jmd#link-a", line.joinedWord(title + 2));
        assertEquals(line.size(), line.joinedEnd(title + 2));
    }

    @Test
    void unitsJoinTheirPrefix() {
        LineReader reader = new LineReader("probe", "rise 2.2 µs; temperature -40 °C");
        TokenLine first = reader.line();
        assertEquals("µs", first.joinedWord(2));
        reader.skipBlank();
        TokenLine second = reader.line();
        assertEquals("-40", second.joinedWord(1));
        assertEquals("°C", second.joinedWord(2));
    }

    @Test
    void aLooseHashIsAComment() {
        LineReader reader = new LineReader("probe", "# heading\nvalue 5 # five\nnext {\n}\n");
        reader.skipBlank();
        TokenLine value = reader.line();
        assertEquals(2, value.size());
        assertEquals("value 5", value.text(0, value.size()));
        reader.skipBlank();
        TokenLine block = reader.line();
        assertEquals("next", block.joinedWord(0));
        assertTrue(reader.at(BasicToken.T_OPEN_CURLY));
        reader.expect(BasicToken.T_OPEN_CURLY, "'{'");
        reader.skipBlank();
        reader.expect(BasicToken.T_CLOSE_CURLY, "'}'");
        reader.skipBlank();
        assertTrue(reader.atEnd());
    }

    @Test
    void separatorsEndAWord() {
        TokenLine line = new LineReader("probe", "list a, b=c").line();
        assertFalse(line.joined(1));
        assertEquals(1, line.commas(0, line.size()).size());
        assertEquals("b", line.joinedWord(3));
    }

    @Test
    void aRefusalSaysWhere() {
        LineReader reader = new LineReader("probe.jmd", "sheet\n  oops");
        reader.skipBlank();
        reader.line();
        reader.skipBlank();
        LineSyntaxException refusal = assertThrows(LineSyntaxException.class,
                () -> reader.expect(BasicToken.T_OPEN_CURLY, "'{'"));
        assertEquals(2, refusal.line());
        assertEquals(3, refusal.column());
        assertEquals("probe.jmd", refusal.document());
    }
}
