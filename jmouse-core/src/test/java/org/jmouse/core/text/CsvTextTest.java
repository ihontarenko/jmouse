package org.jmouse.core.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvTextTest {

    @Test
    void aFieldIsQuotedOnlyWhenItMustBe() {
        assertEquals("plain text", CsvText.field("plain text"));
        assertEquals("\"a,b\"", CsvText.field("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", CsvText.field("say \"hi\""));
        assertEquals("\"two\nlines\"", CsvText.field("two\nlines"));
        assertEquals("\"two\rlines\"", CsvText.field("two\rlines"));
    }

    @Test
    void aRowJoinsItsFields() {
        assertEquals("R1,\"10,5\",", CsvText.row(List.of("R1", "10,5", "")));
    }

    @Test
    void textThatWouldRunAsAFormulaIsShownInstead() {
        assertEquals("'=HYPERLINK(\"x\")", CsvText.inert("=HYPERLINK(\"x\")"));
        assertEquals("'+1", CsvText.inert("+1"));
        assertEquals("'-1", CsvText.inert("-1"));
        assertEquals("'@cmd", CsvText.inert("@cmd"));
        assertEquals("LM358-N", CsvText.inert("LM358-N"));
        assertEquals("", CsvText.inert(""));
    }
}
