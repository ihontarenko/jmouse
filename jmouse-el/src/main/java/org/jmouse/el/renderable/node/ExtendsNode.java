package org.jmouse.el.renderable.node;

import org.jmouse.el.core.node.AbstractNode;
import org.jmouse.el.core.node.ExpressionNode;

/**
 * Represents an "extends" node in a template.
 * <p>
 * This node is used to specify that a template extends a parent template.
 * It evaluates an expression that should resolve to the parent template's name,
 * converts the result to a String, and then sets the parent template in the current template.
 * </p>
 */
public class ExtendsNode extends AbstractNode {

    private ExpressionNode parent;

    /**
     * Returns the expression node that evaluates to the parent template name.
     *
     * @return the parent expression node
     */
    public ExpressionNode getParent() {
        return parent;
    }

    /**
     * Sets the expression node that represents the parent template.
     *
     * @param parent the parent expression node
     */
    public void setParent(ExpressionNode parent) {
        this.parent = parent;
    }

}
