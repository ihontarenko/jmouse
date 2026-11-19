package org.jmouse.java.parser.grammar;

import java.util.function.Consumer;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;

/** The same argument-list production serves calls and class creation. */
public final class JavaArgumentListParser {
    private JavaArgumentListParser() {
    }

    public static void parse(JavaParsingInput input, ParseSession session, Consumer<JavaExpressionNode> destination) {
        input.expect("(", session);
        if (!input.take(")", session)) {
            do {
                destination.accept(session.parse(JavaParseRoles.EXPRESSION, input));
            } while (input.take(",", session));
            input.expect(")", session);
        }
    }
}
