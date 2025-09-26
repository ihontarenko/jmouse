package org.jmouse.el.renderable.node;

import org.jmouse.el.node.AbstractNode;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.NodeVisitor;

public class SetNode extends AbstractNode {

    private String     variable;
    private Expression value;

    public String getVariable() {
        return variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public Expression getValue() {
        return value;
    }

    public void setValue(Expression value) {
        this.value = value;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor instanceof NodeVisitor nv) {
            nv.visit(this);
        } else {
            visitor.visit(this);
        }
    }

    @Override
    public String toString() {
        return "SET: %s = %s".formatted(variable, value);
    }
}
