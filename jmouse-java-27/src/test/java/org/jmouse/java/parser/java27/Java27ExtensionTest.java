package org.jmouse.java.parser.java27;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.el.translate.NodeOperationBindingException;
import org.jmouse.java.parser.JavaParser;
import org.jmouse.java.parser.JavaSyntaxParser;
import org.jmouse.java.parser.grammar.JavaSyntaxException;
import org.jmouse.java.parser.grammar.unit.JavaUnitRoles;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.projection.JavaExecutableProjection;
import org.jmouse.java.parser.projection.JavaProjectionNames;
import org.junit.jupiter.api.Test;

class Java27ExtensionTest {
    @Test
    void anIndependentModuleExtendsLexingGrammarAndBothNavigationViews() {
        JavaParser parser = Java27Example.parser();
        String text =
                "import bundle sample.tools; class Example { void run(int input) { inspect #(input"
                        + " + 1); } }";
        JavaSourceFile file = parser.parse("Example.java", text, JavaParser.Level.EXPRESSIONS);
        assertTrue(file.isSyntaxComplete(), file.problems().toString());
        assertEquals("sample.tools", file.imports().getFirst().name());
        assertTrue(file.imports().getFirst().onDemand());
        assertEquals(
                1,
                file.bodies().getFirst().references().stream()
                        .filter(reference -> reference.name().equals("input"))
                        .count());
        assertEquals(27, parser.parseSyntax("Example.java", text).profile().release().number());
        assertFalse(parser.parse("Example.java", text).isSyntaxComplete());
        assertThrows(
                JavaSyntaxException.class,
                () -> new JavaSyntaxParser().parseCompilationUnit("Example.java", text));
    }

    @Test
    void aMissingProjectionIsAnExplicitExactNodeBindingFailure() {
        NodeOperationBindingException expression =
                assertThrows(
                        NodeOperationBindingException.class,
                        () ->
                                JavaExecutableProjection.project(
                                        Java27Example.syntaxParser()
                                                .parseSource(
                                                        JavaUnitRoles.COMPILATION_UNIT,
                                                        "Example.java",
                                                        "class Example { int value = #(1); }")));
        assertEquals(Java27ValueNode.KIND.name(), expression.nodeKind());
        assertEquals(JavaProjectionNames.EXPRESSION, expression.operation());
        NodeOperationBindingException statement =
                assertThrows(
                        NodeOperationBindingException.class,
                        () ->
                                JavaExecutableProjection.project(
                                        Java27Example.syntaxParser()
                                                .parseSource(
                                                        JavaUnitRoles.COMPILATION_UNIT,
                                                        "Example.java",
                                                        "class Example { void run() { inspect 1; }"
                                                                + " }")));
        assertEquals(Java27InspectNode.KIND.name(), statement.nodeKind());
        assertEquals(JavaProjectionNames.STATEMENT, statement.operation());
    }
}
