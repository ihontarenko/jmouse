package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaSyntaxException;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.statement.JavaForStatementNode;

/** Basic-for initializers and updates admit statement expressions, not arbitrary expressions. */
public final class JavaForStatementParser implements ParseRule<JavaParsingInput, JavaForStatementNode> {
    @Override
    public JavaForStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect(JavaKeyword.FOR, session);
        input.expect("(", session);
        JavaForStatementNode node = new JavaForStatementNode(input.name(), input.point());
        if (!input.matches(";")) {
            JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
            try {
                node.addInitializer(JavaLocalVariableDeclarationParser.declaration(input, session));
            } catch (JavaSyntaxException exception) {
                input.restore(checkpoint);
                do {
                    JavaExpressionNode expression = session.parse(JavaParseRoles.EXPRESSION, input);
                    JavaExpressionStatementParser.requireStatementExpression(input, expression);
                    node.addInitializer(expression);
                } while (input.take(",", session));
            }
        }
        input.expect(";", session);
        if (!input.matches(";")) {
            node.setCondition(session.parse(JavaParseRoles.EXPRESSION, input));
        }
        input.expect(";", session);
        if (!input.matches(")")) {
            do {
                JavaExpressionNode expression = session.parse(JavaParseRoles.EXPRESSION, input);
                JavaExpressionStatementParser.requireStatementExpression(input, expression);
                node.addUpdate(expression);
            } while (input.take(",", session));
        }
        input.expect(")", session);
        node.setBody(session.parse(JavaStatementRoles.STATEMENT, input));
        return input.finish(node, start);
    }
}
