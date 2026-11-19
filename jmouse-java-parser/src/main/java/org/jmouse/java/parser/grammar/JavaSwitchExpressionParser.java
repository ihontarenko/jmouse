package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.grammar.statement.JavaStatementRoles;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.expression.JavaSwitchExpressionNode;

public final class JavaSwitchExpressionParser implements ParseRule<JavaParsingInput, JavaSwitchExpressionNode> {
    @Override
    public JavaSwitchExpressionNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.requireFeature(JavaSourceFeatures.SWITCH_EXPRESSIONS);
        input.expect(JavaKeyword.SWITCH, session);
        input.expect("(", session);
        JavaSwitchExpressionNode node = new JavaSwitchExpressionNode(input.name(), input.point());
        node.setSelector(session.parse(JavaParseRoles.EXPRESSION, input));
        input.expect(")", session);
        node.setBody(session.with(JavaGrammarContext.SWITCH_FORM, JavaGrammarContext.SwitchForm.EXPRESSION,
                () -> session.parse(JavaStatementRoles.SWITCH_BLOCK, input)));
        return input.finish(node, start);
    }
}
