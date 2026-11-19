package org.jmouse.java.parser.java27;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaToken;

public final class Java27InspectParser implements ParseRule<JavaParsingInput, Java27InspectNode> {
    @Override
    public Java27InspectNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.requireFeature(Java27Names.FEATURE);
        Java27InspectNode node = new Java27InspectNode(input.name(), input.point());
        input.expect(Java27Keyword.INSPECT, session);
        node.setExpression(session.parse(JavaParseRoles.EXPRESSION, input));
        input.expect(JavaToken.SEMICOLON, session);
        return input.finish(node, start);
    }
}
