package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaTypeParser;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.parameter.JavaFormalParameterNode;
import org.jmouse.java.parser.node.statement.JavaEnhancedForStatementNode;

public final class JavaEnhancedForStatementParser implements ParseRule<JavaParsingInput, JavaEnhancedForStatementNode> {
    @Override
    public JavaEnhancedForStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.FOR, session);
        input.expect("(", session);
        int parameterStart = input.offset();
        JavaFormalParameterNode variable = new JavaFormalParameterNode(input.name(), input.point());
        variable.setModifiers(session.parse(JavaParseRoles.VARIABLE_MODIFIERS, input));
        variable.setType(session.parse(JavaParseRoles.TYPE, input));
        variable.setName(input.identifier(session));
        JavaTypeParser.dimensions(input, session).forEach(variable::addDimension);
        input.finish(variable, parameterStart);
        input.expect(":", session);
        JavaEnhancedForStatementNode node = new JavaEnhancedForStatementNode(input.name(), input.point());
        node.setVariable(variable);
        node.setIterable(session.parse(JavaParseRoles.EXPRESSION, input));
        input.expect(")", session);
        node.setBody(session.parse(JavaStatementRoles.STATEMENT, input));
        return input.finish(node, start);
    }
}
