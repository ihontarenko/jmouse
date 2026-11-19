package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaTypeParser;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;

public final class JavaTypeParameterParser implements ParseRule<JavaParsingInput, JavaTypeParameterNode> {
    @Override
    public JavaTypeParameterNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaTypeParameterNode node = new JavaTypeParameterNode(input.name(), input.point());
        JavaTypeParser.annotations(input, session).forEach(node::addAnnotation);
        node.setName(input.typeIdentifier(session));
        if (input.take(JavaKeyword.EXTENDS, session)) {
            do {
                node.addBound(session.parse(JavaParseRoles.TYPE, input));
            } while (input.take("&", session));
        }
        return input.finish(node, start);
    }
}
