package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaSyntaxException;
import org.jmouse.java.parser.node.statement.JavaStatementNode;

/** Chooses between the two complete for productions without resetting the work budget. */
public final class JavaForParser implements ParseRule<JavaParsingInput, JavaStatementNode> {
    @Override
    public JavaStatementNode parse(JavaParsingInput input, ParseSession session) {
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        try {
            return session.parse(JavaStatementRoles.ENHANCED_FOR_STATEMENT, input);
        } catch (JavaSyntaxException exception) {
            input.restore(checkpoint);
        }
        return session.parse(JavaStatementRoles.FOR_STATEMENT, input);
    }
}
