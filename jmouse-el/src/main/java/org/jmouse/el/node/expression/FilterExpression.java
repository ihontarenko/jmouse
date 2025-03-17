package org.jmouse.el.node.expression;

import org.jmouse.el.extension.Filter;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.Node;

public class FilterExpression extends AbstractExpressionNode {

    private final Filter filter;
    private final Node   left;

    public FilterExpression(Filter filter, Node left) {
        this.filter = filter;
        this.left = left;
    }

    public Filter getFilter() {
        return filter;
    }

    public Node getLeft() {
        return left;
    }

}
