package org.jmouse.formula;

import org.jmouse.formula.el.FormulaLanguage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormulaPrecedenceTest {
    @Test
    void powersRemainRightAssociativeAndBindMoreTightlyThanALeadingSign() {
        FormulaLanguage language = new FormulaLanguage();
        assertEquals(512.0, language.evaluate("2^3^2", Map.of()));
        assertEquals(-4.0, language.evaluate("-2^2", Map.of()));
        assertEquals(0.125, language.evaluate("2^-3", Map.of()));
    }

    @Test
    void juxtapositionAndPostfixFactorialsRemainFormulaSyntax() {
        FormulaLanguage language = new FormulaLanguage();
        assertEquals(14.0, language.evaluate("2(3 + 4)", Map.of()));
        assertEquals(720.0, language.evaluate("3!!", Map.of()));
        assertEquals(17.0, language.evaluate("2x + 3", Map.of("x", 7)));
    }
}
