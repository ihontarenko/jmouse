package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

public class EmbedNode extends AbstractNode {

    private ExpressionNode path;
    private ExpressionNode with;
    private Node           body;

    public ExpressionNode getPath() {
        return path;
    }

    public void setPath(ExpressionNode path) {
        this.path = path;
    }

    public ExpressionNode getWith() {
        return with;
    }

    public void setWith(ExpressionNode with) {
        this.with = with;
    }

    public Node getBody() {
        return body;
    }

    public void setBody(Node body) {
        this.body = body;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        }
    }

    @Override
    public String toString() {
        return "EMBED: " + getPath();
    }

}
