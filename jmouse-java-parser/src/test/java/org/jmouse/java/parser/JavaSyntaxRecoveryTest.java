package org.jmouse.java.parser;

import java.util.List;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.projection.JavaDeclarationProjection;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.jmouse.java.parser.grammar.JavaRecoveryPolicy;
import org.jmouse.java.parser.grammar.statement.JavaStatementRoles;
import org.jmouse.java.parser.node.statement.JavaSwitchStatementNode;
import org.jmouse.java.parser.node.statement.JavaBlockNode;
import org.jmouse.el.parser.ParsingBudget;
import org.jmouse.el.parser.ParsingLimitException;
import org.jmouse.java.parser.extension.JavaSourceLanguage;
import org.jmouse.java.parser.source.ProblemKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The same recovery contract as the compatibility facade, exercised through the new full AST. */
class JavaSyntaxRecoveryTest extends RecoveryTest {
    private final JavaSyntaxParser syntaxParser = new JavaSyntaxParser();

    @Override
    protected JavaSourceFile read(String name, String text) {
        return JavaDeclarationProjection.project(syntaxParser.recoverCompilationUnit(name, text));
    }

    @Test
    void malformedPackageAndImportHeadersDoNotEscapeRecovery() {
        JavaSourceFile file = read("Headers.java", """
                package 42;
                import java.util.;
                import java.util.List;
                class Kept {}
                """);

        assertEquals(List.of("Kept"), file.types().stream().map(type -> type.name()).toList());
        assertEquals(1, file.imports().size());
        assertEquals(2, file.problems().size());
    }

    @Test
    void aBrokenModuleDirectiveKeepsFollowingDirectives() {
        JavaSyntaxResult<JavaCompilationUnitNode> result = syntaxParser.recoverCompilationUnit("module-info.java", """
                module example {
                    requires ;
                    exports example.api;
                    uses example.Service;
                }
                """);

        assertEquals(2, result.root().module().orElseThrow().directives().size());
        assertEquals(1, result.problems().size());
    }

    @Test
    void aBrokenSwitchRuleKeepsFollowingRulesDespiteAnUnclosedArgumentList() {
        JavaSyntaxResult<JavaSwitchStatementNode> result = syntaxParser.parseSource(
                JavaStatementRoles.SWITCH_STATEMENT, "switch.java", """
                switch (value) {
                    case 0 -> broken( ;
                    case 1 -> ready();
                    default -> finish();
                }
                """, JavaRecoveryPolicy.RECOVER);

        assertEquals(2, result.root().body().entries().size());
        assertEquals(1, result.problems().size());
    }

    @Test
    void anExhaustedWorkBudgetReturnsTheAlreadyReadTypesAndExplicitDamage() {
        JavaSyntaxParser bounded = new JavaSyntaxParser(JavaSourceLanguage.standard(), new ParsingBudget(192, 80));
        String text = "class Kept {} class TooMuch {" + "int value;".repeat(100) + "}";
        JavaSyntaxResult<JavaCompilationUnitNode> result = bounded.recoverCompilationUnit("Bounded.java", text);

        assertFalse(result.isComplete());
        assertEquals("Kept", JavaDeclarationProjection.project(result).types().getFirst().name());
        assertEquals(ProblemKind.WORK_BUDGET_EXCEEDED, result.problems().getLast().kind());
        assertThrows(ParsingLimitException.class, () -> bounded.parseCompilationUnit("Bounded.java", text));
    }

    @Test
    void excessiveExpressionDepthKeepsTheFollowingType() {
        JavaSyntaxParser bounded = new JavaSyntaxParser(JavaSourceLanguage.standard(), new ParsingBudget(48, 100000));
        String text = "class Deep { int value() { return " + "(".repeat(200) + "1" + ")".repeat(200)
                + "; } } class Following {}";
        JavaSyntaxResult<JavaCompilationUnitNode> result = bounded.recoverCompilationUnit("Deep.java", text);

        assertEquals(List.of("Deep", "Following"), JavaDeclarationProjection.project(result).types().stream()
                .map(type -> type.name()).toList());
        assertEquals(ProblemKind.NESTING_TOO_DEEP, result.problems().getFirst().kind());
    }

    @Test
    void anUnclosedCallDoesNotSwallowTheFollowingStatement() {
        JavaSyntaxResult<JavaBlockNode> result = syntaxParser.parseSource(JavaStatementRoles.BLOCK,
                "body.java", "{ receiver.call(; next(); }", JavaRecoveryPolicy.RECOVER);

        assertEquals(1, result.root().statements().size());
        assertEquals(1, result.problems().size());
    }
}
