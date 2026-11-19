package org.jmouse.java.parser.grammar.module;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.module.JavaRequiresDirectiveNode;

public final class JavaRequiresDirectiveParser implements ParseRule<JavaParsingInput, JavaRequiresDirectiveNode> {
    @Override
    public JavaRequiresDirectiveNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.REQUIRES, session);
        JavaRequiresDirectiveNode node = new JavaRequiresDirectiveNode(input.name(), input.point());
        node.setModifiers(session.parse(JavaModuleRoles.REQUIRES_MODIFIERS, input));
        node.setName(session.parse(JavaParseRoles.QUALIFIED_NAME, input));
        input.expect(";", session);
        return input.finish(node, start);
    }
}
