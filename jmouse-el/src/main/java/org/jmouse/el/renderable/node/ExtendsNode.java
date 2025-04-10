package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

/**
 * Represents an "extends" node in a template.
 * <p>
 * This node is used to specify that a template extends a parent template.
 * It evaluates an expression that should resolve to the parent template's name,
 * converts the result to a String, and then sets the parent template in the current template.
 * </p>
 */
public class ExtendsNode extends AbstractNode {

    private ExpressionNode path;

    /**
     * Returns the expression node that evaluates to the parent template name.
     *
     * @return the parent expression node
     */
    public ExpressionNode getPath() {
        return path;
    }

    /**
     * Sets the expression node that represents the parent template.
     *
     * @param path the parent expression node
     */
    public void setPath(ExpressionNode path) {
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
