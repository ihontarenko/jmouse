package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaBreakStatementNode;

/** Parses break statement syntax. */
public final class JavaBreakStatementParser implements ParseRule<JavaParsingInput, JavaBreakStatementNode> {
    @Override
    public JavaBreakStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.BREAK, session);
        JavaBreakStatementNode node = new JavaBreakStatementNode(input.name(), input.point());
        if (!input.matches(";")) {
            node.setLabel(input.identifier(session));
        }
        input.expect(";", session);
        return input.finish(node, start);
    }
}
