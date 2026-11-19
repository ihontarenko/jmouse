package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.node.expression.JavaCastExpressionNode;
import org.jmouse.java.parser.node.type.JavaIntersectionTypeNode;
import org.jmouse.java.parser.node.type.JavaPrimitiveTypeNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

/** Cast grammar is separate from the parenthesized-expression alternative. */
public final class JavaCastExpressionParser
        implements ParseRule<JavaParsingInput, JavaCastExpressionNode> {
    @Override
    public JavaCastExpressionNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect("(", session);
        JavaTypeSyntaxNode type = session.parse(JavaParseRoles.TYPE, input);
        if (input.take("&", session)) {
            JavaIntersectionTypeNode intersection =
                    new JavaIntersectionTypeNode(input.name(), type.span());
            intersection.addType(type);
            do {
                intersection.addType(session.parse(JavaParseRoles.TYPE, input));
            } while (input.take("&", session));
            type = input.finish(intersection, type.span().start().offset());
        }
        input.expect(")", session);
        if (!(type instanceof JavaPrimitiveTypeNode)
                && (input.matches("+") || input.matches("-"))) {
            throw input.error(
                    "A reference cast requires a unary expression other than plus or minus");
        }
        JavaCastExpressionNode node = new JavaCastExpressionNode(input.name(), input.point());
        node.setType(type);
        node.setOperand(
                !(type instanceof JavaPrimitiveTypeNode)
                                && JavaPrimaryExpressionParser.lambdaAhead(input, session)
                        ? session.parse(JavaParseRoles.LAMBDA, input)
                        : session.parse(JavaParseRoles.UNARY_EXPRESSION, input));
        return input.finish(node, start);
    }
}
