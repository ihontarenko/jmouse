package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.annotation.JavaAnnotationValuePairNode;
import org.jmouse.java.parser.node.annotation.JavaMarkerAnnotationNode;
import org.jmouse.java.parser.node.annotation.JavaNormalAnnotationNode;
import org.jmouse.java.parser.node.annotation.JavaSingleElementAnnotationNode;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;

/** Marker, single-value and named-value annotations, including nested array initializers. */
public final class JavaAnnotationParser implements ParseRule<JavaParsingInput, JavaAnnotationNode> {
    @Override
    public JavaAnnotationNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        input.expect("@", session);
        JavaQualifiedNameNode name = session.parse(JavaParseRoles.QUALIFIED_NAME, input);
        if (!input.take("(", session)) {
            JavaMarkerAnnotationNode node = new JavaMarkerAnnotationNode(input.name(), input.point());
            node.setName(name);
            return input.finish(node, start);
        }
        if (input.take(")", session)) {
            JavaNormalAnnotationNode node = new JavaNormalAnnotationNode(input.name(), input.point());
            node.setName(name);
            return input.finish(node, start);
        }
        if (input.cursor().lookAt(1).value().equals("=") && !input.cursor().lookAt(2).value().equals("=")) {
            JavaNormalAnnotationNode node = new JavaNormalAnnotationNode(input.name(), input.point());
            node.setName(name);
            do {
                int valueStart = input.offset();
                JavaAnnotationValuePairNode pair = new JavaAnnotationValuePairNode(input.name(), input.point());
                pair.setName(input.identifier(session));
                input.expect("=", session);
                pair.setValue(session.parse(JavaParseRoles.ANNOTATION_VALUE, input));
                node.addValue(input.finish(pair, valueStart));
            } while (input.take(",", session));
            input.expect(")", session);
            return input.finish(node, start);
        }
        JavaSingleElementAnnotationNode node = new JavaSingleElementAnnotationNode(input.name(), input.point());
        node.setName(name);
        node.setValue(session.parse(JavaParseRoles.ANNOTATION_VALUE, input));
        input.expect(")", session);
        return input.finish(node, start);
    }
}
