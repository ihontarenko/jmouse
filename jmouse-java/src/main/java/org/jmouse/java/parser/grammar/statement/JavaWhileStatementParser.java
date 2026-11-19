package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaWhileStatementNode;

/** Parses while statement syntax. */
public final class JavaWhileStatementParser implements ParseRule<JavaParsingInput, JavaWhileStatementNode> {
    @Override
    public JavaWhileStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.WHILE, session);
        JavaWhileStatementNode node = new JavaWhileStatementNode(input.name(), input.point());
        input.expect("(", session);
        node.setCondition(session.parse(JavaParseRoles.EXPRESSION, input));
        input.expect(")", session);
        node.setBody(session.parse(JavaStatementRoles.STATEMENT, input));
        return input.finish(node, start);
    }
}
