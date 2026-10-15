package org.jmouse.java.parser.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.java.parser.source.SourceSpan;

/**
 * A node of the Java declaration tree, built on the jME {@link AbstractNode}.
 *
 * <p>Parent and children are the jME ones. Comments written before a declaration are its
 * {@link #getLeadingTrivia() leading trivia}, in source order and exactly as written — a documentation
 * comment among them. Trivia carries no position, so the documentation comment is also kept structured,
 * with its span and its explicit owner, on the declaration itself.
 *
 * <p>⚠️ jME nodes are mutable: {@link #add(Node)} and {@link #setParent(Node)} are public. The parser
 * hands over a finished tree and never touches it again; a caller that changes it changes what the
 * projections ({@link CompilationUnitNode#toSourceFile()}, {@link TypeNode#declaration()}) report.
 */
public abstract sealed class JavaNode extends AbstractNode
        permits CompilationUnitNode, PackageNode, ModuleNode, ImportNode, TypeNode, MemberNode {

    private final SourceSpan span;

    /**
     * Creates a node covering a span.
     *
     * @param span what the node covers, its documentation comment excluded
     */
    protected JavaNode(SourceSpan span) {
        this.span = span;
    }

    /**
     * Returns what the node covers.
     *
     * @return the span
     */
    public SourceSpan span() {
        return span;
    }

    /**
     * Visits this node, then its children in order. A {@link JavaNodeVisitor} receives the node by its type.
     *
     * @param visitor the visitor
     */
    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof JavaNodeVisitor javaVisitor) {
            dispatch(javaVisitor);
        } else {
            visitor.visit(this);
        }
        for (Node child : getChildren()) {
            child.accept(visitor);
        }
    }

    /**
     * Calls the visitor method for this node's type.
     *
     * @param visitor the visitor
     */
    protected abstract void dispatch(JavaNodeVisitor visitor);
}
