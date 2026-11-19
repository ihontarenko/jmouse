package org.jmouse.java.parser.grammar;

import java.util.List;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaToken;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.type.JavaTypeBoundKind;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.node.type.JavaWildcardTypeArgumentNode;

public final class JavaTypeArgumentParser implements ParseRule<JavaParsingInput, JavaTypeSyntaxNode> {
    @Override
    public JavaTypeSyntaxNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaParsingInput.Checkpoint checkpoint = input.checkpoint();
        List<JavaAnnotationNode> annotations = JavaTypeParser.annotations(input, session);
        if (!input.take(JavaToken.QUESTION, session)) {
            input.restore(checkpoint);
            return session.parse(JavaParseRoles.REFERENCE_TYPE, input);
        }
        JavaWildcardTypeArgumentNode wildcard = new JavaWildcardTypeArgumentNode(input.name(), input.point());
        annotations.forEach(wildcard::addAnnotation);
        if (input.take(JavaKeyword.EXTENDS, session)) {
            wildcard.setKind(JavaTypeBoundKind.EXTENDS);
            wildcard.setBound(session.parse(JavaParseRoles.REFERENCE_TYPE, input));
        } else if (input.take(JavaKeyword.SUPER, session)) {
            wildcard.setKind(JavaTypeBoundKind.SUPER);
            wildcard.setBound(session.parse(JavaParseRoles.REFERENCE_TYPE, input));
        } else {
            wildcard.setKind(JavaTypeBoundKind.UNBOUNDED);
        }
        return input.finish(wildcard, start);
    }
}
