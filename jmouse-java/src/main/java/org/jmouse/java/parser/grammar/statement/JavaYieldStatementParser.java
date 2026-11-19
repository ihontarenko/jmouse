package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaYieldStatementNode;

/** Parses yield statement syntax. */
public final class JavaYieldStatementParser implements ParseRule<JavaParsingInput, JavaYieldStatementNode> {
    @Override
    public JavaYieldStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.YIELD, session);
        input.requireFeature(JavaSourceFeatures.SWITCH_EXPRESSIONS);
        JavaYieldStatementNode node = new JavaYieldStatementNode(input.name(), input.point());
        node.setExpression(session.parse(JavaParseRoles.EXPRESSION, input));
        input.expect(";", session);
        return input.finish(node, start);
    }
}
