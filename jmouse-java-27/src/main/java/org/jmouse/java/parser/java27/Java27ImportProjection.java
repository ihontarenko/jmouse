package org.jmouse.java.parser.java27;

import org.jmouse.el.translate.NodeOperation;
import org.jmouse.java.parser.lexer.JavaLexicalSnapshot;
import org.jmouse.java.parser.model.ImportDeclaration;

import java.util.stream.Collectors;

public final class Java27ImportProjection
        implements NodeOperation<Java27BundleImportNode, JavaLexicalSnapshot, ImportDeclaration> {
    @Override
    public ImportDeclaration apply(Java27BundleImportNode node, JavaLexicalSnapshot context) {
        String name =
                node.name().segments().stream()
                        .map(segment -> segment.name())
                        .collect(Collectors.joining("."));
        return new ImportDeclaration(name, false, true, node.name().span(), node.span());
    }
}
