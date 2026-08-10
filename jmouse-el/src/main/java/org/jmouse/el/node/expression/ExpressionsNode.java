package org.jmouse.el.node.expression;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;

import java.util.ArrayList;
import java.util.List;

public class ExpressionsNode extends AbstractExpression {

    private final List<Expression> expressions;

    public ExpressionsNode() {
        expressions = new ArrayList<>();
    }

    public void addExpression(Expression expression) {
        expressions.add(expression);
    }

    public boolean hasExpression(Class<? extends Node> nodeType) {
        return getExpressions().stream().anyMatch(nodeType::isInstance);
    }

    public List<Expression> getExpressions(Class<? extends Node> nodeType) {
        return getExpressions().stream().filter(nodeType::isInstance).toList();
    }

    public List<Expression> getExpressions() {
        return List.copyOf(expressions);
    }

}
