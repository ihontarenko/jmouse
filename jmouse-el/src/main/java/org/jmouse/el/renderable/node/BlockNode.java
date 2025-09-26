package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

public class BlockNode extends AbstractNode {

    private Expression name;
    private Node       body;
    private boolean override = false;

    public Expression getName() {
        return name;
    }

    public void setName(Expression name) {
        this.name = name;
    }

    public Node getBody() {
        return body;
    }

    public void setBody(Node body) {
        this.body = body;
    }

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
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

}
