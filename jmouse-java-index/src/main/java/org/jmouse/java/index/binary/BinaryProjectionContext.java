package org.jmouse.java.index.binary;

import org.jmouse.el.writer.SourceWriter;
import org.jmouse.el.writer.TextSourceWriter;
import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.attribute.JavaInnerClassNode;
import org.jmouse.java.bytecode.node.attribute.JavaInnerClassesAttributeNode;
import org.jmouse.java.index.FileIndex;
import org.jmouse.java.index.IndexedDeclaration;
import org.jmouse.java.index.IndexedReference;
import org.jmouse.java.index.MemberSymbol;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.Modifier;
import org.jmouse.java.parser.model.Parameter;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeReferenceRole;
import org.jmouse.java.parser.source.JavaCoverageLevel;
import org.jmouse.java.parser.source.JavaSourceCoverage;
import org.jmouse.java.parser.source.LineIndex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** One projection invocation, using jME's writer and the source module's existing line index. */
public final class BinaryProjectionContext {
    record PendingDeclaration(
            SymbolId identity,
            DeclarationKind kind,
            String name,
            String qualifiedName,
            SymbolId container,
            List<String> variables,
            Set<Modifier> modifiers,
            BinaryTextRange nameRange,
            BinaryTextRange header,
            BinaryTextRange body,
            BinaryTextRange span) {}

    record PendingParameter(
            String name, BinaryTextRange type, BinaryTextRange span, boolean variableArity) {}

    record PendingMember(
            PendingDeclaration declaration,
            BinaryTextRange type,
            String fixedType,
            List<PendingParameter> parameters) {}

    private record PendingReference(
            String name,
            TypeReferenceRole role,
            BinaryTextRange range,
            SymbolId owner,
            SymbolId scope) {}

    private final String source;
    private final JavaClassFileNode classFile;
    private final ClassFileProjectionConfiguration configuration;
    private final SourceWriter writer;
    private final Map<String, String> qualifiedNames = new HashMap<>();
    private final Map<String, JavaInnerClassNode> innerClasses = new HashMap<>();
    private final Set<String> resolvingNames = new HashSet<>();
    private final List<PendingDeclaration> declarations = new ArrayList<>();
    private final List<PendingMember> members = new ArrayList<>();
    private final List<PendingReference> references = new ArrayList<>();
    private long steps;
    private long cachedNameCharacters;

    public BinaryProjectionContext(
            String source,
            JavaClassFileNode classFile,
            ClassFileProjectionConfiguration configuration) {
        this.source = source;
        this.classFile = classFile;
        this.configuration = configuration;
        writer =
                new TextSourceWriter(
                        configuration.layout(), configuration.limits().maximumOutputCharacters());
        classFile.attributes().stream()
                .filter(JavaInnerClassesAttributeNode.class::isInstance)
                .map(JavaInnerClassesAttributeNode.class::cast)
                .flatMap(table -> table.entries().stream())
                .forEach(
                        entry ->
                                innerClasses.put(
                                        classFile.pool().className(entry.innerClassIndex()),
                                        entry));
    }

    public JavaClassFileNode classFile() {
        return classFile;
    }

    public Optional<JavaInnerClassNode> innerClass(String internalName) {
        return Optional.ofNullable(innerClasses.get(internalName));
    }

    public ClassFileProjectionConfiguration configuration() {
        return configuration;
    }

    public SourceWriter writer() {
        return writer;
    }

    public void step() {
        if (steps >= configuration.limits().maximumWork()) {
            throw new IllegalStateException("Binary API projection work limit exceeded");
        }
        steps++;
    }

    public void requireNameCapacity(long characters) {
        if (characters > configuration.limits().maximumCachedNameCharacters()) {
            throw new IllegalStateException("Binary API projection name limit exceeded");
        }
    }

    public String qualifiedName(String internalName) {
        step();
        String existing = qualifiedNames.get(internalName);
        if (existing != null) {
            return existing;
        }
        if (!resolvingNames.add(internalName)) {
            throw new IllegalArgumentException(
                    "Cyclic InnerClasses naming metadata: " + internalName);
        }
        try {
            String name = configuration.naming().qualifiedName(internalName, this);
            requireNameCapacity(cachedNameCharacters + name.length());
            cachedNameCharacters += name.length();
            qualifiedNames.put(internalName, name);
            return name;
        } finally {
            resolvingNames.remove(internalName);
        }
    }

