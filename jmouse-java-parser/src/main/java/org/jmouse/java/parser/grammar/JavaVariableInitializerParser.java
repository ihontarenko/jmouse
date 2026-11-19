package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;

public final class JavaVariableInitializerParser implements ParseRule<JavaParsingInput, JavaExpressionNode> {
    @Override
    public JavaExpressionNode parse(JavaParsingInput input, ParseSession session) {
        return input.matches("{") ? session.parse(JavaParseRoles.ARRAY_INITIALIZER, input)
                : session.parse(JavaParseRoles.EXPRESSION, input);
    }
}
