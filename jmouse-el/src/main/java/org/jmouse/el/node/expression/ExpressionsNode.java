package org.jmouse.el.node.expression;

import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExpressionsNode extends AbstractExpression {

    private final List<Expression> expressions;

    /**
     * ⚠️ An unmodifiable <em>view</em>, taken once, rather than a copy taken per call.
     *
     * <p>{@link #getExpressions()} used to answer {@code List.copyOf(expressions)}, which is a fresh
     * list every time it is asked. That is invisible while a tree is walked once at load, and it is a
     * garbage generator the moment a node holding statements is evaluated repeatedly — a body inside a
     * fixed-rate loop allocates a copy of itself per tick, per branch, forever, purely to iterate it.
     * The view gives callers the same immutability with nothing allocated.</p>
     *
     * <p>A view is live where a copy was a snapshot, which changes nothing in practice: a tree is built
     * by a parser and read by an evaluator, and nothing holds a list across the seam between them.</p>
     */
    private final List<Expression> readOnly;

    public ExpressionsNode() {
        expressions = new ArrayList<>();
        readOnly = Collections.unmodifiableList(expressions);
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
        return readOnly;
    }

}
