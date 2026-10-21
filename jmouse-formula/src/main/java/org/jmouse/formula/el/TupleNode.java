package org.jmouse.formula.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;

import java.util.List;

/**
 * {@code (a, b)}, {@code (cos t, sin t, t)}: values written together — a point's coordinates, a curve's components.
 * Parsed by jMF's grammar wherever a parenthesised group may stand, so {@code vector((0, 0), P)} reads too. A tuple is
 * not a number: it is evaluated as the list of its components' values, and a formula refuses it where a number is wanted.
 */
public final class TupleNode extends AbstractExpression {

    private final List<Expression> components;

    public TupleNode(List<Expression> components) {
        this.components = List.copyOf(components);
    }

    /** The components, in the order written; at least two. */
    public List<Expression> components() {
        return components;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return components.stream().map(component -> component.evaluate(context)).toList();
    }

    @Override
    public String toSource() {
        return "(" + String.join(", ", components.stream().map(Expression::toSource).toList()) + ")";
    }

    @Override
    public String toString() {
        return "( TUPLE : " + components + " )";
    }
}
