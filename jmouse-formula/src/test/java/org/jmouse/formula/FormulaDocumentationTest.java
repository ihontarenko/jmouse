package org.jmouse.formula;

import org.jmouse.documentation.DocumentationPage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaDocumentationTest {

    @Test
    void saysTheLanguageThenEveryFunctionAndConstantInTheirOwnWords() {
        DocumentationPage page = FormulaDocumentation.page();
        assertEquals(FormulaDocumentation.SUBJECT, page.subject());
        assertTrue(page.sections().stream().anyMatch(section -> section.title().equals("Notation")), page.sections().toString());
        assertTrue(page.sections().stream().anyMatch(section -> section.title().equals("Functions")), page.sections().toString());
        // Every function is listed, with the words of its own Javadoc.
        for (MathematicalFunction function : MathematicalFunction.values()) {
            assertTrue(page.text().contains("`" + function.symbol() + "("), function.symbol());
        }
        assertTrue(page.text().contains("The sine of an angle in radians."), "sin's own words");
        assertTrue(page.text().contains("the golden ratio"), "phi's own words");
    }
}
