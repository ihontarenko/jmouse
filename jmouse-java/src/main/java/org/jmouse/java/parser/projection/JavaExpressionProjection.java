package org.jmouse.java.parser.projection;

import org.jmouse.el.lexer.TokenizableSource.Entry;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.TraversalAction;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.java.model.type.JavaPlatformTypeNames;
import org.jmouse.java.parser.grammar.JavaOperatorCatalog;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.model.Expression;
import org.jmouse.java.parser.node.body.JavaAnonymousClassBodyNode;
import org.jmouse.java.parser.node.declaration.JavaTypeDeclarationNode;
import org.jmouse.java.parser.node.expression.JavaArrayAccessExpressionNode;
import org.jmouse.java.parser.node.expression.JavaArrayCreationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaArrayInitializerNode;
import org.jmouse.java.parser.node.expression.JavaAssignmentExpressionNode;
import org.jmouse.java.parser.node.expression.JavaBinaryExpressionNode;
import org.jmouse.java.parser.node.expression.JavaBooleanLiteralNode;
import org.jmouse.java.parser.node.expression.JavaCastExpressionNode;
import org.jmouse.java.parser.node.expression.JavaCharacterLiteralNode;
import org.jmouse.java.parser.node.expression.JavaClassInstanceCreationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaClassLiteralExpressionNode;
import org.jmouse.java.parser.node.expression.JavaConditionalExpressionNode;
import org.jmouse.java.parser.node.expression.JavaConstructorInvocationNode;
import org.jmouse.java.parser.node.expression.JavaErrorExpressionNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaFieldAccessExpressionNode;
import org.jmouse.java.parser.node.expression.JavaFloatingLiteralValue;
import org.jmouse.java.parser.node.expression.JavaFloatingPointLiteralNode;
import org.jmouse.java.parser.node.expression.JavaInstanceOfExpressionNode;
import org.jmouse.java.parser.node.expression.JavaIntegerLiteralNode;
import org.jmouse.java.parser.node.expression.JavaLambdaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaLiteralNode;
import org.jmouse.java.parser.node.expression.JavaMethodInvocationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodReferenceExpressionNode;
import org.jmouse.java.parser.node.expression.JavaNameExpressionNode;
import org.jmouse.java.parser.node.expression.JavaNullLiteralNode;
import org.jmouse.java.parser.node.expression.JavaParenthesizedExpressionNode;
import org.jmouse.java.parser.node.expression.JavaPostfixExpressionNode;
import org.jmouse.java.parser.node.expression.JavaPrefixExpressionNode;
import org.jmouse.java.parser.node.expression.JavaStringLiteralNode;
import org.jmouse.java.parser.node.expression.JavaSuperQualifierNode;
import org.jmouse.java.parser.node.expression.JavaSwitchExpressionNode;
import org.jmouse.java.parser.node.expression.JavaTextBlockLiteralNode;
import org.jmouse.java.parser.node.expression.JavaThisExpressionNode;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A lossy navigation view of expressions; the compiler continues to consume the full syntax nodes.
 */
final class JavaExpressionProjection {
    private final JavaOperatorCatalog operators = JavaOperatorCatalog.standard();
    private final Map<JavaExpressionNode, Expression> expressions = new IdentityHashMap<>();
    private final JavaProjectionContext context;

    JavaExpressionProjection(JavaProjectionContext context) {
        this.context = context;
    }

    Expression read(JavaExpressionNode root) {
        if (!expressions.containsKey(root)) {
            root.accept(
                    new Visitor() {
                        @Override
                        public TraversalAction enter(Node node) {
                            if (node instanceof JavaAnonymousClassBodyNode
                                    || node instanceof JavaTypeDeclarationNode) {
                                return TraversalAction.SKIP_CHILDREN;
                            }
                            return TraversalAction.CONTINUE;
                        }

                        @Override
                        public void leave(Node node) {
                            if (node instanceof JavaExpressionNode expression) {
                                expressions.put(expression, project(expression));
                            }
                        }
                    });
        }
        return expressions.get(root);
    }

    private Expression child(JavaExpressionNode node) {
        Expression expression = expressions.get(node);
        if (expression == null) {
            throw new IllegalStateException(
                    "A child expression must be projected before its parent");
        }
        return expression;
    }

