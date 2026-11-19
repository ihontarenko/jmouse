package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.parameter.JavaReceiverParameterNode;

public final class JavaReceiverParameterParser implements ParseRule<JavaParsingInput, JavaReceiverParameterNode> {
    @Override
    public JavaReceiverParameterNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaReceiverParameterNode node = new JavaReceiverParameterNode(input.name(), input.point());
        JavaTypeParser.annotations(input, session).forEach(node::addAnnotation);
        node.setType(session.parse(JavaParseRoles.TYPE, input));
        if (!input.matches(JavaKeyword.THIS)) {
            node.setQualifier(session.parse(JavaParseRoles.QUALIFIED_NAME, input));
            input.expect(".", session);
        }
        input.expect(JavaKeyword.THIS, session);
        return input.finish(node, start);
    }
}
