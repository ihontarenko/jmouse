package org.jmouse.java.parser.model;

import java.util.ArrayList;
import java.util.List;
import org.jmouse.java.parser.lexer.JavaComment;
import org.jmouse.java.parser.source.ParseProblem;

/**
 * One parsed compilation unit: the root of an immutable declaration tree.
 *
 * <p>A file with problems is still a full result for everything that could be read; {@link #isComplete()}
 * says whether anything was skipped, and {@link #problems()} says what and where. A partial result is
 * never presented as a complete one.
 *
 * @param sourceName         the name the caller gave the text, for messages only
 * @param packageDeclaration the package declaration, or {@code null} for the default package
 * @param moduleDeclaration  the module declaration of a {@code module-info.java}, or {@code null}
 * @param imports            import declarations, in order
 * @param types              top-level types, in order
 * @param comments           every comment in the file, in order, comments inside bodies included
 * @param javaDocs           every documentation comment outside opaque bodies, each with its owner; a
 *                           comment over a several-variable field appears once per variable; a comment
 *                           that precedes no declaration appears once without an owner
 * @param problems           everything that was not read as written, in order
 */
public record JavaSourceFile(String sourceName, PackageDeclaration packageDeclaration, ModuleDeclaration moduleDeclaration,
                             List<ImportDeclaration> imports, List<TypeDeclaration> types, List<JavaComment> comments,
                             List<JavaDoc> javaDocs, List<ParseProblem> problems) {

    /**
     * Copies the lists.
     */
    public JavaSourceFile {
        imports = List.copyOf(imports);
        types = List.copyOf(types);
        comments = List.copyOf(comments);
        javaDocs = List.copyOf(javaDocs);
        problems = List.copyOf(problems);
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
     * Returns whether the whole file was read without a problem.
     *
     * @return {@code true} when {@link #problems()} is empty
     */
    public boolean isComplete() {
        return problems.isEmpty();
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
