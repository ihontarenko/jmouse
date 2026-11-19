package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaArgumentListParser;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaTypeParser;
import org.jmouse.java.parser.node.declaration.JavaEnumConstantNode;

public final class JavaEnumConstantParser implements ParseRule<JavaParsingInput, JavaEnumConstantNode> {
    @Override
    public JavaEnumConstantNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaEnumConstantNode node = new JavaEnumConstantNode(input.name(), input.point());
        JavaTypeParser.annotations(input, session).forEach(node::addAnnotation);
        node.setName(input.identifier(session));
        if (input.matches("(")) {
            JavaArgumentListParser.parse(input, session, node::addArgument);
        }
        if (input.matches("{")) {
            node.setBody(session.parse(JavaParseRoles.ANONYMOUS_CLASS_BODY, input));
        }
        return input.finish(node, start);
    }
}
