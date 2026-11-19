package org.jmouse.java.parser;

import org.jmouse.el.parser.ParsingBudget;
import org.jmouse.el.parser.ParsingLimitException;
import org.jmouse.java.model.version.JavaRelease;
import org.jmouse.java.model.version.JavaSourceProfile;
import org.jmouse.java.parser.grammar.JavaSyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaSyntaxRejectionTest {
    private final JavaSyntaxParser parser = new JavaSyntaxParser();

    @ParameterizedTest
    @ValueSource(strings = {"0x", "09", "1__", "0b2", "1e+", "\"\\q\"", "'ab'", "new int[1] {2}",
            "new int[]", "new int[][2]", "1 trailing", "1 = 2", "left + right = value"})
    void malformedExpressionsCannotProduceACompleteTree(String source) {
        assertThrows(JavaSyntaxException.class, () -> parser.parseExpression("Invalid.java", source));
    }

    @ParameterizedTest
    @ValueSource(strings = {"List<int>", "List<>", "List<void>", "List<var>"})
    void typeArgumentsMustBeReferenceTypesAndDiamondBelongsToCreation(String source) {
        assertThrows(JavaSyntaxException.class, () -> parser.parseType("Invalid.java", source));
    }

    @ParameterizedTest
    @ValueSource(strings = {"class record {}", "class Example<var> {}"})
    void restrictedTypeIdentifiersCannotDeclareTypesOrTypeVariables(String source) {
        assertThrows(JavaSyntaxException.class, () -> parser.parseCompilationUnit("Invalid.java", source));
    }

    @ParameterizedTest
    @ValueSource(strings = {"class Example { void method(var value) {} }",
            "class Example { void method(public String value) {} }",
            "record Example(final int value) {}",
            "class Example { void method() { public int value = 1; } }"})
    void variableAndParameterContextsHaveTheirOwnGrammar(String source) {
        assertThrows(JavaSyntaxException.class, () -> parser.parseCompilationUnit("Invalid.java", source));
    }

    @Test
    void sourceProfilesRejectFutureSyntaxAndNestingUsesTheSharedBudget() {
        JavaSyntaxParser older = new JavaSyntaxParser(JavaSourceProfile.standard(new JavaRelease(17)),
                JavaSyntaxParser.defaultBudget(), JavaSyntaxParser.standardRules());
        assertThrows(JavaSyntaxException.class,
                () -> older.parseExpression("Patterns.java", "value instanceof Point(int x, int y)"));
        JavaSyntaxParser bounded = new JavaSyntaxParser(JavaSourceProfile.standard(new JavaRelease(21)),
                new ParsingBudget(32, 10000), JavaSyntaxParser.standardRules());
        assertThrows(ParsingLimitException.class,
                () -> bounded.parseExpression("Nested.java", "(".repeat(1000) + "0" + ")".repeat(1000)));
    }
}
