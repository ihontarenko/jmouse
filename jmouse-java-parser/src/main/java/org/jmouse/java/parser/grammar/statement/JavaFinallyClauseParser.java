package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaFinallyClauseNode;

public final class JavaFinallyClauseParser implements ParseRule<JavaParsingInput, JavaFinallyClauseNode> {
    @Override
    public JavaFinallyClauseNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.FINALLY, session);
        JavaFinallyClauseNode node = new JavaFinallyClauseNode(input.name(), input.point());
        node.setBody(session.parse(JavaStatementRoles.BLOCK, input));
        return input.finish(node, start);
    }
}
