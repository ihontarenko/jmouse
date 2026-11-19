package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaDeclarationBodyReader;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaParsingMode;
import org.jmouse.java.parser.grammar.JavaRecoveryBoundaries;
import org.jmouse.java.parser.grammar.JavaSyntaxRecovery;
import org.jmouse.java.parser.grammar.declaration.JavaDeclarationRoles;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.statement.JavaBlockNode;
import org.jmouse.java.parser.source.JavaRegionKind;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

/** Parses block syntax. */
public final class JavaBlockParser implements ParseRule<JavaParsingInput, JavaBlockNode> {
    @Override
    public JavaBlockNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        SourceSpan opening = input.point();
        input.expect(JavaToken.LEFT_BRACE, session);
        JavaBlockNode node = new JavaBlockNode(input.name(), input.point());
        if (input.mode() == JavaParsingMode.DECLARATIONS_ONLY) {
            node.addStatement(
                    JavaDeclarationBodyReader.read(
                            input, session, opening, JavaRegionKind.EXECUTABLE_BLOCK));
            return input.finish(node, start);
        }
        JavaSyntaxRecovery.readRegion(
                JavaDeclarationRoles.BLOCK_ITEM,
                input,
                session,
                opening,
                JavaRecoveryBoundaries.STATEMENT,
                ParseProblem.Phase.BODY,
                node::addStatement);
        return input.finish(node, start);
    }
}
