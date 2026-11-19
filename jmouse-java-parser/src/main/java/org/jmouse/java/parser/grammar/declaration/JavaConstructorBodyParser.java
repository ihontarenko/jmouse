package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaRecoveryBoundaries;
import org.jmouse.java.parser.grammar.JavaSyntaxException;
import org.jmouse.java.parser.grammar.JavaSyntaxRecovery;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.body.JavaConstructorBodyNode;
import org.jmouse.java.parser.node.body.JavaConstructorBodySyntaxNode;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

public final class JavaConstructorBodyParser implements ParseRule<JavaParsingInput, JavaConstructorBodySyntaxNode> {
    @Override
    public JavaConstructorBodyNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        SourceSpan opening = input.point();
        input.expect(JavaToken.LEFT_BRACE, session);
        JavaConstructorBodyNode node = new JavaConstructorBodyNode(input.name(), input.point());
        if (!input.matches("}")) {
            JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
            try {
                node.setInvocation(session.parse(JavaParseRoles.CONSTRUCTOR_INVOCATION, input));
            } catch (JavaSyntaxException exception) {
                input.restore(checkpoint);
            }
        }
        JavaSyntaxRecovery.readRegion(JavaDeclarationRoles.BLOCK_ITEM, input, session, opening,
                JavaRecoveryBoundaries.STATEMENT, ParseProblem.Phase.BODY, node::addStatement);
        return input.finish(node, start);
    }
}
