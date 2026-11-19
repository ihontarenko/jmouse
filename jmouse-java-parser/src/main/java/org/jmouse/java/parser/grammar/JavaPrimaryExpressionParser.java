package org.jmouse.java.parser.grammar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor.Savepoint;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.expression.JavaArrayAccessExpressionNode;
import org.jmouse.java.parser.node.expression.JavaClassInstanceCreationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaClassLiteralExpressionNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaFieldAccessExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodInvocationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodReferenceExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodReferenceKind;
import org.jmouse.java.parser.node.expression.JavaNameExpressionNode;
import org.jmouse.java.parser.node.expression.JavaParenthesizedExpressionNode;
import org.jmouse.java.parser.node.expression.JavaPostfixExpressionNode;
import org.jmouse.java.parser.node.expression.JavaSuperQualifierNode;
import org.jmouse.java.parser.node.expression.JavaThisExpressionNode;
import org.jmouse.java.parser.node.expression.JavaUnaryOperator;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

/** Primary syntax and selectors; no precedence climbing or type resolution is duplicated here. */
public final class JavaPrimaryExpressionParser implements ParseRule<JavaParsingInput, JavaExpressionNode> {
    private final JavaOperatorCatalog operators;

    public JavaPrimaryExpressionParser(JavaOperatorCatalog operators) {
        this.operators = operators;
    }