    Expression constructor(JavaConstructorInvocationNode node) {
        return new Expression(
                Expression.Kind.CALL,
                JavaKeyword.valueOf(node.kind().name()).spelling(),
                node.keywordSpan(),
                node.span(),
                node.qualifier().map(this::read).orElse(null),
                node.arguments().stream().map(this::read).toList());
    }

    static void install(NodeOperationRegistry<JavaProjectionContext, Expression> registry) {
        registry.bind(
                JavaErrorExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaErrorExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaNameExpressionNode.KIND,
                JavaExpressionProjection.class.getName() + "#" + JavaNameExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaThisExpressionNode.KIND,
                JavaExpressionProjection.class.getName() + "#" + JavaThisExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaSuperQualifierNode.KIND,
                JavaExpressionProjection.class.getName() + "#" + JavaSuperQualifierNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaClassLiteralExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaClassLiteralExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaCastExpressionNode.KIND,
                JavaExpressionProjection.class.getName() + "#" + JavaCastExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaConditionalExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaConditionalExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaInstanceOfExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaInstanceOfExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaFieldAccessExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaFieldAccessExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaMethodInvocationExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaMethodInvocationExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaClassInstanceCreationExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaClassInstanceCreationExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaMethodReferenceExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaMethodReferenceExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaAssignmentExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaAssignmentExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaArrayInitializerNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaArrayInitializerNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaArrayCreationExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaArrayCreationExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaArrayAccessExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaArrayAccessExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaPrefixExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaPrefixExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaPostfixExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaPostfixExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaBinaryExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaBinaryExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaParenthesizedExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaParenthesizedExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaLambdaExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaLambdaExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaSwitchExpressionNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaSwitchExpressionNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaBooleanLiteralNode.KIND,
                JavaExpressionProjection.class.getName() + "#" + JavaBooleanLiteralNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaCharacterLiteralNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaCharacterLiteralNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaStringLiteralNode.KIND,
                JavaExpressionProjection.class.getName() + "#" + JavaStringLiteralNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaTextBlockLiteralNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaTextBlockLiteralNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaNullLiteralNode.KIND,
                JavaExpressionProjection.class.getName() + "#" + JavaNullLiteralNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaIntegerLiteralNode.KIND,
                JavaExpressionProjection.class.getName() + "#" + JavaIntegerLiteralNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
        registry.bind(
                JavaFloatingPointLiteralNode.KIND,
                JavaExpressionProjection.class.getName()
                        + "#"
                        + JavaFloatingPointLiteralNode.KIND.name(),
                (node, context) -> context.expressionProjection().standard(node));
    }

    private Expression project(JavaExpressionNode node) {
        return context.configuration.expressions().apply(node, context);
    }

    private Expression standard(JavaExpressionNode node) {
        if (node instanceof JavaErrorExpressionNode) {
            return expression(node, Expression.Kind.UNKNOWN, "", node.span(), null, List.of());
        }
        if (node instanceof JavaNameExpressionNode name) {
            return expression(
                    node,
                    Expression.Kind.NAME,
                    JavaSourceSpelling.name(name.name()),
                    name.name().span(),
                    null,
                    List.of());
        }
        if (node instanceof JavaThisExpressionNode) {
            return expression(
                    node,
                    Expression.Kind.NAME,
                    JavaKeyword.THIS.spelling(),
                    node.span(),
                    null,
                    List.of());
        }
        if (node instanceof JavaSuperQualifierNode) {
            return expression(
                    node,
                    Expression.Kind.NAME,
                    JavaKeyword.SUPER.spelling(),
                    node.span(),
                    null,
                    List.of());
        }
        if (node instanceof JavaClassLiteralExpressionNode) {
            return expression(
                    node,
                    Expression.Kind.LITERAL,
                    JavaPlatformTypeNames.CLASS,
                    node.span(),
                    null,
                    List.of());
        }
        if (node instanceof JavaCastExpressionNode cast) {
            return expression(
                    node,
                    Expression.Kind.OPERATION,
                    JavaNavigationOperationNames.CAST_PREFIX + context.spelling.text(cast.type()),
                    cast.type().span(),
                    null,
                    List.of(child(cast.operand())));
        }
        if (node instanceof JavaConditionalExpressionNode conditional) {
            return expression(
                    node,
                    Expression.Kind.OPERATION,
                    JavaNavigationOperationNames.CONDITIONAL,
                    node.span(),
                    null,
                    List.of(
                            child(conditional.condition()),
                            child(conditional.thenExpression()),
                            child(conditional.elseExpression())));
        }
        if (node instanceof JavaInstanceOfExpressionNode instanceOf) {
            return expression(
                    node,
                    Expression.Kind.OPERATION,
                    JavaKeyword.INSTANCEOF.spelling(),
                    node.span(),
                    null,
                    List.of(child(instanceOf.expression())));
        }
        if (node instanceof JavaLiteralNode literal) {
            return expression(
                    node,
                    Expression.Kind.LITERAL,
                    literalType(literal),
                    node.span(),
                    null,
                    List.of());
        }
        if (node instanceof JavaFieldAccessExpressionNode field) {
            return expression(
                    node,
                    Expression.Kind.FIELD,
                    field.name().name(),
                    field.name().span(),
                    child(field.receiver()),
                    List.of());
        }
        if (node instanceof JavaMethodInvocationExpressionNode call) {
            return expression(
                    node,
                    Expression.Kind.CALL,
                    call.name().name(),
                    call.name().span(),
                    call.receiver().map(this::child).orElse(null),
                    call.arguments().stream().map(this::child).toList());
        }
        if (node instanceof JavaClassInstanceCreationExpressionNode creation) {
            return expression(
                    node,
                    Expression.Kind.NEW,
                    context.spelling.text(creation.type()),
                    creation.type().span(),
                    creation.qualifier().map(this::child).orElse(null),
                    creation.arguments().stream().map(this::child).toList());
        }
        if (node instanceof JavaMethodReferenceExpressionNode reference) {
            Expression receiver =
                    reference.qualifier() instanceof JavaExpressionNode expression
                            ? child(expression)
                            : new Expression(
                                    Expression.Kind.NAME,
                                    context.spelling.text(reference.qualifier()),
                                    reference.qualifier().span(),
                                    reference.qualifier().span(),
                                    null,
                                    List.of());
            Entry last =
                    context.source
                            .tokens()
                            .get(
                                    context.source.tokenIndexAtOrAfter(node.span().end().offset())
                                            - 1);
            SourceSpan nameSpan =
                    reference
                            .name()
                            .map(name -> name.span())
                            .orElseGet(
                                    () ->
                                            context.source.span(
                                                    last.offset(), last.offset() + last.length()));
            return expression(
                    node,
                    Expression.Kind.METHOD_REFERENCE,
                    reference.name().map(name -> name.name()).orElse(JavaKeyword.NEW.spelling()),
                    nameSpan,
                    receiver,
                    List.of());
        }
        if (node instanceof JavaAssignmentExpressionNode assignment) {
            return expression(
                    node,
                    Expression.Kind.OPERATION,
                    operators
                            .require(
                                    JavaOperatorCatalog.Kind.ASSIGNMENT,
                                    assignment.operator().name())
                            .spelling(),
                    node.span(),
                    null,
                    List.of(child(assignment.target()), child(assignment.value())));
        }
        if (node instanceof JavaArrayInitializerNode initializer) {
            return expression(
                    node,
                    Expression.Kind.OPERATION,
                    JavaNavigationOperationNames.ARRAY_INITIALIZER,
                    node.span(),
                    null,
                    initializer.elements().stream().map(this::child).toList());
        }
        if (node instanceof JavaArrayCreationExpressionNode creation) {
            List<Expression> arguments = new ArrayList<>();
            creation.dimensions()
                    .forEach(
                            dimension ->
                                    dimension
                                            .length()
                                            .ifPresent(length -> arguments.add(child(length))));
            creation.initializer().ifPresent(initializer -> arguments.add(child(initializer)));
            return expression(
                    node,
                    Expression.Kind.NEW,
                    context.spelling.text(creation.elementType())
                            + "[]".repeat(creation.dimensions().size()),
                    creation.elementType().span(),
                    null,
                    arguments);
        }
        if (node instanceof JavaArrayAccessExpressionNode access) {
            return expression(
                    node,
                    Expression.Kind.ARRAY_ACCESS,
                    JavaNavigationOperationNames.ARRAY_ACCESS,
                    node.span(),
                    child(access.receiver()),
                    List.of(child(access.index())));
        }
        if (node instanceof JavaPrefixExpressionNode prefix) {
            return expression(
                    node,
                    Expression.Kind.OPERATION,
                    operators
                            .require(JavaOperatorCatalog.Kind.PREFIX, prefix.operator().name())
                            .spelling(),
                    node.span(),
                    null,
                    List.of(child(prefix.operand())));
        }
        if (node instanceof JavaPostfixExpressionNode postfix) {
            return expression(
                    node,
                    Expression.Kind.OPERATION,
                    operators
                            .require(JavaOperatorCatalog.Kind.POSTFIX, postfix.operator().name())
                            .spelling(),
                    node.span(),
                    null,
                    List.of(child(postfix.operand())));
        }
        if (node instanceof JavaBinaryExpressionNode binary) {
            return expression(
                    node,
                    Expression.Kind.OPERATION,
                    operators
                            .require(JavaOperatorCatalog.Kind.BINARY, binary.operator().name())
                            .spelling(),
                    node.span(),
                    null,
                    List.of(child(binary.left()), child(binary.right())));
        }
        if (node instanceof JavaParenthesizedExpressionNode grouped) {
            Expression expression = child(grouped.expression());
            return expression(
                    node,
                    expression.kind(),
                    expression.name(),
                    expression.nameSpan(),
                    expression.receiver(),
                    expression.arguments());
        }
        if (node instanceof JavaLambdaExpressionNode lambda) {
            return expression(
                    node,
                    Expression.Kind.LAMBDA,
                    JavaNavigationOperationNames.LAMBDA,
                    node.span(),
                    null,
                    roots(lambda.body()));
        }
        if (node instanceof JavaSwitchExpressionNode selection) {
            List<Expression> branches = new ArrayList<>();
            branches.add(child(selection.selector()));
            branches.addAll(roots(selection.body()));
            return expression(
                    node,
                    Expression.Kind.OPERATION,
                    JavaKeyword.SWITCH.spelling(),
                    node.span(),
                    null,
                    branches);
        }
        throw new TranslationRefusedException(
                "The executable navigation view does not support " + node.schema().kind().name());
    }

    List<Expression> roots(Node root) {
        List<Expression> roots = new ArrayList<>();
        root.accept(
                new Visitor() {
                    @Override
                    public TraversalAction enter(Node node) {
                        if (node instanceof JavaExpressionNode expression) {
                            roots.add(child(expression));
                            return TraversalAction.SKIP_CHILDREN;
                        }
                        if (node instanceof JavaAnonymousClassBodyNode
                                || node instanceof JavaTypeDeclarationNode) {
                            return TraversalAction.SKIP_CHILDREN;
                        }
                        return TraversalAction.CONTINUE;
                    }
                });
        return List.copyOf(roots);
    }

    private static String literalType(JavaLiteralNode literal) {
        return switch (literal) {
            case JavaBooleanLiteralNode ignored -> JavaKeyword.BOOLEAN.spelling();
            case JavaCharacterLiteralNode ignored -> JavaKeyword.CHAR.spelling();
            case JavaStringLiteralNode ignored -> JavaPlatformTypeNames.STRING;
            case JavaTextBlockLiteralNode ignored -> JavaPlatformTypeNames.STRING;
            case JavaNullLiteralNode ignored -> JavaToken.NULL_LITERAL.spelling();
            case JavaIntegerLiteralNode integer ->
                    JavaKeyword.valueOf(integer.kind().name()).spelling();
            case JavaFloatingPointLiteralNode floating ->
                    floating.value() instanceof JavaFloatingLiteralValue.Binary32
                            ? JavaKeyword.FLOAT.spelling()
                            : JavaKeyword.DOUBLE.spelling();
            default ->
                    throw new TranslationRefusedException(
                            "The executable navigation view does not support literal "
                                    + literal.schema().kind().name());
        };
    }

    private static Expression expression(
            JavaExpressionNode node,
            Expression.Kind kind,
            String name,
            SourceSpan nameSpan,
            Expression receiver,
            List<Expression> arguments) {
        return new Expression(kind, name, nameSpan, node.span(), receiver, arguments);
    }
}
