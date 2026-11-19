package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaTryStatementNode;

/** Structured resource specification, protected block, catches and finally. */
public final class JavaTryStatementParser implements ParseRule<JavaParsingInput, JavaTryStatementNode> {
    @Override
    public JavaTryStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.TRY, session);
        JavaTryStatementNode node = new JavaTryStatementNode(input.name(), input.point());
        if (input.take("(", session)) {
            do {
                node.addResource(session.parse(JavaStatementRoles.RESOURCE, input));
            } while (input.take(";", session) && !input.matches(")"));
            input.expect(")", session);
        }
        node.setBody(session.parse(JavaStatementRoles.BLOCK, input));
        while (input.matches(JavaKeyword.CATCH)) {
            node.addCatchClause(session.parse(JavaStatementRoles.CATCH_CLAUSE, input));
        }
        if (input.matches(JavaKeyword.FINALLY)) {
            node.setFinallyClause(session.parse(JavaStatementRoles.FINALLY_CLAUSE, input));
        }
        if (node.resources().isEmpty() && node.catches().isEmpty() && node.finallyClause().isEmpty()) {
            throw input.error("try requires resources, a catch clause or a finally clause");
        }
        return input.finish(node, start);
    }
}
