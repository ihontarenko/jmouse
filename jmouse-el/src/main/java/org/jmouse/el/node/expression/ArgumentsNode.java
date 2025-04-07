package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node that aggregates a list of argument expressions.
 * <p>
 * When evaluated, this node processes each child expression and returns their evaluated
 * results as an array. It is typically used to represent the arguments passed to functions
 * or operators in the expression language.
 * </p>
 */
public class ArgumentsNode extends AbstractExpressionNode {

    /**
     * Evaluates the arguments node.
     * <p>
     * Iterates over its child nodes, evaluates each expression, and collects the results into an array.
     * </p>
     *
     * @param context the evaluation context
     * @return an array containing the evaluated values of each child expression
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        List<Object> compiled = new ArrayList<>();

        for (Node child : getChildren()) {
            if (child instanceof ExpressionNode expression) {
                compiled.add(expression.evaluate(context));
            }
        }

        return compiled.toArray();
    }

    /**
     * Returns a string representation of the arguments node.
     * <p>
     * The string includes the literal "ARGUMENTS:" followed by a list of its child nodes.
     * </p>
     *
     * @return a string representation of the arguments node
     */
    @Override
    public String toString() {
        return "ARGUMENTS: " + getChildren();
    }
}
