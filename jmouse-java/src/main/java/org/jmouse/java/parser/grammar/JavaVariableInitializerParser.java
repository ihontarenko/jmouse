package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.node.expression.JavaErrorExpressionNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.source.ParseProblem;

public final class JavaVariableInitializerParser
        implements ParseRule<JavaParsingInput, JavaExpressionNode> {
    @Override
    public JavaExpressionNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        try {
            return input.matches("{")
                    ? session.parse(JavaParseRoles.ARRAY_INITIALIZER, input)
                    : session.parse(JavaParseRoles.EXPRESSION, input);
        } catch (JavaSyntaxException exception) {
            input.restore(checkpoint);
            input.report(exception, ParseProblem.Phase.BODY);
            JavaSyntaxRecovery.synchronize(input, session, JavaRecoveryBoundaries.INITIALIZER);
            ParseProblem problem =
                    new ParseProblem(
                            exception.kind(),
                            exception.getMessage(),
                            exception.span(),
                            ParseProblem.Phase.BODY);
            return new JavaErrorExpressionNode(input.name(), input.spanFrom(start), problem);
        }
    }
}
