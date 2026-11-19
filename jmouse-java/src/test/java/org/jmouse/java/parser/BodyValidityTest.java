package org.jmouse.java.parser;

import static org.junit.jupiter.api.Assertions.*;

import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.source.ParseProblem;
import org.junit.jupiter.api.Test;

class BodyValidityTest {
    private final JavaParser parser = new JavaParser();

    @Test
    void switchExpressionsAreReadCompletelyAcrossAllArms() {
        JavaSourceFile file =
                parser.parse(
                        "Switch.java",
                        "class Example { int value(int input) { return switch(input) { case 1 -> 2;"
                                + " default -> 3; }; } void valid() {} }",
                        JavaParser.Level.EXPRESSIONS);
        assertTrue(file.isStructureComplete());
        assertTrue(file.isSyntaxComplete(), file.problems().toString());
        assertTrue(file.isBodyComplete(file.bodies().getFirst().owner()));
        assertTrue(file.isBodyComplete(file.bodies().getLast().owner()));
    }

    @Test
    void primitiveAndVoidClassLiteralsAreSupported() {
        JavaSourceFile file =
                parser.parse(
                        "Literal.java",
                        "class Example { Object first() { return int.class; } Object second() {"
                                + " return void.class; } }",
                        JavaParser.Level.EXPRESSIONS);
        assertTrue(file.isComplete(), file.problems().toString());
    }

    @Test
    void lexicalDamageIsDifferentFromBodyRecovery() {
        JavaSourceFile file =
                parser.parse(
                        "Lexical.java",
                        "class Example { String value = \"unfinished; }",
                        JavaParser.Level.EXPRESSIONS);
        assertFalse(file.isStructureComplete());
        assertTrue(
                file.problems().stream()
                        .anyMatch(problem -> problem.phase() == ParseProblem.Phase.LEXICAL));
    }
}
