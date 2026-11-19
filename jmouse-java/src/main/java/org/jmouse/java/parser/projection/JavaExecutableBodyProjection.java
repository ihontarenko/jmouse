package org.jmouse.java.parser.projection;

import org.jmouse.el.node.Node;
import org.jmouse.el.node.TraversalAction;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.java.parser.model.ExecutableBody;
import org.jmouse.java.parser.model.Expression;
import org.jmouse.java.parser.model.LocalBinding;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.body.JavaAnonymousClassBodyNode;
import org.jmouse.java.parser.node.declaration.JavaRecordComponentNode;
import org.jmouse.java.parser.node.declaration.JavaTypeDeclarationNode;
import org.jmouse.java.parser.node.declaration.JavaVariableDeclaratorNode;
import org.jmouse.java.parser.node.expression.JavaConstructorInvocationNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaLambdaExpressionNode;
import org.jmouse.java.parser.node.parameter.JavaFormalParameterNode;
import org.jmouse.java.parser.node.parameter.JavaInferredLambdaParameterNode;
import org.jmouse.java.parser.node.parameter.JavaParameterKind;
import org.jmouse.java.parser.node.parameter.JavaParameterNode;
import org.jmouse.java.parser.node.parameter.JavaVariableArityParameterNode;
import org.jmouse.java.parser.node.resource.JavaResourceVariableDeclarationNode;
import org.jmouse.java.parser.node.statement.JavaBlockNode;
import org.jmouse.java.parser.node.statement.JavaCatchClauseNode;
import org.jmouse.java.parser.node.statement.JavaEnhancedForStatementNode;
import org.jmouse.java.parser.node.statement.JavaForStatementNode;
import org.jmouse.java.parser.node.statement.JavaLocalVariableDeclarationStatementNode;
import org.jmouse.java.parser.node.statement.JavaTryStatementNode;
import org.jmouse.java.parser.node.type.JavaInferredTypeNode;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Per-body navigation projection and lexical bindings, using the shared jME traversal. */
final class JavaExecutableBodyProjection {
    private final JavaProjectionContext context;
    private final JavaExpressionProjection projection;
    private final JavaBodyReferenceProjection references = new JavaBodyReferenceProjection();

    JavaExecutableBodyProjection(JavaProjectionContext context) {
        this.context = context;
        projection = new JavaExpressionProjection(context);
    }

    ExecutableBody read(
            JavaSyntaxNode body,
            SymbolId owner,
            SymbolId type,
            List<? extends JavaSyntaxNode> parameters,
            SourceSpan span) {
        return readNodes(List.of(body), owner, type, parameters, span);
    }

    ExecutableBody arguments(
            List<JavaExpressionNode> arguments, SymbolId owner, SymbolId type, SourceSpan span) {
        return readNodes(arguments, owner, type, List.of(), span);
    }

