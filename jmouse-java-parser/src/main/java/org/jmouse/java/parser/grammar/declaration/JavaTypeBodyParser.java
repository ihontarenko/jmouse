package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaGrammarContext;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaRecoveryBoundaries;
import org.jmouse.java.parser.grammar.JavaSyntaxRecovery;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.body.JavaTypeBodyNode;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

public final class JavaTypeBodyParser implements ParseRule<JavaParsingInput, JavaTypeBodyNode> {
    @Override
    public JavaTypeBodyNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        SourceSpan opening = input.point();
        input.expect(JavaToken.LEFT_BRACE, session);
        JavaTypeBodyNode node = new JavaTypeBodyNode(input.name(), input.point());
        JavaEnclosingType enclosing = session.context(JavaGrammarContext.ENCLOSING_TYPE);
        if (enclosing != null && enclosing.form() == JavaTypeForm.ENUM) {
            while (!input.matches(JavaToken.RIGHT_BRACE) && !input.matches(JavaToken.SEMICOLON) && !input.atEnd()) {
                JavaSyntaxRecovery.parse(JavaDeclarationRoles.ENUM_CONSTANT, input, session,
                        JavaRecoveryBoundaries.ENUM_CONSTANT, ParseProblem.Phase.STRUCTURE).ifPresent(node::addConstant);
                if (!input.take(JavaToken.COMMA, session)) {
                    break;
                }
            }
            if (!input.matches(JavaToken.RIGHT_BRACE) && !input.atEnd() && !input.take(JavaToken.SEMICOLON, session)) {
                input.report(input.error("Expected a semicolon after enum constants"), ParseProblem.Phase.STRUCTURE);
            }
        }
        JavaSyntaxRecovery.readRegion(JavaDeclarationRoles.MEMBER, input, session, opening,
                JavaRecoveryBoundaries.MEMBER, ParseProblem.Phase.STRUCTURE, node::addDeclaration);
        return input.finish(node, start);
    }
}
