package org.jmouse.java.parser;

import java.math.BigInteger;
import java.util.List;
import org.jmouse.el.node.NodeLifecycle;
import org.jmouse.java.parser.node.declaration.JavaCompactConstructorDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaEnumDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaRecordDeclarationNode;
import org.jmouse.java.parser.node.expression.JavaArrayCreationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaBinaryExpressionNode;
import org.jmouse.java.parser.node.expression.JavaBinaryOperator;
import org.jmouse.java.parser.node.expression.JavaCastExpressionNode;
import org.jmouse.java.parser.node.expression.JavaInstanceOfExpressionNode;
import org.jmouse.java.parser.node.expression.JavaIntegerLiteralNode;
import org.jmouse.java.parser.node.expression.JavaLambdaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodInvocationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaParenthesizedExpressionNode;
import org.jmouse.java.parser.node.expression.JavaPrefixExpressionNode;
import org.jmouse.java.parser.node.expression.JavaSwitchExpressionNode;
import org.jmouse.java.parser.node.expression.JavaUnaryOperator;
import org.jmouse.java.parser.node.module.JavaModuleKind;
import org.jmouse.java.parser.node.module.JavaRequiresDirectiveNode;
import org.jmouse.java.parser.node.pattern.JavaRecordPatternNode;
import org.jmouse.java.parser.node.statement.JavaBlockNode;
import org.jmouse.java.parser.node.statement.JavaEnhancedForStatementNode;
import org.jmouse.java.parser.node.statement.JavaForStatementNode;
import org.jmouse.java.parser.node.statement.JavaIfStatementNode;
import org.jmouse.java.parser.node.statement.JavaPatternCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaSwitchRuleNode;
import org.jmouse.java.parser.node.statement.JavaTryStatementNode;
import org.jmouse.java.parser.node.type.JavaArrayTypeNode;
import org.jmouse.java.parser.node.type.JavaNamedTypeNode;
import org.jmouse.java.parser.node.type.JavaTypeBoundKind;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.node.type.JavaWildcardTypeArgumentNode;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaSyntaxParsingTest {
    @Test
    void moduleUnitsRetainEachDirectiveAndItsTargets() {
        JavaCompilationUnitNode unit = new JavaSyntaxParser().parseCompilationUnit("module-info.java", """
                open module sample.application {
                    requires static transitive java.sql;
                    exports sample.api to consumer.one, consumer.two;
                    opens sample.internal;
                    uses sample.Service;
                    provides sample.Service with sample.DefaultService;
                }
                """);
        assertEquals(JavaModuleKind.OPEN, unit.module().orElseThrow().kind());
        assertEquals(5, unit.module().orElseThrow().directives().size());
        JavaRequiresDirectiveNode requires = assertInstanceOf(JavaRequiresDirectiveNode.class,
                unit.module().orElseThrow().directives().getFirst());
        assertEquals(2, requires.modifiers().entries().size());
        assertTrue(unit.declarations().isEmpty());
    }

    @Test
    void compilationUnitsRetainRecordConstructorsEnumArgumentsAndConstantBodies() {
        JavaCompilationUnitNode unit = new JavaSyntaxParser().parseCompilationUnit("Shapes.java", """
                package demo;
                import java.util.List;
                sealed interface Shape permits Point {}
                record Point(int x, int y) implements Shape {
                    public Point { if (x < 0) throw new IllegalArgumentException(); }
                    public int sum() { return x + y; }
                    static final int[] AXES = {1, 2};
                }
                enum Mode {
                    A(1) { @Override int code() { return 2; } }, B(2);
                    final int value;
                    Mode(int value) { this.value = value; }
                    int code() { return value; }
                }
                """);
        assertEquals(3, unit.declarations().size());
        JavaRecordDeclarationNode record = assertInstanceOf(JavaRecordDeclarationNode.class, unit.declarations().get(1));
        assertEquals(2, record.components().size());
        assertInstanceOf(JavaCompactConstructorDeclarationNode.class, record.body().declarations().getFirst());
        JavaEnumDeclarationNode enumeration = assertInstanceOf(JavaEnumDeclarationNode.class, unit.declarations().getLast());
        assertEquals(2, enumeration.constants().size());
        assertEquals(1, enumeration.constants().getFirst().arguments().size());
        assertEquals(1, enumeration.constants().getFirst().body().orElseThrow().declarations().size());
    }

    @Test
    void switchExpressionsRetainRecordPatternsGuardsAndYieldBlocks() {
        JavaSyntaxParser parser = new JavaSyntaxParser();
        JavaSwitchExpressionNode selection = assertInstanceOf(JavaSwitchExpressionNode.class,
                parser.parseExpression("Selections.java", """
                        switch (value) {
                            case Point(int x, int y) when x > 0 -> x + y;
                            case null, default -> { yield 0; }
                        }
                        """));
        assertEquals(2, selection.body().entries().size());
        JavaSwitchRuleNode first = assertInstanceOf(JavaSwitchRuleNode.class, selection.body().entries().getFirst());
        JavaPatternCaseLabelNode label = assertInstanceOf(JavaPatternCaseLabelNode.class, first.labels().getFirst());
        assertTrue(label.guard().isPresent());
        JavaSwitchRuleNode second = assertInstanceOf(JavaSwitchRuleNode.class, selection.body().entries().getLast());
        assertEquals(2, second.labels().size());
        assertInstanceOf(JavaBlockNode.class, second.outcome());
    }

    @Test
    void lambdasGenericCallsAndArrayInitializersHaveExecutableChildren() {
        JavaSyntaxParser parser = new JavaSyntaxParser();
        JavaLambdaExpressionNode lambda = assertInstanceOf(JavaLambdaExpressionNode.class,
                parser.parseExpression("Functions.java", "(String value) -> { return value.trim(); }"));
        assertEquals(1, lambda.parameters().size());
        assertEquals(NodeLifecycle.FROZEN, lambda.body().lifecycle());
        JavaMethodInvocationExpressionNode call = assertInstanceOf(JavaMethodInvocationExpressionNode.class,
                parser.parseExpression("Functions.java", "factory.<String>create(new int[] {1, 2, 3})"));
        assertEquals(1, call.typeArguments().size());
        JavaArrayCreationExpressionNode array = assertInstanceOf(JavaArrayCreationExpressionNode.class,
                call.arguments().getFirst());
        assertEquals(3, array.initializer().orElseThrow().elements().size());
    }

    @Test
    void bothForFormsPreserveInitializationConditionsUpdatesAndIterationVariables() {
        JavaSyntaxParser parser = new JavaSyntaxParser();
        JavaForStatementNode loop = assertInstanceOf(JavaForStatementNode.class,
                parser.parseStatement("Loops.java", "for (int index = 0; index < count; index++) continue;"));
        assertEquals(1, loop.initializers().size());
        assertTrue(loop.condition().isPresent());
        assertEquals(1, loop.updates().size());
        JavaEnhancedForStatementNode each = assertInstanceOf(JavaEnhancedForStatementNode.class,
                parser.parseStatement("Loops.java", "for (final String name : names) print(name);"));
        assertEquals("name", each.variable().name().name());
        assertEquals(1, each.variable().modifiers().entries().size());
    }

    @Test
    void resourceTryCatchFinallyAndDanglingElseKeepTheirStructuralOwnership() {
        JavaSyntaxParser parser = new JavaSyntaxParser();
        JavaBlockNode block = assertInstanceOf(JavaBlockNode.class, parser.parseStatement("Flow.java", """
                {
                    if (ready) try (Resource resource = open(); existing) {
                        return resource.read();
                    } catch (IOException | RuntimeException failure) {
                        throw failure;
                    } finally {
                        cleanup();
                    } else return;
                }
                """));
        JavaIfStatementNode conditional = assertInstanceOf(JavaIfStatementNode.class, block.statements().getFirst());
        JavaTryStatementNode guarded = assertInstanceOf(JavaTryStatementNode.class, conditional.thenStatement());
        assertEquals(2, guarded.resources().size());
        assertEquals(1, guarded.catches().size());
        assertTrue(guarded.finallyClause().isPresent());
        assertTrue(conditional.elseStatement().isPresent());
        assertSame(guarded, guarded.catches().getFirst().getParent());
        assertEquals(NodeLifecycle.FROZEN, guarded.lifecycle());
    }

    @Test
    void castsAndInstanceofPatternsAreDistinguishedFromParenthesizedExpressions() {
        JavaSyntaxParser parser = new JavaSyntaxParser();
        assertInstanceOf(JavaCastExpressionNode.class,
                parser.parseExpression("Casts.java", "(String) value"));
        JavaBinaryExpressionNode addition = assertInstanceOf(JavaBinaryExpressionNode.class,
                parser.parseExpression("Casts.java", "(value) + 1"));
        assertInstanceOf(JavaParenthesizedExpressionNode.class, addition.left());
        JavaInstanceOfExpressionNode test = assertInstanceOf(
                JavaInstanceOfExpressionNode.class,
                parser.parseExpression("Casts.java", "value instanceof Point(int x, int y)"));
        assertInstanceOf(JavaRecordPatternNode.class, test.pattern().orElseThrow());
    }

    @Test
    void annotatedGenericArrayTypesRetainEveryArgumentAndDimension() {
        JavaSyntaxParser parser = new JavaSyntaxParser();
        JavaArrayTypeNode array = assertInstanceOf(JavaArrayTypeNode.class,
                parser.parseType("Types.java", "@A java.util.Map<String, ? extends List<@B Integer[]>> @C []"));
        assertEquals(1, array.dimensions().size());
        assertEquals(1, array.dimensions().getFirst().annotations().size());
        JavaNamedTypeNode map = assertInstanceOf(JavaNamedTypeNode.class, array.elementType());
        assertEquals(3, map.segments().size());
        assertEquals(1, map.segments().getFirst().annotations().size());
        List<JavaTypeSyntaxNode> arguments = map.segments().getLast().arguments();
        assertEquals(2, arguments.size());
        JavaWildcardTypeArgumentNode wildcard = assertInstanceOf(JavaWildcardTypeArgumentNode.class,
                arguments.getLast());
        assertEquals(JavaTypeBoundKind.EXTENDS, wildcard.kind());
        assertTrue(wildcard.bound().isPresent());
        assertEquals(NodeLifecycle.FROZEN, wildcard.lifecycle());
    }

    @Test
    void javaExpressionsRetainUnaryBoundariesPrecedenceValuesAndSourceSpans() {
        JavaSyntaxParser parser = new JavaSyntaxParser();
        JavaBinaryExpressionNode sum = assertInstanceOf(JavaBinaryExpressionNode.class,
                parser.parseExpression("Numbers.java", "a + +0x10 * 2"));
        assertEquals(JavaBinaryOperator.ADD, sum.operator());
        JavaBinaryExpressionNode product = assertInstanceOf(JavaBinaryExpressionNode.class, sum.right());
        assertEquals(JavaBinaryOperator.MULTIPLY, product.operator());
        JavaPrefixExpressionNode positive = assertInstanceOf(JavaPrefixExpressionNode.class, product.left());
        assertEquals(JavaUnaryOperator.PLUS, positive.operator());
        JavaIntegerLiteralNode sixteen = assertInstanceOf(JavaIntegerLiteralNode.class, positive.operand());
        assertEquals(BigInteger.valueOf(16), sixteen.value());
        assertEquals("0x10", sixteen.raw());
        assertEquals(5, sixteen.span().start().offset());
        assertSame(positive, sixteen.getParent());
        assertEquals(NodeLifecycle.FROZEN, sum.lifecycle());
    }
}
