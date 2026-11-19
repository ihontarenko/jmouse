package org.jmouse.java.parser.grammar.statement;

import java.util.Map;
import org.jmouse.el.lexer.TokenCursor.Savepoint;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaSyntaxException;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.statement.JavaStatementNode;

/** Keyword routes are configuration; declaration/expression ambiguity uses bounded jME checkpoints. */
public final class JavaStatementParser implements ParseRule<JavaParsingInput, JavaStatementNode> {
    private final Map<String, ParseRole<JavaParsingInput, ? extends JavaStatementNode>> routes;

    public JavaStatementParser(Map<String, ParseRole<JavaParsingInput, ? extends JavaStatementNode>> routes) {
        this.routes = Map.copyOf(routes);
    }

    @Override
    public JavaStatementNode parse(JavaParsingInput input, ParseSession session) {
        ParseRole<JavaParsingInput, ? extends JavaStatementNode> route = routes.get(input.token().value());
        if (route != null) {
            return session.parse(route, input);
        }
        if (JavaToken.IDENTIFIER.matches(input.token()) && input.cursor().lookAt(1).value().equals(":")) {
            return session.parse(JavaStatementRoles.LABELED_STATEMENT, input);
        }
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        try {
            return session.parse(JavaStatementRoles.LOCAL_VARIABLE_DECLARATION, input);
        } catch (JavaSyntaxException exception) {
            input.restore(checkpoint);
        }
        return session.parse(JavaStatementRoles.EXPRESSION_STATEMENT, input);
    }
}