    @Override
    public JavaExpressionNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        if (session.context(JavaGrammarContext.EXPRESSION_FORM) != JavaGrammarContext.ExpressionForm.CONDITIONAL
                && lambdaAhead(input, session)) {
            return session.parse(JavaParseRoles.LAMBDA, input);
        }
        if (input.matches("(")) {
            JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
            try {
                return session.parse(JavaParseRoles.CAST, input);
            } catch (JavaSyntaxException exception) {
                input.restore(checkpoint);
            }
        }
        JavaExpressionNode expression = typePrimary(input, session);
        if (expression != null) {
            return selectors(input, session, expression, start);
        }
        if (input.take("(", session)) {
            JavaParenthesizedExpressionNode node = new JavaParenthesizedExpressionNode(input.name(), input.point());
            node.setExpression(session.parse(JavaParseRoles.EXPRESSION, input));
            input.expect(")", session);
            expression = input.finish(node, start);
        } else if (input.matches(JavaKeyword.NEW)) {
            expression = session.parse(JavaParseRoles.CREATION, input);
        } else if (input.matches(JavaKeyword.SWITCH)) {
            expression = session.parse(JavaParseRoles.SWITCH_EXPRESSION, input);
        } else if (input.take(JavaKeyword.THIS, session)) {
            expression = input.finish(new JavaThisExpressionNode(input.name(), input.point()), start);
        } else if (input.take(JavaKeyword.SUPER, session)) {
            expression = input.finish(new JavaSuperQualifierNode(input.name(), input.point()), start);
        } else if (input.matches("<")) {
            JavaMethodInvocationExpressionNode call = new JavaMethodInvocationExpressionNode(input.name(), input.point());
            JavaTypeParser.explicitArguments(input, session, call::addTypeArgument);
            call.setName(input.identifier(session));
            JavaArgumentListParser.parse(input, session, call::addArgument);
            expression = input.finish(call, start);
        } else if (JavaToken.IDENTIFIER.matches(input.token())) {
            JavaQualifiedNameNode name = new JavaQualifiedNameNode(input.name(), input.point());
            name.addSegment(input.identifier(session));
            input.finish(name, start);
            JavaNameExpressionNode node = new JavaNameExpressionNode(input.name(), name.span());
            node.setName(name);
            expression = node;
        } else {
            expression = session.parse(JavaParseRoles.LITERAL, input);
        }
        return selectors(input, session, expression, start);
    }

    private JavaExpressionNode typePrimary(JavaParsingInput input, ParseSession session) {
        if (!JavaToken.IDENTIFIER.matches(input.token()) && JavaTypeParser.primitive(input.token().value()) == null
                && !input.matches(JavaKeyword.VOID)) {
            return null;
        }
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        int start = input.offset();
        try {
            JavaTypeSyntaxNode type = session.parse(JavaParseRoles.TYPE, input);
            if (input.matches(".") && input.cursor().lookAt(1).value().equals(JavaKeyword.CLASS.spelling())) {
                input.expect(".", session);
                input.expect(JavaKeyword.CLASS, session);
                JavaClassLiteralExpressionNode node = new JavaClassLiteralExpressionNode(input.name(), input.point());
                node.setType(type);
                return input.finish(node, start);
            }
            if (input.matches("::")) {
                return reference(input, session, type, start);
            }
        } catch (JavaSyntaxException exception) {
            // This alternative may instead be a relational expression or an ordinary name.
        }
        input.restore(checkpoint);
        return null;
    }

    private JavaExpressionNode selectors(JavaParsingInput input, ParseSession session,
                                          JavaExpressionNode expression, int start) {
        while (true) {
            if (input.take(".", session)) {
                if (input.take(JavaKeyword.THIS, session)) {
                    JavaThisExpressionNode node = new JavaThisExpressionNode(input.name(), input.point());
                    node.setQualifier(qualifierName(input, expression));
                    expression = input.finish(node, start);
                    continue;
                }
                if (input.take(JavaKeyword.SUPER, session)) {
                    JavaSuperQualifierNode node = new JavaSuperQualifierNode(input.name(), input.point());
                    node.setQualifier(qualifierName(input, expression));
                    expression = input.finish(node, start);
                    continue;
                }
                if (input.matches(JavaKeyword.NEW)) {
                    JavaExpressionNode creation = session.parse(JavaParseRoles.CREATION, input);
                    if (!(creation instanceof JavaClassInstanceCreationExpressionNode instance)) {
                        throw input.error("A qualified creation must create a class instance");
                    }
                    instance.setQualifier(expression);
                    expression = input.finish(instance, start);
                    continue;
                }
                List<JavaTypeSyntaxNode> arguments = new ArrayList<>();
                JavaTypeParser.explicitArguments(input, session, arguments::add);
                JavaIdentifierNode name = input.identifier(session);
                if (input.matches("(")) {
                    JavaMethodInvocationExpressionNode call =
                            new JavaMethodInvocationExpressionNode(input.name(), input.point());
                    call.setReceiver(expression);
                    arguments.forEach(call::addTypeArgument);
                    call.setName(name);
                    JavaArgumentListParser.parse(input, session, call::addArgument);
                    expression = input.finish(call, start);
                } else {
                    if (!arguments.isEmpty()) {
                        throw input.error("Explicit type arguments require a method invocation");
                    }
                    JavaFieldAccessExpressionNode field = new JavaFieldAccessExpressionNode(input.name(), input.point());
                    field.setReceiver(expression);
                    field.setName(name);
                    expression = input.finish(field, start);
                }
            } else if (input.matches("(")) {
                if (!(expression instanceof JavaNameExpressionNode name)) {
                    throw input.error("Only a method name or member selection may be invoked");
                }
                JavaMethodInvocationExpressionNode call =
                        new JavaMethodInvocationExpressionNode(input.name(), input.point());
                JavaQualifiedNameNode qualified = name.removeChild(JavaNameExpressionNode.NAME, 0);
                call.setName(qualified.removeChild(JavaQualifiedNameNode.SEGMENTS, 0));
                JavaArgumentListParser.parse(input, session, call::addArgument);
                expression = input.finish(call, start);
            } else if (input.take("[", session)) {
                JavaArrayAccessExpressionNode access = new JavaArrayAccessExpressionNode(input.name(), input.point());
                access.setReceiver(expression);
                access.setIndex(session.parse(JavaParseRoles.EXPRESSION, input));
                input.expect("]", session);
                expression = input.finish(access, start);
            } else if (input.matches("::")) {
                return reference(input, session, expression, start);
            } else {
                JavaOperatorCatalog.Definition postfix = operators.find(input, Set.of(JavaOperatorCatalog.Kind.POSTFIX));
                if (postfix == null) {
                    return expression;
                }
                input.expect(postfix.spelling(), session);
                JavaPostfixExpressionNode node = new JavaPostfixExpressionNode(input.name(), input.point());
                node.setOperator(JavaUnaryOperator.valueOf(postfix.identity()));
                node.setOperand(expression);
                expression = input.finish(node, start);
            }
        }
    }

    private JavaMethodReferenceExpressionNode reference(JavaParsingInput input, ParseSession session,
                                                         JavaSyntaxNode qualifier, int start) {
        input.expect("::", session);
        JavaMethodReferenceExpressionNode node = new JavaMethodReferenceExpressionNode(input.name(), input.point());
        node.setQualifier(qualifier);
        JavaTypeParser.explicitArguments(input, session, node::addTypeArgument);
        if (input.take(JavaKeyword.NEW, session)) {
            node.setKind(JavaMethodReferenceKind.CONSTRUCTOR);
        } else {
            node.setKind(JavaMethodReferenceKind.METHOD);
            node.setName(input.identifier(session));
        }
        return input.finish(node, start);
    }

    private JavaQualifiedNameNode qualifierName(JavaParsingInput input, JavaExpressionNode expression) {
        JavaQualifiedNameNode result = new JavaQualifiedNameNode(input.name(), expression.span());
        Deque<JavaIdentifierNode> segments = new ArrayDeque<>();
        while (expression instanceof JavaFieldAccessExpressionNode field) {
            segments.addFirst(field.removeChild(JavaFieldAccessExpressionNode.NAME, 0));
            expression = field.removeChild(JavaFieldAccessExpressionNode.RECEIVER, 0);
        }
        if (!(expression instanceof JavaNameExpressionNode name)) {
            throw input.error("A qualified this or super requires a type name");
        }
        JavaQualifiedNameNode first = name.removeChild(JavaNameExpressionNode.NAME, 0);
        while (!first.segments().isEmpty()) {
            result.addSegment(first.removeChild(JavaQualifiedNameNode.SEGMENTS, 0));
        }
        segments.forEach(result::addSegment);
        return result;
    }

    private boolean lambdaAhead(JavaParsingInput input, ParseSession session) {
        if (JavaToken.IDENTIFIER.matches(input.token())) {
            return input.cursor().lookAt(1).value().equals("-")
                    && input.cursor().lookAt(2).value().equals(">")
                    && input.source().endOffset(input.cursor().lookAt(1)) == input.cursor().lookAt(2).offset();
        }
        if (!input.matches("(")) {
            return false;
        }
        int depth = 0;
        for (int lookahead = 0; lookahead < input.cursor().remaining(); lookahead++) {
            session.step();
            Token token = input.cursor().lookAt(lookahead);
            if (token.value().equals("(")) {
                depth++;
            } else if (token.value().equals(")")) {
                depth--;
                if (depth == 0) {
                    Token minus = input.cursor().lookAt(lookahead + 1);
                    Token greater = input.cursor().lookAt(lookahead + 2);
                    return minus.value().equals("-") && greater.value().equals(">")
                            && input.source().endOffset(minus) == greater.offset();
                }
            }
        }
        return false;
    }
}
