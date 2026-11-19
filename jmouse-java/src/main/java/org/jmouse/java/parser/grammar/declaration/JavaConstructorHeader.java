package org.jmouse.java.parser.grammar.declaration;

import java.util.List;
import java.util.Objects;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;

public record JavaConstructorHeader(JavaDeclarationHeader declaration, List<JavaTypeParameterNode> typeParameters,
                                    JavaIdentifierNode name) {
    public JavaConstructorHeader {
        Objects.requireNonNull(declaration, "declaration");
        typeParameters = List.copyOf(typeParameters);
        Objects.requireNonNull(name, "name");
    }
}
