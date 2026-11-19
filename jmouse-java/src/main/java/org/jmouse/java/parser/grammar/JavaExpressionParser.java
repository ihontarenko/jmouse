package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.el.parser.expression.ExpressionReader;
import org.jmouse.el.parser.expression.InfixBinding;
import org.jmouse.el.parser.expression.PrecedenceGrammar;
import org.jmouse.el.parser.expression.PrecedenceParser;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.node.expression.JavaArrayAccessExpressionNode;
import org.jmouse.java.parser.node.expression.JavaAssignmentExpressionNode;
import org.jmouse.java.parser.node.expression.JavaAssignmentOperator;
import org.jmouse.java.parser.node.expression.JavaBinaryExpressionNode;
import org.jmouse.java.parser.node.expression.JavaBinaryOperator;
import org.jmouse.java.parser.node.expression.JavaConditionalExpressionNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaFieldAccessExpressionNode;
import org.jmouse.java.parser.node.expression.JavaInstanceOfExpressionNode;
import org.jmouse.java.parser.node.expression.JavaNameExpressionNode;
import org.jmouse.java.parser.node.expression.JavaParenthesizedExpressionNode;
import org.jmouse.java.parser.node.expression.JavaPrefixExpressionNode;
import org.jmouse.java.parser.node.expression.JavaUnaryOperator;

import java.util.Objects;
import java.util.Set;

/** Java expression construction on jME's shared precedence engine. */
public final class JavaExpressionParser implements ParseRule<JavaParsingInput, JavaExpressionNode> {
    private static final Set<JavaOperatorCatalog.Kind> INFIX =
            Set.of(
                    JavaOperatorCatalog.Kind.BINARY,
                    JavaOperatorCatalog.Kind.ASSIGNMENT,
                    JavaOperatorCatalog.Kind.CONDITIONAL,
                    JavaOperatorCatalog.Kind.TYPE_TEST);
    private final JavaOperatorCatalog operators;
    private final int minimumPrecedence;
    private final int prefixPrecedence;
    private final int lambdaMaximumPrecedence;
    private final JavaGrammarContext.ExpressionForm form;

    public JavaExpressionParser(
            JavaOperatorCatalog operators, int minimumPrecedence, int prefixPrecedence) {
        this(
                operators,
                minimumPrecedence,
                prefixPrecedence,
                JavaGrammarContext.ExpressionForm.VALUE);
    }

    public JavaExpressionParser(
            JavaOperatorCatalog operators,
            int minimumPrecedence,
            int prefixPrecedence,
            JavaGrammarContext.ExpressionForm form) {
        this.operators = Objects.requireNonNull(operators, "operators");
        this.minimumPrecedence = minimumPrecedence;
        this.prefixPrecedence = prefixPrecedence;
        this.form = Objects.requireNonNull(form, "form");
        lambdaMaximumPrecedence =
                operators.definitions().stream()
                        .filter(
                                definition ->
                                        definition.kind() == JavaOperatorCatalog.Kind.CONDITIONAL)
                        .mapToInt(JavaOperatorCatalog.Definition::precedence)
                        .max()
                        .orElse(minimumPrecedence);
    }

    @Override
    public JavaExpressionNode parse(JavaParsingInput input, ParseSession session) {
        return session.with(
                JavaGrammarContext.PRIMARY_FORM,
                JavaGrammarContext.PrimaryForm.STANDARD,
                () ->
                        session.with(
                                JavaGrammarContext.EXPRESSION_FORM,
                                form,
                                () ->
                                        PrecedenceParser.parse(
                                                input,
                                                minimumPrecedence,
                                                new Grammar(session),
                                                session)));
    }

