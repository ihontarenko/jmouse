package org.jmouse.java.parser.projection;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jmouse.el.lexer.TokenizableSource.Entry;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.java.parser.lexer.JavaLexicalSnapshot;
import org.jmouse.java.parser.model.Annotation;
import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.JavaDoc;
import org.jmouse.java.parser.model.JavaDocOwner;
import org.jmouse.java.parser.model.Modifier;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeParameter;
import org.jmouse.java.parser.model.TypeReferenceRole;
import org.jmouse.java.parser.node.annotation.JavaAnnotationNode;
import org.jmouse.java.parser.node.annotation.JavaMarkerAnnotationNode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.name.JavaModifierNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;
import org.jmouse.java.parser.node.type.JavaTypeParameterNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** State of one immutable-model projection; it holds no cursor or parser. */
final class JavaProjectionContext {
    final JavaLexicalSnapshot source;
    final JavaSourceSpelling spelling;
    final JavaDocumentationProjection documentation;
    final JavaTypeReferenceProjection references;
    final Map<JavaSyntaxNode, Declaration> declarations = new IdentityHashMap<>();
    private final Map<String, Integer> symbols = new HashMap<>();

    JavaProjectionContext(JavaLexicalSnapshot source, JavaSyntaxNode root) {
        this.source = source;
        spelling = new JavaSourceSpelling(source);
        documentation = new JavaDocumentationProjection(source, root);
        references = new JavaTypeReferenceProjection(source);
    }

    SymbolId symbol(String identity) {
        int count = symbols.merge(identity, 1, Integer::sum);
        return new SymbolId(count == 1 ? identity : identity + "~" + count);
    }

    SymbolId member(String owner, String name) {
        return symbol(owner + "#" + name);
    }

    SymbolId executable(String owner, String name, List<String> parameters) {
        return member(owner, name + "(" + String.join(",", parameters) + ")");
    }

    static String qualify(String scope, String name) {
        return scope.isEmpty() ? name : scope + "." + name;
    }

    Set<Modifier> modifiers(JavaModifiersNode node) {
        Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        for (JavaSyntaxNode entry : node.entries()) {
            if (entry instanceof JavaModifierNode modifier) {
                modifiers.add(Modifier.fromKeyword(modifier.keyword()).orElseThrow(
                        () -> new TranslationRefusedException("The declaration view does not support modifier "
                                + modifier.keyword().spelling())));
            }
        }
        return Set.copyOf(modifiers);
    }

    List<Annotation> annotations(JavaModifiersNode node) {
        return annotations(node.entries().stream().filter(JavaAnnotationNode.class::isInstance)
                .map(JavaAnnotationNode.class::cast).toList());
    }

    List<Annotation> annotations(List<? extends JavaAnnotationNode> annotations) {
        return annotations.stream().map(this::annotation).toList();
    }

    private Annotation annotation(JavaAnnotationNode node) {
        references.annotation(node);
        String arguments = null;
        if (!(node instanceof JavaMarkerAnnotationNode)) {
            Entry opening = source.tokens().get(source.tokenIndexAtOrAfter(node.name().span().end().offset()));
            Entry closing = source.tokens().get(source.tokenIndexAtOrAfter(node.span().end().offset()) - 1);
            arguments = source.text().substring(opening.offset() + opening.length(), closing.offset());
        }
        return new Annotation(JavaSourceSpelling.name(node.name()), arguments, node.span());
    }

    List<TypeParameter> typeParameters(List<JavaTypeParameterNode> parameters) {
        return parameters.stream().map(parameter -> new TypeParameter(parameter.name().name(),
                types(parameter.bounds(), TypeReferenceRole.TYPE_PARAMETER_BOUND),
                annotations(parameter.annotations()), parameter.span())).toList();
    }

    String type(JavaTypeSyntaxNode node, TypeReferenceRole role) {
        references.type(node, role);
        return spelling.text(node);
    }

    List<String> types(List<JavaTypeSyntaxNode> nodes, TypeReferenceRole role) {
        return nodes.stream().map(node -> type(node, role)).toList();
    }

    JavaDoc document(JavaSyntaxNode syntax, SymbolId symbol, DeclarationKind kind, String qualified, String signature) {
        return documentation.owner(syntax, new JavaDocOwner(symbol, kind, qualified, signature, syntax.span()));
    }

    <D extends Declaration> D declaration(JavaSyntaxNode node, D declaration) {
        declarations.put(node, declaration);
        return declaration;
    }

    SourceSpan header(JavaSyntaxNode declaration, int bodyStart) {
        return spelling.header(declaration, bodyStart);
    }
}
