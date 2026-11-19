package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.node.expression.JavaArrayInitializerNode;

public final class JavaArrayInitializerParser implements ParseRule<JavaParsingInput, JavaArrayInitializerNode> {
    @Override
    public JavaArrayInitializerNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect("{", session);
        JavaArrayInitializerNode node = new JavaArrayInitializerNode(input.name(), input.point());
        if (!input.take("}", session)) {
            do {
                node.addElement(session.parse(JavaParseRoles.VARIABLE_INITIALIZER, input));
            } while (input.take(",", session) && !input.matches("}"));
            input.expect("}", session);
        }
        return input.finish(node, start);
    }
}
