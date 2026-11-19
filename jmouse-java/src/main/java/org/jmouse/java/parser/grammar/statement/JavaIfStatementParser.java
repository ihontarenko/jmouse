package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaIfStatementNode;

/** Parses if statement syntax. */
public final class JavaIfStatementParser implements ParseRule<JavaParsingInput, JavaIfStatementNode> {
    @Override
    public JavaIfStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.IF, session);
        JavaIfStatementNode node = new JavaIfStatementNode(input.name(), input.point());
        input.expect("(", session);
        node.setCondition(session.parse(JavaParseRoles.EXPRESSION, input));
        input.expect(")", session);
        node.setThenStatement(session.parse(JavaStatementRoles.STATEMENT, input));
        if (input.take(JavaKeyword.ELSE, session)) {
            node.setElseStatement(session.parse(JavaStatementRoles.STATEMENT, input));
        }
        return input.finish(node, start);
    }
}
