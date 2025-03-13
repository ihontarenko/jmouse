package org.jmouse.template.node.expression;

import org.jmouse.template.extension.Filter;
import org.jmouse.template.node.AbstractExpressionNode;
import org.jmouse.template.node.Node;

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
