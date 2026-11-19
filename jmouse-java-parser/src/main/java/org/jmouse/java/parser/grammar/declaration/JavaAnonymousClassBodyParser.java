package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaGrammarContext;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaRecoveryBoundaries;
import org.jmouse.java.parser.grammar.JavaSyntaxRecovery;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.body.JavaAnonymousClassBodyNode;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

public final class JavaAnonymousClassBodyParser implements ParseRule<JavaParsingInput, JavaAnonymousClassBodyNode> {
    @Override
    public JavaAnonymousClassBodyNode parse(JavaParsingInput input, ParseSession session) {
        return session.with(JavaGrammarContext.ENCLOSING_TYPE, new JavaEnclosingType("", JavaTypeForm.ANONYMOUS), () -> {
            int start = input.offset();
            SourceSpan opening = input.point();
            input.expect(JavaToken.LEFT_BRACE, session);
            JavaAnonymousClassBodyNode node = new JavaAnonymousClassBodyNode(input.name(), input.point());
            JavaSyntaxRecovery.readRegion(JavaDeclarationRoles.MEMBER, input, session, opening,
                    JavaRecoveryBoundaries.MEMBER, ParseProblem.Phase.STRUCTURE, node::addDeclaration);
            return input.finish(node, start);
        });
    }
}
