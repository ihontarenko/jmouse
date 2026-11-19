package org.jmouse.java.parser.grammar.module;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.module.JavaUsesDirectiveNode;

public final class JavaUsesDirectiveParser implements ParseRule<JavaParsingInput, JavaUsesDirectiveNode> {
    @Override
    public JavaUsesDirectiveNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.USES, session);
        JavaUsesDirectiveNode node = new JavaUsesDirectiveNode(input.name(), input.point());
        node.setService(JavaServiceTypeParser.parse(input, session));
        input.expect(";", session);
        return input.finish(node, start);
    }
}
