package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.parameter.JavaFormalParameterNode;
import org.jmouse.java.parser.node.statement.JavaCatchClauseNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.node.type.JavaUnionTypeNode;

/** A catch clause owns one exception parameter, including all multi-catch alternatives. */
public final class JavaCatchClauseParser implements ParseRule<JavaParsingInput, JavaCatchClauseNode> {
    @Override
    public JavaCatchClauseNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.CATCH, session);
        input.expect("(", session);
        int parameterStart = input.offset();
        JavaFormalParameterNode parameter = new JavaFormalParameterNode(input.name(), input.point());
        parameter.setModifiers(session.parse(JavaParseRoles.VARIABLE_MODIFIERS, input));
        JavaTypeSyntaxNode type = session.parse(JavaParseRoles.TYPE, input);
        if (input.take("|", session)) {
            JavaUnionTypeNode union = new JavaUnionTypeNode(input.name(), type.span());
            union.addType(type);
            do {
                union.addType(session.parse(JavaParseRoles.TYPE, input));
            } while (input.take("|", session));
            type = input.finish(union, type.span().start().offset());
        }
        parameter.setType(type);
        parameter.setName(input.identifier(session));
        input.finish(parameter, parameterStart);
        input.expect(")", session);
        JavaCatchClauseNode node = new JavaCatchClauseNode(input.name(), input.point());
        node.setParameter(parameter);
        node.setBody(session.parse(JavaStatementRoles.BLOCK, input));
        return input.finish(node, start);
    }
}
