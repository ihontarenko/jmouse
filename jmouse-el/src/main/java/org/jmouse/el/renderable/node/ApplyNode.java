package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.node.expression.FilterNode;
import org.jmouse.el.renderable.NodeVisitor;

import java.util.List;

public class ApplyNode extends AbstractNode {

    private List<FilterNode> chain;
    private Node             body;

    public List<FilterNode> getChain() {
        return chain;
    }

    public void setChain(List<FilterNode> chain) {
        this.chain = chain;
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
        return "APPLY: %s".formatted(chain);
    }

}
