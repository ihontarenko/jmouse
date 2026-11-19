package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaAssertStatementNode;

/** Parses assert statement syntax. */
public final class JavaAssertStatementParser implements ParseRule<JavaParsingInput, JavaAssertStatementNode> {
    @Override
    public JavaAssertStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.ASSERT, session);
        JavaAssertStatementNode node = new JavaAssertStatementNode(input.name(), input.point());
        node.setCondition(session.parse(JavaParseRoles.EXPRESSION, input));
        if (input.take(":", session)) {
            node.setDetail(session.parse(JavaParseRoles.EXPRESSION, input));
        }
        input.expect(";", session);
        return input.finish(node, start);
    }
}
