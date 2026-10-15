package org.jmouse.java.parser.node;

import org.jmouse.el.node.Visitor;

/**
 * A jME {@link Visitor} that also receives the Java declaration nodes by their own type.
 *
 * <p>{@link JavaNode#accept(Visitor)} calls the typed method when the visitor is a
 * {@code JavaNodeVisitor}, and the generic {@link Visitor#visit(org.jmouse.el.node.Node)} otherwise, so
 * any existing jME visitor can still walk a Java tree. Children are visited after their parent, in
 * source order. Every method defaults to doing nothing.
 */
public interface JavaNodeVisitor extends Visitor {

    /**
     * Visits the root.
     *
     * @param compilationUnit the file
     */
    default void visit(CompilationUnitNode compilationUnit) {
    }

    /**
     * Visits the package declaration.
     *
     * @param packageNode the package
     */
    default void visit(PackageNode packageNode) {
    }

    /**
     * Visits the module declaration.
     *
     * @param moduleNode the module
     */
    default void visit(ModuleNode moduleNode) {
    }

    /**
     * Visits an import declaration.
     *
     * @param importNode the import
     */
    default void visit(ImportNode importNode) {
    }

    /**
     * Visits a type declaration, before its record components and members.
     *
     * @param typeNode the type
     */
    default void visit(TypeNode typeNode) {
    }

    /**
     * Visits a member: field, method, constructor, enum constant, initializer or record component.
     *
     * @param memberNode the member
     */
    default void visit(MemberNode memberNode) {
    }
}
