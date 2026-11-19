package org.jmouse.java.parser.model;

import org.jmouse.java.parser.lexer.JavaComment;
import org.jmouse.java.parser.source.JavaSourceCoverage;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.ParseValidity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One parsed compilation unit: the root of an immutable declaration tree.
 *
 * <p>{@link #isComplete()} describes validity of the requested navigation view. Full executable
 * coverage is reported separately by {@link #isSyntaxComplete()} and {@link #coverage()}; metadata
 * parsing deliberately records omitted regions without treating them as syntax errors.
 *
 * @param sourceName the name the caller gave the text, for messages only
 * @param packageDeclaration the package declaration, or {@code null} for the default package
 * @param moduleDeclaration the module declaration of a {@code module-info.java}, or {@code null}
 * @param imports import declarations, in order
 * @param types top-level types, in order
 * @param comments every comment in the file, in order, comments inside bodies included
 * @param javaDocs every documentation comment outside opaque bodies, each with its owner; a comment
 *     over a several-variable field appears once per variable; a comment that precedes no
 *     declaration appears once without an owner
 * @param problems everything that was not read as written, in order
 * @param typeReferences every type name written in a declaration header, in source order — see
 *     {@link TypeReference}
 */
public record JavaSourceFile(
        String sourceName,
        PackageDeclaration packageDeclaration,
        ModuleDeclaration moduleDeclaration,
        List<ImportDeclaration> imports,
        List<TypeDeclaration> types,
        List<JavaComment> comments,
        List<JavaDoc> javaDocs,
        List<ParseProblem> problems,
        List<TypeReference> typeReferences,
        List<ExecutableBody> bodies,
        List<LocalTypeDeclaration> localTypes,
        JavaSourceCoverage coverage) {

    public JavaSourceFile(
            String sourceName,
            PackageDeclaration packageDeclaration,
            ModuleDeclaration moduleDeclaration,
            List<ImportDeclaration> imports,
            List<TypeDeclaration> types,
            List<JavaComment> comments,
            List<JavaDoc> javaDocs,
            List<ParseProblem> problems,
            List<TypeReference> typeReferences,
            List<ExecutableBody> bodies,
            List<LocalTypeDeclaration> localTypes) {
        this(
                sourceName,
                packageDeclaration,
                moduleDeclaration,
                imports,
                types,
                comments,
                javaDocs,
                problems,
                typeReferences,
                bodies,
                localTypes,
                JavaSourceCoverage.unspecified());
    }

    public JavaSourceFile(
            String sourceName,
            PackageDeclaration packageDeclaration,
            ModuleDeclaration moduleDeclaration,
            List<ImportDeclaration> imports,
            List<TypeDeclaration> types,
            List<JavaComment> comments,
            List<JavaDoc> javaDocs,
            List<ParseProblem> problems,
            List<TypeReference> typeReferences,
            List<ExecutableBody> bodies) {
        this(
                sourceName,
                packageDeclaration,
                moduleDeclaration,
                imports,
                types,
                comments,
                javaDocs,
                problems,
                typeReferences,
                bodies,
                List.of());
    }

    /** Source-compatible declaration-only construction. */
    public JavaSourceFile(
            String sourceName,
            PackageDeclaration packageDeclaration,
            ModuleDeclaration moduleDeclaration,
            List<ImportDeclaration> imports,
            List<TypeDeclaration> types,
            List<JavaComment> comments,
            List<JavaDoc> javaDocs,
            List<ParseProblem> problems,
            List<TypeReference> typeReferences) {
        this(
                sourceName,
                packageDeclaration,
                moduleDeclaration,
                imports,
                types,
                comments,
                javaDocs,
                problems,
                typeReferences,
                List.of());
    }

    /** Copies the lists. */
    public JavaSourceFile {
        imports = List.copyOf(imports);
        types = List.copyOf(types);
        comments = List.copyOf(comments);
        javaDocs = List.copyOf(javaDocs);
        problems = List.copyOf(problems);
        typeReferences = List.copyOf(typeReferences);
        bodies = List.copyOf(bodies);
        localTypes = List.copyOf(localTypes);
        Objects.requireNonNull(coverage, "coverage");
    }

    /**
     * Returns the package name.
     *
     * @return the dotted name, or the empty string for the default package
     */
    public String packageName() {
        return packageDeclaration == null ? "" : packageDeclaration.name();
    }

    /**
     * Returns whether the requested view was read without a problem. See {@link
     * #isSyntaxComplete()} for full coverage.
     *
     * @return {@code true} when {@link #problems()} is empty
     */
    public boolean isComplete() {
        return problems.isEmpty();
    }

    public boolean isSyntaxComplete() {
        return coverage.fullyRead() && problems.isEmpty();
    }

    /** Whether declaration structure was read without lexical or structural errors. */
    public boolean isStructureComplete() {
        return ParseValidity.structureComplete(problems);
    }

    public boolean isDeclarationComplete(SymbolId symbolId) {
        return declarations().stream()
                .filter(declaration -> declaration.symbolId().equals(symbolId))
                .anyMatch(
                        declaration ->
                                ParseValidity.declarationComplete(problems, declaration.span()));
    }

    /**
     * Conservative validity: a body with recovery diagnostics is not a source of definite usages.
     */
    public boolean isBodyComplete(SymbolId owner) {
        return ParseValidity.bodyComplete(problems, bodies, owner);
    }

    /**
     * Returns every declaration in the file, depth first in source order: a type, then its record
     * components, then its members — a nested type followed immediately by its own declarations.
     *
     * @return all declarations
     */
    public List<Declaration> declarations() {
        List<Declaration> collected = new ArrayList<>();
        for (TypeDeclaration type : types) {
            collect(type, collected);
        }
        for (LocalTypeDeclaration type : localTypes) {
            collect(type.declaration(), collected);
        }
        return List.copyOf(collected);
    }

    private static void collect(TypeDeclaration type, List<Declaration> collected) {
        collected.add(type);
        collected.addAll(type.recordComponents());
        for (Declaration member : type.members()) {
            if (member instanceof TypeDeclaration nested) {
                collect(nested, collected);
            } else {
                collected.add(member);
            }
        }
    }
}
