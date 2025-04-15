package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.node.expression.MapNode;
import org.jmouse.el.renderable.NodeVisitor;

public class ScopeNode extends AbstractNode {

    private MapNode with;
    private Node    body;

    public MapNode getWith() {
        return with;
    }

    public void setWith(MapNode with) {
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
        return "SCOPE: (%s)".formatted(with);
    }

}
