package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.annotation.JavaAnnotationArrayValueNode;

/** Annotation values have their own replaceable production, including recursive array values. */
public final class JavaAnnotationValueParser implements ParseRule<JavaParsingInput, JavaSyntaxNode> {
    @Override
    public JavaSyntaxNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        if (input.matches(JavaToken.AT)) {
            return session.parse(JavaParseRoles.ANNOTATION, input);
        }
        if (input.take(JavaToken.LEFT_BRACE, session)) {
            JavaAnnotationArrayValueNode array = new JavaAnnotationArrayValueNode(input.name(), input.point());
            if (!input.take(JavaToken.RIGHT_BRACE, session)) {
                do {
                    array.addValue(session.parse(JavaParseRoles.ANNOTATION_VALUE, input));
                } while (input.take(JavaToken.COMMA, session) && !input.matches(JavaToken.RIGHT_BRACE));
                input.expect(JavaToken.RIGHT_BRACE, session);
            }
            return input.finish(array, start);
        }
        return session.parse(JavaParseRoles.CONDITIONAL_EXPRESSION, input);
    }
}
