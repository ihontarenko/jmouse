package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.statement.JavaLabeledStatementNode;

/** Parses labeled statement syntax. */
public final class JavaLabeledStatementParser implements ParseRule<JavaParsingInput, JavaLabeledStatementNode> {
    @Override
    public JavaLabeledStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaLabeledStatementNode node = new JavaLabeledStatementNode(input.name(), input.point());
        node.setLabel(input.identifier(session));
        input.expect(":", session);
        node.setStatement(session.parse(JavaStatementRoles.STATEMENT, input));
        return input.finish(node, start);
    }
}
