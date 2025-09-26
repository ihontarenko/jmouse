package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

/**
 * Represents an "extends" node in a view.
 * <p>
 * This node is used to specify that a view extends a parent view.
 * It evaluates an expression that should resolve to the parent view's name,
 * converts the result to a String, and then sets the parent view in the current view.
 * </p>
 */
public class ExtendsNode extends AbstractNode {

    private Expression path;

    /**
     * Returns the expression node that evaluates to the parent view name.
     *
     * @return the parent expression node
     */
    public Expression getPath() {
        return path;
    }

    /**
     * Sets the expression node that represents the parent view.
     *
     * @param path the parent expression node
     */
    public void setPath(Expression path) {
        this.path = path;
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        }
    }

    @Override
    public String toString() {
        return "EXTENDS: " + getPath();
    }
}
