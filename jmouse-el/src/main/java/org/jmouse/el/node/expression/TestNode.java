package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.Node;

public class TestNode extends AbstractExpressionNode {

    private final String  name;
    private       Node    arguments;
    private       boolean negated;

    public TestNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Node getArguments() {
        return arguments;
    }

    public void setArguments(Node arguments) {
        this.arguments = arguments;
    }

    public boolean isNegated() {
        return negated;
    }

    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    /**
     * 🏗️ Evaluates the expression within the given context.
     *
     * @param context 🎛️ The evaluation context containing variables and functions.
     * @return 📤 The result of evaluating the expression.
     * @throws UnsupportedOperationException if evaluation is not implemented.
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        return context.getExtensions().getTest(getName());
    }

    @Override
    public String toString() {
        return "%s%s?".formatted(negated ? "!" : "", name);
    }
}
