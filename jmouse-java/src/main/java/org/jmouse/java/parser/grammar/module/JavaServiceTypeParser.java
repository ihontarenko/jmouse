package org.jmouse.java.parser.grammar.module;

import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.node.type.JavaNamedTypeNode;
import org.jmouse.java.parser.node.type.JavaNamedTypeSegmentNode;

/** Module service names admit neither type arguments nor annotations. */
public final class JavaServiceTypeParser {
    private JavaServiceTypeParser() {
    }

    public static JavaNamedTypeNode parse(JavaParsingInput input, ParseSession session) {
        JavaQualifiedNameNode name = session.parse(JavaParseRoles.QUALIFIED_NAME, input);
        JavaNamedTypeNode type = new JavaNamedTypeNode(input.name(), name.span());
        while (!name.segments().isEmpty()) {
            JavaNamedTypeSegmentNode segment = new JavaNamedTypeSegmentNode(input.name(), name.segments().getFirst().span());
            segment.setName(name.removeChild(JavaQualifiedNameNode.SEGMENTS, 0));
            type.addSegment(segment);
        }
        return type;
    }
}
