package org.jmouse.java.index.binary;

import org.jmouse.java.index.FileIndex;
import org.jmouse.java.index.IndexedDeclaration;
import org.jmouse.java.index.IndexedReference;
import org.jmouse.java.parser.JavaParser;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeReferenceRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure class-bytes to FileIndex conversion, including generic signatures and navigable API text.
 */
public final class ClassFileIndexer {

    public static final String REVISION = "1";
    private final JavaParser parser = new JavaParser();
    private final ClassApiRenderer renderer = new ClassApiRenderer();

    public IndexedClass index(String source, byte[] classBytes) {
        BinaryClass binary = ClassApiReader.read(classBytes);
        String text = renderer.render(binary);
        FileIndex parsed = FileIndex.of(parser.parse(source, text));
        SymbolId identity = new SymbolId(SignatureTypes.name(binary.binaryName()));
        IndexedDeclaration type =
                parsed.declaration(identity)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Could not read the API of "
                                                        + binary.binaryName()
                                                        + ": "
                                                        + parsed.problems()));
        List<IndexedDeclaration> declarations =
                parsed.declarations().stream()
                        .filter(
                                declaration ->
                                        declaration.symbolId().equals(identity)
                                                || identity.equals(declaration.container()))
                        .toList();
        List<IndexedReference> references =
                new ArrayList<>(
                        parsed.references().stream()
                                .filter(
                                        reference ->
                                                reference.owner() != null
                                                        && (reference.owner().equals(identity)
                                                                || reference
                                                                        .owner()
                                                                        .value()
                                                                        .startsWith(
                                                                                identity.value()
                                                                                        + "#")))
                                .toList());

        addParent(references, type, binary.superclass(), TypeReferenceRole.EXTENDS);
        for (String implemented : binary.interfaces()) {
            addParent(references, type, implemented, TypeReferenceRole.IMPLEMENTS);
        }

        FileIndex contribution =
                new FileIndex(
                        source,
                        parsed.packageName(),
                        parsed.imports(),
                        declarations,
                        references,
                        parsed.problems(),
                        parsed.members().stream()
                                .filter(member -> member.container().equals(identity))
                                .toList(),
                        List.of());
        return new IndexedClass(contribution, text);
    }

    private void addParent(
            List<IndexedReference> references,
            IndexedDeclaration type,
            String written,
            TypeReferenceRole role) {
        if (written == null) {
            return;
        }
        int arguments = written.indexOf('<');
        String name = arguments < 0 ? written : written.substring(0, arguments);
        boolean known =
                references.stream()
                        .anyMatch(
                                reference ->
                                        type.symbolId().equals(reference.owner())
                                                && reference.name().equals(name)
                                                && (reference.role() == TypeReferenceRole.EXTENDS
                                                        || reference.role()
                                                                == TypeReferenceRole.IMPLEMENTS));
        if (!known) {
            references.add(
                    new IndexedReference(
                            name, role, type.nameSpan(), type.symbolId(), type.container()));
        }
    }

    public record IndexedClass(FileIndex file, String text) {}
}
