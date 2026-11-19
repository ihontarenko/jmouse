package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaGrammarContext;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.statement.JavaSwitchStatementNode;

public final class JavaSwitchStatementParser implements ParseRule<JavaParsingInput, JavaSwitchStatementNode> {
    @Override
    public JavaSwitchStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.SWITCH, session);
        input.expect("(", session);
        JavaSwitchStatementNode node = new JavaSwitchStatementNode(input.name(), input.point());
        node.setSelector(session.parse(JavaParseRoles.EXPRESSION, input));
        input.expect(")", session);
        node.setBody(session.with(JavaGrammarContext.SWITCH_FORM, JavaGrammarContext.SwitchForm.STATEMENT,
                () -> session.parse(JavaStatementRoles.SWITCH_BLOCK, input)));
        return input.finish(node, start);
    }
}