    private final class Grammar
            implements PrecedenceGrammar<
                    JavaParsingInput, JavaExpressionNode, JavaOperatorCatalog.Definition> {
        private final ParseSession session;

        private Grammar(ParseSession session) {
            this.session = session;
        }

        @Override
        public JavaExpressionNode primary(
                JavaParsingInput input, ExpressionReader<JavaExpressionNode> expressions) {
            return primary(input, minimumPrecedence, expressions);
        }

        @Override
        public JavaExpressionNode primary(
                JavaParsingInput input,
                int minimum,
                ExpressionReader<JavaExpressionNode> expressions) {
            int start = input.offset();
            JavaOperatorCatalog.Definition prefix =
                    operators.find(input, Set.of(JavaOperatorCatalog.Kind.PREFIX));
            if (prefix != null) {
                input.expect(prefix.spelling(), session);
                JavaPrefixExpressionNode node =
                        new JavaPrefixExpressionNode(input.name(), input.point());
                node.setOperator(JavaUnaryOperator.valueOf(prefix.identity()));
                node.setOperand(expressions.parse(prefixPrecedence));
                return input.finish(node, start);
            }
            JavaGrammarContext.ExpressionForm operandForm =
                    form == JavaGrammarContext.ExpressionForm.VALUE
                                    && minimum <= lambdaMaximumPrecedence
                            ? JavaGrammarContext.ExpressionForm.VALUE
                            : JavaGrammarContext.ExpressionForm.CONDITIONAL;
            return session.with(
                    JavaGrammarContext.EXPRESSION_FORM,
                    operandForm,
                    () -> session.parse(JavaParseRoles.PRIMARY, input));
        }

        @Override
        public InfixBinding<JavaOperatorCatalog.Definition> infix(
                JavaParsingInput input, JavaExpressionNode left) {
            JavaOperatorCatalog.Definition definition = operators.find(input, INFIX);
            return definition == null
                    ? null
                    : new InfixBinding<>(
                            definition, definition.precedence(), definition.associativity());
        }

        @Override
        public JavaExpressionNode reduce(
                JavaParsingInput input,
                JavaExpressionNode left,
                InfixBinding<JavaOperatorCatalog.Definition> binding,
                ExpressionReader<JavaExpressionNode> expressions) {
            int start = left.span().start().offset();
            JavaOperatorCatalog.Definition operator = binding.operator();
            input.expect(operator.spelling(), session);
            return switch (operator.kind()) {
                case BINARY -> {
                    JavaBinaryExpressionNode node =
                            new JavaBinaryExpressionNode(input.name(), input.point());
                    node.setOperator(JavaBinaryOperator.valueOf(operator.identity()));
                    node.setLeft(left);
                    node.setRight(expressions.parse(binding.rightMinimum()));
                    yield input.finish(node, start);
                }
                case ASSIGNMENT -> {
                    requireAssignmentTarget(input, left);
                    JavaAssignmentExpressionNode node =
                            new JavaAssignmentExpressionNode(input.name(), input.point());
                    node.setOperator(JavaAssignmentOperator.valueOf(operator.identity()));
                    node.setTarget(left);
                    node.setValue(expressions.parse(binding.rightMinimum()));
                    yield input.finish(node, start);
                }
                case CONDITIONAL -> {
                    JavaConditionalExpressionNode node =
                            new JavaConditionalExpressionNode(input.name(), input.point());
                    node.setCondition(left);
                    node.setThenExpression(expressions.parse(minimumPrecedence));
                    input.expect(":", session);
                    node.setElseExpression(expressions.parse(binding.rightMinimum()));
                    yield input.finish(node, start);
                }
                case TYPE_TEST -> {
                    JavaInstanceOfExpressionNode node =
                            new JavaInstanceOfExpressionNode(input.name(), input.point());
                    node.setExpression(left);
                    JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
                    try {
                        node.setPattern(session.parse(JavaParseRoles.PATTERN, input));
                    } catch (JavaSyntaxException exception) {
                        input.restore(checkpoint);
                        node.setType(session.parse(JavaParseRoles.TYPE, input));
                    }
                    if (node.pattern().isPresent()) {
                        input.requireFeature(JavaSourceFeatures.INSTANCEOF_PATTERNS);
                    }
                    yield input.finish(node, start);
                }
                default -> throw input.error("Expected a supported infix operation");
            };
        }

        @Override
        public long position(JavaParsingInput input) {
            return input.cursor().position();
        }
    }

    private static void requireAssignmentTarget(
            JavaParsingInput input, JavaExpressionNode expression) {
        JavaExpressionNode target = expression;
        while (target instanceof JavaParenthesizedExpressionNode parenthesized) {
            target = parenthesized.expression();
        }
        if (!(target instanceof JavaNameExpressionNode
                || target instanceof JavaFieldAccessExpressionNode
                || target instanceof JavaArrayAccessExpressionNode)) {
            throw input.errorAt(
                    "An assignment requires a variable access on the left", expression.span());
        }
    }
}
