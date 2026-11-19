package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaDoWhileStatementNode;

/** Parses do while statement syntax. */
public final class JavaDoWhileStatementParser implements ParseRule<JavaParsingInput, JavaDoWhileStatementNode> {
    @Override
    public JavaDoWhileStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.DO, session);
        JavaDoWhileStatementNode node = new JavaDoWhileStatementNode(input.name(), input.point());
        node.setBody(session.parse(JavaStatementRoles.STATEMENT, input));
        input.expect(JavaKeyword.WHILE, session);
        input.expect("(", session);
        node.setCondition(session.parse(JavaParseRoles.EXPRESSION, input));
        input.expect(")", session);
        input.expect(";", session);
        return input.finish(node, start);
    }
}
