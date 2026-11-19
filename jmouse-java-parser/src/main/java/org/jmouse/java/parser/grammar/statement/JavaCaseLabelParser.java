package org.jmouse.java.parser.grammar.statement;

import org.jmouse.el.lexer.TokenCursor.Savepoint;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaSyntaxException;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.pattern.JavaPatternNode;
import org.jmouse.java.parser.node.statement.JavaCaseGuardNode;
import org.jmouse.java.parser.node.statement.JavaCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaConstantCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaDefaultCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaNullCaseLabelNode;
import org.jmouse.java.parser.node.statement.JavaPatternCaseLabelNode;

/** A case payload is a constant, null, default or a pattern with an optional guard. */
public final class JavaCaseLabelParser implements ParseRule<JavaParsingInput, JavaCaseLabelNode> {
    @Override
    public JavaCaseLabelNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        if (input.take("null", session)) {
            input.requireFeature(JavaSourceFeatures.SWITCH_PATTERNS);
            return new JavaNullCaseLabelNode(input.name(), input.spanFrom(start));
        }
        if (input.take(JavaKeyword.DEFAULT, session)) {
            input.requireFeature(JavaSourceFeatures.SWITCH_PATTERNS);
            return new JavaDefaultCaseLabelNode(input.name(), input.spanFrom(start));
        }
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        JavaPatternNode pattern = null;
        try {
            pattern = session.parse(JavaParseRoles.PATTERN, input);
        } catch (JavaSyntaxException exception) {
            input.restore(checkpoint);
        }
        if (pattern != null) {
            input.requireFeature(JavaSourceFeatures.SWITCH_PATTERNS);
            JavaPatternCaseLabelNode node = new JavaPatternCaseLabelNode(input.name(), input.point());
            node.setPattern(pattern);
            int guardStart = input.offset();
            if (input.take(JavaKeyword.WHEN, session)) {
                JavaCaseGuardNode guard = new JavaCaseGuardNode(input.name(), input.point());
                guard.setCondition(session.parse(JavaParseRoles.CONDITIONAL_EXPRESSION, input));
                node.setGuard(input.finish(guard, guardStart));
            }
            return input.finish(node, start);
        }
        JavaConstantCaseLabelNode node = new JavaConstantCaseLabelNode(input.name(), input.point());
        node.setExpression(session.parse(JavaParseRoles.CONDITIONAL_EXPRESSION, input));
        return input.finish(node, start);
    }
}
