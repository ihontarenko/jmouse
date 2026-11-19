package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaContinueStatementNode;

/** Parses continue statement syntax. */
public final class JavaContinueStatementParser implements ParseRule<JavaParsingInput, JavaContinueStatementNode> {
    @Override
    public JavaContinueStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.CONTINUE, session);
        JavaContinueStatementNode node = new JavaContinueStatementNode(input.name(), input.point());
        if (!input.matches(";")) {
            node.setLabel(input.identifier(session));
        }
        input.expect(";", session);
        return input.finish(node, start);
    }
}
