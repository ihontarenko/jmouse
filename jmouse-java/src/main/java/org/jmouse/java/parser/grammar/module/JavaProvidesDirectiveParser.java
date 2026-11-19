package org.jmouse.java.parser.grammar.module;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.module.JavaProvidesDirectiveNode;

public final class JavaProvidesDirectiveParser implements ParseRule<JavaParsingInput, JavaProvidesDirectiveNode> {
    @Override
    public JavaProvidesDirectiveNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.PROVIDES, session);
        JavaProvidesDirectiveNode node = new JavaProvidesDirectiveNode(input.name(), input.point());
        node.setService(JavaServiceTypeParser.parse(input, session));
        input.expect(JavaKeyword.WITH, session);
        do {
            node.addProvider(JavaServiceTypeParser.parse(input, session));
        } while (input.take(",", session));
        input.expect(";", session);
        return input.finish(node, start);
    }
}
