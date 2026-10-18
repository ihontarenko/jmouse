package org.jmouse.java.index;

import org.jmouse.java.parser.model.BodyReference;
import org.jmouse.java.parser.model.ExecutableBody;
import org.jmouse.java.parser.model.ImportDeclaration;
import org.jmouse.java.parser.model.JavaDoc;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.ParseValidity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Everything one file contributes to an index: what it declares, which type names it writes and
 * what it imports. Computed from that file alone, so it is valid for as long as the file's text is
 * unchanged and can be cached against the text's hash.
 *
 * @param source the name the file was parsed under, usually its path
 * @param packageName the package, or the empty string for the default package
 * @param imports the import declarations, in order
 * @param declarations every type and member, outer before inner, in source order
 * @param references every type name written in a declaration header, in source order
 * @param problems what the parser could not read; a non-empty list means the file was read in part
 */
public record FileIndex(
        String source,
        String packageName,
        List<ImportDeclaration> imports,
        List<IndexedDeclaration> declarations,
        List<IndexedReference> references,
        List<ParseProblem> problems,
        List<MemberSymbol> members,
        List<ExecutableBody> bodies,
        List<JavaDoc> javaDocs) {

    public static final String REVISION = "3";

    /** Source-compatible construction for indexes without retained JavaDoc. */
    public FileIndex(
            String source,
            String packageName,
            List<ImportDeclaration> imports,
            List<IndexedDeclaration> declarations,
            List<IndexedReference> references,
            List<ParseProblem> problems,
            List<MemberSymbol> members,
            List<ExecutableBody> bodies) {
        this(
                source,
                packageName,
                imports,
                declarations,
                references,
                problems,
                members,
                bodies,
                List.of());
    }

    /** Declaration-only construction retained for existing consumers. */
    public FileIndex(
            String source,
            String packageName,
            List<ImportDeclaration> imports,
            List<IndexedDeclaration> declarations,
            List<IndexedReference> references,
            List<ParseProblem> problems) {
        this(
                source,
                packageName,
                imports,
                declarations,
                references,
                problems,
                List.of(),
                List.of());
    }

    /** Copies the lists. */
    public FileIndex {
        javaDocs = List.copyOf(javaDocs);
        imports = List.copyOf(imports);
        declarations = List.copyOf(declarations);
        references = List.copyOf(references);
        problems = List.copyOf(problems);
        members = List.copyOf(members);
        bodies = List.copyOf(bodies);
    }

    /**
     * Indexes one parsed file.
     *
     * @param file the parser's result
     * @return the file's index
     */
    public static FileIndex of(JavaSourceFile file) {
        return FileIndexer.index(file);
    }

    /** Rebinds the path-neutral contribution to one caller-owned source identity. */
    public FileIndex withSource(String replacement) {
        List<MemberSymbol> rebased =
                members.stream()
                        .map(
                                member ->
                                        new MemberSymbol(
                                                replacement,
                                                member.symbolId(),
                                                member.container(),
                                                member.name(),
                                                member.qualifiedName(),
                                                member.kind(),
                                                member.type(),
                                                member.parameters(),
                                                member.modifiers(),
                                                member.nameSpan()))
                        .toList();
        return new FileIndex(
                replacement,
                packageName,
                imports,
                declarations,
                references,
                problems,
                rebased,
                bodies,
                javaDocs);
    }

    public boolean isStructureComplete() {
        return ParseValidity.structureComplete(problems);
    }

    public boolean isDeclarationComplete(SymbolId symbolId) {
        return declaration(symbolId)
                .map(declaration -> ParseValidity.declarationComplete(problems, declaration.span()))
                .orElse(false);
    }

    /** No definite member resolution is emitted from a recovered body. */
    public boolean isBodyComplete(SymbolId owner) {
        return ParseValidity.bodyComplete(problems, bodies, owner);
    }

    /**
     * Finds a declaration by its id.
     *
     * @param symbolId the id
     * @return the declaration, if this file declares it
     */
    public Optional<IndexedDeclaration> declaration(SymbolId symbolId) {
        return declarations.stream()
                .filter(declaration -> declaration.symbolId().equals(symbolId))
                .findFirst();
    }

    /** A body reference under the caret; end offsets are exclusive, as in the parser. */
    public Optional<BodyReference> bodyReferenceAt(int offset) {
        return bodies.stream()
                .flatMap(body -> body.references().stream())
                .filter(
                        reference ->
                                reference.span().start().offset() <= offset
                                        && offset < reference.span().end().offset())
                .min(Comparator.comparingInt(reference -> reference.span().length()));
    }

    /** A member declared under the caret. */
    public Optional<MemberSymbol> memberNamedAt(int offset) {
        return members.stream()
                .filter(
                        member ->
                                member.nameSpan().start().offset() <= offset
                                        && offset < member.nameSpan().end().offset())
                .findFirst();
    }

    /**
     * Finds the type name written at a position.
     *
     * @param offset a character offset into the file's text
     * @return the reference whose span contains it, if any
     */
    public Optional<IndexedReference> referenceAt(int offset) {
        return references.stream()
                .filter(
                        reference ->
                                reference.span().start().offset() <= offset
                                        && offset <= reference.span().end().offset())
                .findFirst();
    }

    /**
     * Finds the declaration whose name is written at a position.
     *
     * @param offset a character offset into the file's text
     * @return the declaration whose name span contains it, if any
     */
    public Optional<IndexedDeclaration> declarationNamedAt(int offset) {
        return declarations.stream()
                .filter(
                        declaration ->
                                declaration.nameSpan().start().offset() <= offset
                                        && offset <= declaration.nameSpan().end().offset())
                .findFirst();
    }

    /**
     * Returns the types this file declares, nested ones included.
     *
     * @return the type declarations, outer before inner
     */
    public List<IndexedDeclaration> types() {
        return declarations.stream().filter(IndexedDeclaration::isType).toList();
    }
}
