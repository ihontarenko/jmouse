package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

public class NameNode extends AbstractExpression {

    private String name;
    private String alias;

    public NameNode() { }

    public NameNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return getAlias() != null ? getAlias() : getName();
    }

    @Override
    public String toString() {
        return getName() + (getAlias() != null ? " AS " + getAlias() : "");
    }
}