    public BinaryTextRange name(String name) {
        step();
        writer.word(name);
        return new BinaryTextRange(writer.length() - name.length(), writer.length());
    }

    public void reference(
            String name,
            TypeReferenceRole role,
            BinaryTextRange range,
            SymbolId owner,
            SymbolId scope) {
        step();
        references.add(new PendingReference(name, role, range, owner, scope));
    }

    void declaration(PendingDeclaration declaration) {
        declarations.add(declaration);
    }

    void member(PendingMember member) {
        members.add(member);
    }

    public ClassFileIndexer.IndexedClass finish(String packageName) {
        String text = writer.text();
        LineIndex lines = LineIndex.of(text);
        List<IndexedDeclaration> indexedDeclarations =
                declarations.stream()
                        .sorted(
                                Comparator.comparingInt(
                                                (PendingDeclaration declaration) ->
                                                        declaration.span().start())
                                        .thenComparing(
                                                Comparator.comparingInt(
                                                                (PendingDeclaration declaration) ->
                                                                        declaration.span().end())
                                                        .reversed()))
                        .map(
                                declaration ->
                                        new IndexedDeclaration(
                                                declaration.identity(),
                                                declaration.kind(),
                                                declaration.name(),
                                                declaration.qualifiedName(),
                                                declaration.container(),
                                                declaration.variables(),
                                                declaration.modifiers(),
                                                slice(text, declaration.header()).strip(),
                                                lines.span(
                                                        declaration.nameRange().start(),
                                                        declaration.nameRange().end()),
                                                declaration.body() == null
                                                        ? null
                                                        : lines.span(
                                                                declaration.body().start(),
                                                                declaration.body().end()),
                                                lines.span(
                                                        declaration.span().start(),
                                                        declaration.span().end())))
                        .toList();
        List<MemberSymbol> indexedMembers =
                members.stream()
                        .map(
                                member -> {
                                    PendingDeclaration declaration = member.declaration();
                                    List<Parameter> parameters =
                                            member.parameters().stream()
                                                    .map(
                                                            parameter ->
                                                                    new Parameter(
                                                                            parameter.name(),
                                                                            slice(
                                                                                            text,
                                                                                            parameter
                                                                                                    .type())
                                                                                    .strip(),
                                                                            List.of(),
                                                                            Set.of(),
                                                                            parameter
                                                                                    .variableArity(),
                                                                            false,
                                                                            lines.span(
                                                                                    parameter
                                                                                            .span()
                                                                                            .start(),
                                                                                    parameter
                                                                                            .span()
                                                                                            .end())))
                                                    .toList();
                                    return new MemberSymbol(
                                            source,
                                            declaration.identity(),
                                            declaration.container(),
                                            declaration.name(),
                                            declaration.qualifiedName(),
                                            declaration.kind(),
                                            member.type() == null
                                                    ? member.fixedType()
                                                    : slice(text, member.type()).strip(),
                                            parameters,
                                            declaration.modifiers(),
                                            lines.span(
                                                    declaration.nameRange().start(),
                                                    declaration.nameRange().end()));
                                })
                        .toList();
        List<IndexedReference> indexedReferences =
                references.stream()
                        .map(
                                reference ->
                                        new IndexedReference(
                                                reference.name(),
                                                reference.role(),
                                                lines.span(
                                                        reference.range().start(),
                                                        reference.range().end()),
                                                reference.owner(),
                                                reference.scope()))
                        .toList();
        FileIndex file =
                new FileIndex(
                        source,
                        packageName,
                        List.of(),
                        indexedDeclarations,
                        indexedReferences,
                        List.of(),
                        indexedMembers,
                        List.of(),
                        List.of(),
                        List.of(),
                        new JavaSourceCoverage(JavaCoverageLevel.DECLARATIONS, List.of()));
        return new ClassFileIndexer.IndexedClass(file, text);
    }

    private static String slice(String text, BinaryTextRange range) {
        return text.substring(range.start(), range.end());
    }
}
