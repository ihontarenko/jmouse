package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;

/** A dotted identifier sequence, stopping before a non-name selection such as '.class'. */
public final class JavaQualifiedNameParser implements ParseRule<JavaParsingInput, JavaQualifiedNameNode> {
    @Override
    public JavaQualifiedNameNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaQualifiedNameNode name = new JavaQualifiedNameNode(input.name(), input.point());
        name.addSegment(input.identifier(session));
        while (input.matches(".") && JavaToken.IDENTIFIER.matches(input.cursor().lookAt(1))) {
            input.expect(".", session);
            name.addSegment(input.identifier(session));
        }
        return input.finish(name, start);
    }
}
