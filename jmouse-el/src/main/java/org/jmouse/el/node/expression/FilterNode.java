package org.jmouse.el.node.expression;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.EvaluationException;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Filter;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Visitor;

/**
 * Represents a filter expression node that applies a filter to an expression.
 * <p>
 * The filter is identified by its name and is applied to the left-hand expression.
 * Optional arguments for the filter can be provided.
 * </p>
 */
public class FilterNode extends AbstractExpressionNode {

    private final String         name;
    private       ExpressionNode left;
    private       ExpressionNode arguments;

    /**
     * Constructs a new FilterNode with the specified filter name.
     *
     * @param name the name of the filter
     */
    public FilterNode(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the filter.
     *
     * @return the filter name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the arguments node for the filter.
     *
     * @return the arguments expression node, or {@code null} if none is set
     */
    public ExpressionNode getArguments() {
        return arguments;
    }

    /**
     * Sets the arguments node for the filter.
     *
     * @param arguments the arguments expression node to set
     */
    public void setArguments(ExpressionNode arguments) {
        this.arguments = arguments;
    }

    /**
     * Returns the left-hand expression to which the filter is applied.
     *
     * @return the left expression node
     */
    public ExpressionNode getLeft() {
        return left;
    }

    /**
     * Sets the left-hand expression to which the filter is applied.
     *
     * @param left the left expression node to set
     */
    public void setLeft(ExpressionNode left) {
        this.left = left;
    }

    /**
     * Evaluates the filter node by applying the filter to the left-hand expression.
     * <p>
     * It retrieves the filter by name from the extensions, evaluates the left expression,
     * processes any provided arguments, and applies the filter. If the filter is not found,
     * an EvaluationException is thrown.
     * </p>
     *
     * @param context the evaluation context
     * @return the result of filter application
     * @throws EvaluationException if the filter is not found
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        Filter    filter    = context.getExtensions().getFilter(getName());
        Arguments arguments = Arguments.empty();

        if (filter == null) {
            throw new FilterNotFoundException("Filter '%s' not found".formatted(getName()));
        }

        if (getArguments() != null) {
            Object evaluatedArguments = getArguments().evaluate(context);
            if (evaluatedArguments instanceof Object[] array) {
                arguments = Arguments.forArray(array);
            }
        }

        Object             compiled  = getLeft().evaluate(context);
        ClassTypeInspector inspector = TypeInformation.forInstance(compiled);

        return filter.apply(compiled, arguments, context, inspector);
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    /**
     * Returns a string representation of this filter node.
     *
     * @return a string in the format "filterName(leftExpression)"
     */
    @Override
    public String toString() {
        return "%s[ %s ]".formatted(name, left);
    }
}
