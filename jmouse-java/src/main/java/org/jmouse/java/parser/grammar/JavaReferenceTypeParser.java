package org.jmouse.java.parser.grammar;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.node.type.JavaArrayTypeNode;
import org.jmouse.java.parser.node.type.JavaNamedTypeNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

public final class JavaReferenceTypeParser implements ParseRule<JavaParsingInput, JavaTypeSyntaxNode> {
    @Override
    public JavaTypeSyntaxNode parse(JavaParsingInput input, ParseSession session) {
        JavaTypeSyntaxNode type = session.parse(JavaParseRoles.TYPE, input);
        if (!(type instanceof JavaNamedTypeNode || type instanceof JavaArrayTypeNode)) {
            throw input.errorAt("Expected a reference type", type.span());
        }
        return type;
    }
}
