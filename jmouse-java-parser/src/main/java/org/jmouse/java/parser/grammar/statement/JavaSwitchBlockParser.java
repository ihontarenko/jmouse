package org.jmouse.java.parser.grammar.statement;

import java.util.Optional;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaRecoveryBoundaries;
import org.jmouse.java.parser.grammar.JavaSyntaxRecovery;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.statement.JavaSwitchBlockNode;
import org.jmouse.java.parser.node.statement.JavaSwitchRuleNode;
import org.jmouse.java.parser.node.statement.JavaSwitchStatementGroupNode;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

/** One switch body preserves either arrow rules or colon groups in source order. */
public final class JavaSwitchBlockParser implements ParseRule<JavaParsingInput, JavaSwitchBlockNode> {
    @Override
    public JavaSwitchBlockNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        SourceSpan opening = input.point();
        input.expect(JavaToken.LEFT_BRACE, session);
        JavaSwitchBlockNode node = new JavaSwitchBlockNode(input.name(), input.point());
        while (!input.matches(JavaToken.RIGHT_BRACE) && !input.atEnd()) {
            Optional<JavaSyntaxNode> entry = JavaSyntaxRecovery.attempt(input, session,
                    JavaRecoveryBoundaries.SWITCH_ENTRY, ParseProblem.Phase.BODY, () -> {
                        JavaSyntaxNode syntax = session.parse(JavaStatementRoles.SWITCH_ENTRY, input);
                        if (!node.entries().isEmpty()) {
                            JavaSyntaxNode previous = node.entries().getLast();
                            if (syntax instanceof JavaSwitchRuleNode && previous instanceof JavaSwitchStatementGroupNode
                                    || syntax instanceof JavaSwitchStatementGroupNode && previous instanceof JavaSwitchRuleNode) {
                                throw input.errorAt("Arrow rules and colon groups cannot be mixed", syntax.span());
                            }
                        }
                        return syntax;
                    });
            entry.ifPresent(node::addEntry);
        }
        JavaSyntaxRecovery.closeBrace(input, session, opening, ParseProblem.Phase.BODY);
        return input.finish(node, start);
    }
}