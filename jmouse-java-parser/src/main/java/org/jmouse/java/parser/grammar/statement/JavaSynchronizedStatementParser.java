package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaSynchronizedStatementNode;

/** Parses synchronized statement syntax. */
public final class JavaSynchronizedStatementParser implements ParseRule<JavaParsingInput, JavaSynchronizedStatementNode> {
    @Override
    public JavaSynchronizedStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.SYNCHRONIZED, session);
        JavaSynchronizedStatementNode node = new JavaSynchronizedStatementNode(input.name(), input.point());
        input.expect("(", session);
        node.setMonitor(session.parse(JavaParseRoles.EXPRESSION, input));
        input.expect(")", session);
        node.setBody(session.parse(JavaStatementRoles.BLOCK, input));
        return input.finish(node, start);
    }
}
