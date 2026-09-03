package org.jmouse.el.node;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.helpers.Strings.underscored;

/**
 * An abstract implementation of the {@link Node} interface, providing
 * basic parent-child relationships for hierarchical structures.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
abstract public class AbstractNode implements Node {

    protected final List<Node> children = new ArrayList<>();    // List of child nodes
    protected       Node       parent;                          // Parent node reference

    /**
     * What was written above this node — see {@link Trivia}.
     *
     * <p>⚠️ Created on demand rather than eagerly. Most nodes in most trees carry none, and a language
     * parses thousands of them; an always-allocated list is a per-node cost paid for a property that is
     * usually absent.</p>
     */
    private List<Trivia> leadingTrivia;
    private Trivia       trailingTrivia;

    @Override
    public List<Trivia> getLeadingTrivia() {
        return leadingTrivia == null ? List.of() : List.copyOf(leadingTrivia);
    }

    @Override
    public void addLeadingTrivia(List<Trivia> trivia) {
        if (trivia.isEmpty()) {
            return;
        }

        if (leadingTrivia == null) {
            leadingTrivia = new ArrayList<>(trivia.size());
        }

        leadingTrivia.addAll(trivia);
    }

    @Override
    public Trivia getTrailingTrivia() {
        return trailingTrivia;
    }

    @Override
    public void setTrailingTrivia(Trivia trivia) {
        this.trailingTrivia = trivia;
    }

    /**
     * Constructs an {@code AbstractNode} with no parent.
     */
    public AbstractNode() {
        this(null);
    }

    /**
     * Constructs an {@code AbstractNode} with the specified parent node.
     *
     * @param parent the parent node, or {@code null} if this is a sourceRoot node
     */
    public AbstractNode(Node parent) {
        this.parent = parent;
    }

    /**
     * Returns the parent node of this node.
     *
     * @return the parent node or {@code null} if this is a sourceRoot node
     */
    @Override
    public Node getParent() {
        return this.parent;
    }

    /**
     * Sets the parent node of this node.
     *
     * @param node the parent node to set
     */
    @Override
    public void setParent(Node node) {
        this.parent = node;
    }

    /**
     * Returns the list of child nodes.
     *
     * @return a list of child nodes
     */
    @Override
    public List<Node> getChildren() {
        return this.children;
    }

    @Override
    public String toString() {
        return "%s".formatted(underscored(getClass().getSimpleName(), true));
    }
}
