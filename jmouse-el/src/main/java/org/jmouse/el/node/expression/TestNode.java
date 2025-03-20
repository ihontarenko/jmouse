package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Test;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.ExpressionNode;

public class TestNode extends AbstractExpressionNode {

    private final String         name;
    private       ExpressionNode arguments;
    private       ExpressionNode left;
    private       boolean        negated;

    public TestNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ExpressionNode getArguments() {
        return arguments;
    }

    public void setArguments(ExpressionNode arguments) {
        this.arguments = arguments;
    }

    public boolean isNegated() {
        return negated;
    }

    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public void setLeft(ExpressionNode left) {
        this.left = left;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Arguments arguments = Arguments.empty();
        Test      test      = context.getExtensions().getTest(getName());
        Object    instance  = getLeft().evaluate(context);

        if (getArguments() != null) {
            if (getArguments().evaluate(context) instanceof Object[] array) {
                arguments = Arguments.forArray(array);
            }
        }

        return isNegated() != test.test(instance, arguments, context);
    }

    @Override
    public String toString() {
        return "IS %s %s%s".formatted(left, negated ? "NOT " : "", name);
    }
}
