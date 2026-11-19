package org.jmouse.java.parser.grammar.declaration;

import java.util.List;
import java.util.Objects;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

/** Common written prefix of a field, method or annotation element; never reparsed by its rule. */
public record JavaMemberHeader(JavaDeclarationHeader declaration, List<JavaTypeParameterNode> typeParameters,
                               JavaTypeSyntaxNode type, JavaIdentifierNode name) {
    public JavaMemberHeader {
        Objects.requireNonNull(declaration, "declaration");
        typeParameters = List.copyOf(typeParameters);
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");
    }
}