    private ExecutableBody readNodes(
            List<? extends JavaSyntaxNode> roots,
            SymbolId owner,
            SymbolId type,
            List<? extends JavaSyntaxNode> parameters,
            SourceSpan span) {
        List<Expression> expressions = new ArrayList<>();
        List<LocalBinding> variables = new ArrayList<>();
        for (JavaSyntaxNode parameter : parameters) {
            variables.add(parameter(parameter, span));
        }
        Visitor visitor =
                new Visitor() {
                    private final Deque<Node> activeExpressions = new ArrayDeque<>();

                    @Override
                    public TraversalAction enter(Node node) {
                        if (node instanceof JavaTypeDeclarationNode
                                || node instanceof JavaAnonymousClassBodyNode) {
                            return TraversalAction.SKIP_CHILDREN;
                        }
                        if (node instanceof JavaParameterNode parameter) {
                            variables.add(parameter(parameter, localScope(parameter, span)));
                        }
                        if (node instanceof JavaResourceVariableDeclarationNode resource) {
                            String declared =
                                    resource.type() instanceof JavaInferredTypeNode
                                            ? null
                                            : context.spelling.text(resource.type());
                            variables.add(
                                    new LocalBinding(
                                            resource.name().name(),
                                            declared,
                                            resource.name().span(),
                                            resourceScope(resource),
                                            projection.read(resource.initializer())));
                        }
                        if (node instanceof JavaLocalVariableDeclarationStatementNode local) {
                            SourceSpan scope = localScope(local, span);
                            String declared =
                                    local.type() instanceof JavaInferredTypeNode
                                            ? null
                                            : context.spelling.text(local.type());
                            for (JavaVariableDeclaratorNode variable : local.variables()) {
                                String variableType =
                                        declared == null
                                                ? null
                                                : declared
                                                        + context.spelling.dimensions(
                                                                variable.dimensions());
                                variables.add(
                                        new LocalBinding(
                                                variable.name().name(),
                                                variableType,
                                                variable.name().span(),
                                                scope,
                                                variable.initializer()
                                                        .map(projection::read)
                                                        .orElse(null)));
                            }
                        }
                        if (node instanceof JavaExpressionNode expression) {
                            if (activeExpressions.isEmpty()) {
                                expressions.add(projection.read(expression));
                            }
                            activeExpressions.push(expression);
                        }
                        if (node instanceof JavaConstructorInvocationNode invocation) {
                            expressions.add(projection.constructor(invocation));
                            activeExpressions.push(invocation);
                        }
                        return TraversalAction.CONTINUE;
                    }

                    @Override
                    public void leave(Node node) {
                        if (node instanceof JavaExpressionNode
                                || node instanceof JavaConstructorInvocationNode) {
                            activeExpressions.pop();
                        }
                    }
                };
        roots.forEach(root -> root.accept(visitor));
        return new ExecutableBody(
                owner,
                type,
                span,
                expressions,
                variables,
                references.references(expressions, owner, type));
    }

    private LocalBinding parameter(JavaSyntaxNode parameter, SourceSpan scope) {
        if (parameter instanceof JavaFormalParameterNode formal) {
            String type =
                    formal.type() instanceof JavaInferredTypeNode
                            ? null
                            : context.spelling.text(formal.type())
                                    + context.spelling.dimensions(formal.dimensions());
            return new LocalBinding(formal.name().name(), type, formal.name().span(), scope, null);
        }
        if (parameter instanceof JavaVariableArityParameterNode variable) {
            return new LocalBinding(
                    variable.name().name(),
                    context.spelling.text(variable.type())
                            + "[]"
                            + context.spelling.dimensions(variable.dimensions()),
                    variable.name().span(),
                    scope,
                    null);
        }
        if (parameter instanceof JavaInferredLambdaParameterNode inferred) {
            return new LocalBinding(
                    inferred.name().name(), null, inferred.name().span(), scope, null);
        }
        if (parameter instanceof JavaRecordComponentNode component) {
            return new LocalBinding(
                    component.name().name(),
                    context.spelling.text(component.type())
                            + (component.kind() == JavaParameterKind.VARIABLE_ARITY ? "[]" : ""),
                    component.name().span(),
                    scope,
                    null);
        }
        throw new TranslationRefusedException(
                "Unsupported lexical parameter: " + parameter.schema().kind().name());
    }

    private static SourceSpan localScope(Node node, SourceSpan fallback) {
        for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof JavaLambdaExpressionNode lambda) {
                return lambda.body().span();
            }
            if (parent instanceof JavaCatchClauseNode clause) {
                return clause.body().span();
            }
            if (parent instanceof JavaBlockNode block) {
                return block.span();
            }
            if (parent instanceof JavaForStatementNode loop) {
                return loop.span();
            }
            if (parent instanceof JavaEnhancedForStatementNode loop) {
                return loop.body().span();
            }
        }
        return fallback;
    }

    private SourceSpan resourceScope(JavaResourceVariableDeclarationNode resource) {
        for (Node parent = resource.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof JavaTryStatementNode statement) {
                return context.source.span(
                        resource.name().span().start().offset(),
                        statement.body().span().end().offset());
            }
        }
        throw new TranslationRefusedException("A resource declaration requires its try statement");
    }
}
