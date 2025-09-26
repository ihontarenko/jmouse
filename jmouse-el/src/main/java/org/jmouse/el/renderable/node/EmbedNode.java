package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

public class EmbedNode extends AbstractNode {

    private Expression path;
    private Expression with;
    private Node       body;

    public Expression getPath() {
        return path;
    }

    public void setPath(Expression path) {
        this.path = path;
    }

    public Expression getWith() {
        return with;
    }

    public void setWith(Expression with) {
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
