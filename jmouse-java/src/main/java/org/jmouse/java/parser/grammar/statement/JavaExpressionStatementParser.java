package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.expression.JavaAssignmentExpressionNode;
import org.jmouse.java.parser.node.expression.JavaClassInstanceCreationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.expression.JavaMethodInvocationExpressionNode;
import org.jmouse.java.parser.node.expression.JavaPostfixExpressionNode;
import org.jmouse.java.parser.node.expression.JavaPrefixExpressionNode;
import org.jmouse.java.parser.node.expression.JavaUnaryOperator;
import org.jmouse.java.parser.node.statement.JavaExpressionStatementNode;

/** Only Java statement expressions may stand before a semicolon. */
public final class JavaExpressionStatementParser implements ParseRule<JavaParsingInput, JavaExpressionStatementNode> {
    @Override
    public JavaExpressionStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaExpressionNode expression = session.parse(JavaParseRoles.EXPRESSION, input);
        requireStatementExpression(input, expression);
        input.expect(";", session);
        JavaExpressionStatementNode node = new JavaExpressionStatementNode(input.name(), input.spanFrom(start));
        node.setExpression(expression);
        return node;
    }

    public static void requireStatementExpression(JavaParsingInput input, JavaExpressionNode expression) {
        if (expression instanceof JavaAssignmentExpressionNode
                || expression instanceof JavaMethodInvocationExpressionNode
                || expression instanceof JavaClassInstanceCreationExpressionNode
                || expression instanceof JavaPostfixExpressionNode
                || expression instanceof JavaPrefixExpressionNode prefix
                && (prefix.operator() == JavaUnaryOperator.INCREMENT || prefix.operator() == JavaUnaryOperator.DECREMENT)) {
            return;
        }
        throw input.errorAt("This expression cannot be used as a Java statement", expression.span());
    }
}
