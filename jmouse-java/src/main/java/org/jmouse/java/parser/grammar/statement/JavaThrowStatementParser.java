package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaThrowStatementNode;

/** Parses throw statement syntax. */
public final class JavaThrowStatementParser implements ParseRule<JavaParsingInput, JavaThrowStatementNode> {
    @Override
    public JavaThrowStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.THROW, session);
        JavaThrowStatementNode node = new JavaThrowStatementNode(input.name(), input.point());
        node.setExpression(session.parse(JavaParseRoles.EXPRESSION, input));
        input.expect(";", session);
        return input.finish(node, start);
    }
}
