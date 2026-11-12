package org.jmouse.el.lexer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the language reads as an identifier, and the identifier a name written elsewhere becomes. */
class IdentifiersTest {

    @Test
    @DisplayName("an identifier opens with a letter or an underscore")
    void identifiersOpenWithALetter() {
        assertTrue(Identifiers.isIdentifier("VCC"));
        assertTrue(Identifiers.isIdentifier("_hidden2"));
        assertFalse(Identifiers.isIdentifier("3V3"));
        assertFalse(Identifiers.isIdentifier("GND@1"));
        assertFalse(Identifiers.isIdentifier(""));
    }

    @Test
    @DisplayName("a name becomes one: what it cannot hold is an underscore, its case kept, its letters Latin")
    void aNameBecomesAnIdentifier() {
        assertEquals("GND_1", Identifiers.of("GND@1", "pin"));
        assertEquals("SCL_SCK", Identifiers.of("(SCL/SCK)", "pin"));
        assertEquals("pin1", Identifiers.of("1", "pin"));
        assertEquals("P3V3", Identifiers.of("3V3", "P"));
        assertEquals("Vkhid", Identifiers.of("Вхід", "net_"));
        assertEquals("pin", Identifiers.of("~", "pin"));
    }

    @Test
    @DisplayName("what it becomes is what the lexer reads as one token")
    void theLexerReadsItAsOne() {
        assertTrue(Identifiers.isIdentifier(Identifiers.of("IN-(2)", "pin")));
    }
}
