package org.jmouse.java.parser.grammar.declaration;

import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.declaration.JavaCompactConstructorDeclarationNode;

public final class JavaCompactConstructorDeclarationParser
        implements ParseRule<JavaConstructorHeader, JavaCompactConstructorDeclarationNode> {
    @Override
    public JavaCompactConstructorDeclarationNode parse(JavaConstructorHeader header, ParseSession session) {
        JavaParsingInput input = header.declaration().input();
        if (!header.typeParameters().isEmpty()) {
            throw input.error("A compact constructor cannot declare type parameters");
        }
        JavaCompactConstructorDeclarationNode node = new JavaCompactConstructorDeclarationNode(input.name(), input.point());
        node.setModifiers(header.declaration().modifiers());
        node.setName(header.name());
        node.setBody(session.parse(JavaDeclarationRoles.CONSTRUCTOR_BODY, input));
        return input.finish(node, header.declaration().start());
    }
}
