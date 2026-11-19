package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.statement.JavaEmptyStatementNode;

/** Parses empty statement syntax. */
public final class JavaEmptyStatementParser implements ParseRule<JavaParsingInput, JavaEmptyStatementNode> {
    @Override
    public JavaEmptyStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(";", session);
        return new JavaEmptyStatementNode(input.name(), input.spanFrom(start));
    }
}
