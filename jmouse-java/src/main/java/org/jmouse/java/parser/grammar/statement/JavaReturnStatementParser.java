package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaReturnStatementNode;

/** Parses return statement syntax. */
public final class JavaReturnStatementParser implements ParseRule<JavaParsingInput, JavaReturnStatementNode> {
    @Override
    public JavaReturnStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.RETURN, session);
        JavaReturnStatementNode node = new JavaReturnStatementNode(input.name(), input.point());
        if (!input.matches(";")) {
            node.setExpression(session.parse(JavaParseRoles.EXPRESSION, input));
        }
        input.expect(";", session);
        return input.finish(node, start);
    }
}
