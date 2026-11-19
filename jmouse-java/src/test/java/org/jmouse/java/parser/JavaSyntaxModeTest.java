package org.jmouse.java.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.java.parser.grammar.JavaParsingMode;
import org.jmouse.java.parser.grammar.JavaRecoveryPolicy;
import org.jmouse.java.parser.grammar.JavaSyntaxException;
import org.jmouse.java.parser.grammar.unit.JavaUnitRoles;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.jmouse.java.parser.projection.JavaDeclarationProjection;
import org.jmouse.java.parser.projection.JavaExecutableProjection;
import org.junit.jupiter.api.Test;

class JavaSyntaxModeTest {
    @Test
    void declarationReadingRetainsHeadersAndExplicitlyMarksUnreadExecutableSyntax() {
        String text = "class Example { void method() { ???; } int field; }";
        JavaSyntaxParser parser = new JavaSyntaxParser();
        JavaSyntaxResult<JavaCompilationUnitNode> result =
                parser.parseSource(
                        JavaUnitRoles.COMPILATION_UNIT,
                        "Example.java",
                        text,
                        JavaParsingMode.DECLARATIONS_ONLY,
                        JavaRecoveryPolicy.RECOVER);

        assertTrue(result.problems().isEmpty());
        assertFalse(result.isComplete());
        assertEquals(1, result.omittedRegions().size());
        assertEquals(" ???; ", result.omittedRegions().getFirst().span().slice(text));
        assertEquals(
                2, JavaDeclarationProjection.project(result).types().getFirst().members().size());
        assertThrows(
                TranslationRefusedException.class, () -> JavaExecutableProjection.project(result));
        assertThrows(
                JavaSyntaxException.class, () -> parser.parseCompilationUnit("Example.java", text));
    }
}
