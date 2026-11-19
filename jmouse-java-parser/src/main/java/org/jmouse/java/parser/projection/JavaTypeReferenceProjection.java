package org.jmouse.java.parser.projection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.java.parser.lexer.JavaLexicalSnapshot;
import org.jmouse.java.parser.model.TypeReference;
import org.jmouse.java.parser.model.TypeReferenceRole;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.type.JavaNamedTypeNode;
import org.jmouse.java.parser.node.type.JavaNamedTypeSegmentNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;

/** The legacy header-reference view is derived only from typed syntax, using jME traversal. */
final class JavaTypeReferenceProjection {
    private final JavaLexicalSnapshot source;
    private final List<TypeReference> references = new ArrayList<>();

    JavaTypeReferenceProjection(JavaLexicalSnapshot source) {
        this.source = source;
    }

    void annotation(JavaAnnotationNode annotation) {
        references.add(new TypeReference(JavaSourceSpelling.name(annotation.name()), TypeReferenceRole.ANNOTATION,
                annotation.name().span()));
    }

    void type(JavaTypeSyntaxNode root, TypeReferenceRole role) {
        root.accept(new Visitor() {
            @Override
            public void visit(Node node) {
                if (node instanceof JavaNamedTypeNode named) {
                    String name = named.segments().stream().map(segment -> segment.name().name())
                            .collect(Collectors.joining("."));
                    references.add(new TypeReference(name, referenceRole(named, root, role),
                            source.span(named.segments().getFirst().name().span().start().offset(),
                                    named.segments().getLast().name().span().end().offset())));
                } else if (node instanceof JavaAnnotationNode annotation) {
                    annotation(annotation);
                }
            }
        });
    }

    List<TypeReference> references() {
        return references.stream().sorted(Comparator.comparingInt(reference -> reference.span().start().offset())).toList();
    }

    private TypeReferenceRole referenceRole(Node node, Node root, TypeReferenceRole fallback) {
        for (Node current = node; current != root && current != null; current = current.getParent()) {
            if (current.getParent() instanceof JavaNamedTypeSegmentNode segment
                    && segment.arguments().contains(current)) {
                return TypeReferenceRole.TYPE_ARGUMENT;
            }
        }
        return fallback;
    }
}
