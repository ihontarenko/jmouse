package org.jmouse.java.parser.node;

import java.util.ArrayList;
import java.util.List;
import org.jmouse.java.parser.lexer.JavaComment;
import org.jmouse.java.parser.model.ImportDeclaration;
import org.jmouse.java.parser.model.JavaDoc;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.ModuleDeclaration;
import org.jmouse.java.parser.model.PackageDeclaration;
import org.jmouse.java.parser.model.TypeDeclaration;
import org.jmouse.java.parser.model.TypeReference;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * The root of the declaration tree: one compilation unit. Its children are the package, module, import
 * and top-level type nodes, in source order.
 *
 * <p>What belongs to the file rather than to one declaration — every comment, every documentation
 * comment with its owner, every problem — is held here.
 */
public final class CompilationUnitNode extends JavaNode {

    private final String             sourceName;
    private final List<JavaComment>  comments;
    private final List<JavaDoc>      javaDocs;
    private final List<ParseProblem> problems;
    private final List<TypeReference> typeReferences;

    /**
     * Creates the root.
     *
     * @param sourceName     the name the caller gave the text
     * @param span           the whole text
     * @param comments       every comment, in order
     * @param javaDocs       every documentation comment outside opaque bodies, with its owner
     * @param problems       every problem, in order
     * @param typeReferences every type name written in a declaration header, in order
     */
    public CompilationUnitNode(String sourceName, SourceSpan span, List<JavaComment> comments, List<JavaDoc> javaDocs,
                               List<ParseProblem> problems, List<TypeReference> typeReferences) {
        super(span);
        this.sourceName = sourceName;
        this.comments = List.copyOf(comments);
        this.javaDocs = List.copyOf(javaDocs);
        this.problems = List.copyOf(problems);
        this.typeReferences = List.copyOf(typeReferences);
    }

    /**
     * Returns every type name written in a declaration header, in source order.
     *
     * @return the references, unresolved
     */
    public List<TypeReference> typeReferences() {
        return typeReferences;
    }

    /**
     * Returns the name the caller gave the text.
     *
     * @return the source name
     */
    public String sourceName() {
        return sourceName;
    }

    /**
     * Returns every problem found, in order.
     *
     * @return the problems
     */
    public List<ParseProblem> problems() {
        return problems;
    }

    /**
     * Projects the tree into the immutable record model.
     *
     * @return the file as records
     */
    public JavaSourceFile toSourceFile() {
        PackageDeclaration      packageDeclaration = null;
        ModuleDeclaration       moduleDeclaration  = null;
        List<ImportDeclaration> imports            = new ArrayList<>();
        List<TypeDeclaration>   types              = new ArrayList<>();
        for (var child : getChildren()) {
            switch (child) {
                case PackageNode packageNode -> packageDeclaration = packageNode.declaration();
                case ModuleNode moduleNode -> moduleDeclaration = moduleNode.declaration();
                case ImportNode importNode -> imports.add(importNode.declaration());
                case TypeNode typeNode -> types.add(typeNode.declaration());
                default -> {
                }
            }
        }
        return new JavaSourceFile(sourceName, packageDeclaration, moduleDeclaration, imports, types, comments, javaDocs, problems,
                typeReferences);
    }

    @Override
    protected void dispatch(JavaNodeVisitor visitor) {
        visitor.visit(this);
    }
}
