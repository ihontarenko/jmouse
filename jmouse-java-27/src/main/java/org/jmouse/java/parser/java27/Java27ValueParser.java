package org.jmouse.java.parser.java27;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaToken;

public final class Java27ValueParser implements ParseRule<JavaParsingInput, Java27ValueNode> {
    @Override
    public Java27ValueNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.requireFeature(Java27Names.FEATURE);
        Java27ValueNode node = new Java27ValueNode(input.name(), input.point());
        input.expect(Java27Token.VALUE_PREFIX, session);
        node.setExpression(session.parse(JavaParseRoles.EXPRESSION, input));
        input.expect(JavaToken.RIGHT_PARENTHESIS, session);
        return input.finish(node, start);
    }
}
