package org.jmouse.el.node;

import org.jmouse.el.node.expression.SpanNode;

/**
 * AbstractExpression is the base class for tag nodes in the AST.
 *
 * @author Ivan Hontarenko
 * @version 1.0
 */
public abstract class AbstractExpression extends AbstractNode implements Expression {

    private SpanNode spanNode;

    public SpanNode getSpan() {
        return spanNode;
    }

    public void setSpan(SpanNode spanNode) {
        this.spanNode = spanNode;
    }
}
