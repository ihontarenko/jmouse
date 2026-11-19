package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaTypeParser;
import org.jmouse.java.parser.node.declaration.JavaVariableDeclaratorNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.statement.JavaLocalVariableDeclarationStatementNode;

/** Variable declarators are also reused by basic-for initializers. */
public final class JavaLocalVariableDeclarationParser
        implements ParseRule<JavaParsingInput, JavaLocalVariableDeclarationStatementNode> {
    @Override
    public JavaLocalVariableDeclarationStatementNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaLocalVariableDeclarationStatementNode node = declaration(input, session);
        input.expect(";", session);
        return input.finish(node, start);
    }

    public static JavaLocalVariableDeclarationStatementNode declaration(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaLocalVariableDeclarationStatementNode node =
                new JavaLocalVariableDeclarationStatementNode(input.name(), input.point());
        node.setModifiers(session.parse(JavaParseRoles.VARIABLE_MODIFIERS, input));
        node.setType(session.parse(JavaParseRoles.TYPE, input));
        do {
            node.addVariable(variable(input, session));
        } while (input.take(",", session));
        return input.finish(node, start);
    }

    public static JavaVariableDeclaratorNode variable(JavaParsingInput input, ParseSession session) {
        return variable(input, session, input.identifier(session));
    }

    public static JavaVariableDeclaratorNode variable(JavaParsingInput input, ParseSession session, JavaIdentifierNode name) {
        int start = name.span().start().offset();
        JavaVariableDeclaratorNode variable = new JavaVariableDeclaratorNode(input.name(), input.point());
        variable.setName(name);
        JavaTypeParser.dimensions(input, session).forEach(variable::addDimension);
        if (input.take("=", session)) {
            variable.setInitializer(session.parse(JavaParseRoles.VARIABLE_INITIALIZER, input));
        }
        return input.finish(variable, start);
    }
}
