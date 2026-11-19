package org.jmouse.java.parser.projection;

import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaLexicalSnapshot;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaIdentifierNode;
import org.jmouse.java.parser.node.name.JavaQualifiedNameNode;
import org.jmouse.java.parser.node.type.JavaArrayDimensionNode;
import org.jmouse.java.parser.node.type.JavaArrayTypeNode;
import org.jmouse.java.parser.node.type.JavaNamedTypeNode;
import org.jmouse.java.parser.node.type.JavaPrimitiveTypeNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.node.type.JavaVoidTypeNode;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.List;
import java.util.stream.Collectors;

/** Written metadata and as-written identity spellings, never name resolution or reparsing. */
final class JavaSourceSpelling {
    private final JavaLexicalSnapshot source;

    JavaSourceSpelling(JavaLexicalSnapshot source) {
        this.source = source;
    }

    String text(JavaSyntaxNode node) {
        return collapse(node.span());
    }

    String collapse(SourceSpan span) {
        return span.slice(source.text()).replaceAll("\\s+", " ").strip();
    }

    SourceSpan header(JavaSyntaxNode declaration, int bodyStart) {
        return source.span(declaration.span().start().offset(), source.endBefore(bodyStart));
    }

    static String name(JavaQualifiedNameNode name) {
        return name.segments().stream()
                .map(JavaIdentifierNode::name)
                .collect(Collectors.joining("."));
    }

    static String identity(JavaTypeSyntaxNode type) {
        return switch (type) {
            case JavaPrimitiveTypeNode primitive ->
                    JavaKeyword.valueOf(primitive.kind().name()).spelling();
            case JavaVoidTypeNode ignored -> JavaKeyword.VOID.spelling();
            case JavaNamedTypeNode named ->
                    named.segments().stream()
                            .map(segment -> segment.name().name())
                            .collect(Collectors.joining("."));
            case JavaArrayTypeNode array ->
                    identity(array.elementType()) + "[]".repeat(array.dimensions().size());
            default ->
                    throw new IllegalArgumentException(
                            "This syntax cannot form a declaration identity: "
                                    + type.schema().kind().name());
        };
    }

    String dimensions(List<JavaArrayDimensionNode> dimensions) {
        StringBuilder text = new StringBuilder();
        for (JavaArrayDimensionNode dimension : dimensions) {
            if (!dimension.annotations().isEmpty()) {
                text.append(' ');
            }
            text.append(collapse(dimension.span()));
        }
        return text.toString();
    }
}
