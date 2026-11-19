package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.pattern.JavaPatternNode;
import org.jmouse.java.parser.node.pattern.JavaRecordPatternNode;
import org.jmouse.java.parser.node.pattern.JavaTypePatternNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

/** Recursive Java type and record patterns; matching semantics belong to analysis. */
public final class JavaPatternParser implements ParseRule<JavaParsingInput, JavaPatternNode> {
    @Override
    public JavaPatternNode parse(JavaParsingInput input, ParseSession session) {
        int start = input.offset();
        JavaModifiersNode modifiers = session.parse(JavaParseRoles.VARIABLE_MODIFIERS, input);
        JavaTypeSyntaxNode type = session.parse(JavaParseRoles.TYPE, input);
        if (input.take("(", session)) {
            input.requireFeature(JavaSourceFeatures.RECORD_PATTERNS);
            if (!modifiers.entries().isEmpty()) {
                throw input.error("A record pattern does not have variable modifiers");
            }
            JavaRecordPatternNode node = new JavaRecordPatternNode(input.name(), input.point());
            node.setType(type);
            if (!input.take(")", session)) {
                do {
                    node.addComponent(session.parse(JavaParseRoles.PATTERN, input));
                } while (input.take(",", session));
                input.expect(")", session);
            }
            return input.finish(node, start);
        }
        JavaTypePatternNode node = new JavaTypePatternNode(input.name(), input.point());
        node.setModifiers(modifiers);
        node.setType(type);
        node.setBinding(input.identifier(session));
        return input.finish(node, start);
    }
}
