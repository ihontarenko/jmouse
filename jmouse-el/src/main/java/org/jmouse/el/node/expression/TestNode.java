package org.jmouse.el.node.expression;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Test;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.node.Visitor;

/**
 * Represents a test expression node in the expression language.
 */
public class TestNode extends AbstractExpressionNode {

    private final String         name;
    private       ExpressionNode arguments;
    private       ExpressionNode left;
    private       boolean        negated;

    /**
     * Constructs a new TestNode with the specified test name.
     *
     * @param name the name of the test to be applied (e.g., "even", "odd")
     */
    public TestNode(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the test.
     *
     * @return the test name as a String
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the expression node representing the arguments for the test.
     *
     * @return the arguments ExpressionNode, or {@code null} if none are provided
     */
    public ExpressionNode getArguments() {
        return arguments;
    }

    /**
     * Sets the expression node representing the arguments for the test.
     *
     * @param arguments the arguments ExpressionNode to set
     */
    public void setArguments(ExpressionNode arguments) {
        this.arguments = arguments;
    }

    /**
     * Returns whether the test result is negated.
     *
     * @return {@code true} if the test result should be negated; {@code false} otherwise
     */
    public boolean isNegated() {
        return negated;
    }

    /**
     * Sets whether the test result should be negated.
     *
     * @param negated {@code true} to negate the test result; {@code false} to use the test result as is
     */
    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    /**
     * Returns the left-hand expression node whose evaluated value is the subject of the test.
     *
     * @return the left ExpressionNode
     */
    public ExpressionNode getLeft() {
        return left;
    }

    /**
     * Sets the left-hand expression node whose evaluated value is the subject of the test.
     *
     * @param left the left ExpressionNode to set
     */
    public void setLeft(ExpressionNode left) {
        this.left = left;
    }

    /**
     * Evaluates the test node within the given evaluation context.
     * <p>
     * This method evaluates the left-hand expression to obtain the instance to be tested.
     * If the node has an associated arguments expression, it evaluates that to obtain an array of arguments,
     * which are then wrapped into an {@link Arguments} instance.
     * The appropriate {@link Test} is retrieved using the test name from the extension container,
     * and the test is applied to the instance and arguments. Finally, if negation is enabled,
     * the result is inverted.
     * </p>
     *
     * @param context the evaluation context providing scope, conversion, and extension services
     * @return the boolean result of the test evaluation (as an {@code Object})
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        Arguments arguments = Arguments.empty();
        Test      test      = context.getExtensions().getTest(getName());
        Object    instance  = getLeft().evaluate(context);

        if (test == null) {
            throw new TestNotFoundException("Test '%s' not found".formatted(getName()));
        }

        if (getArguments() != null) {
            Object evaluatedArguments = getArguments().evaluate(context);
            if (evaluatedArguments instanceof Object[] array) {
                arguments = Arguments.forArray(array);
            }
        }

        ClassTypeInspector type = TypeInformation.forInstance(instance);

        // The test result is negated if the 'negated' flag is true.
        return isNegated() != test.test(instance, arguments, context, type);
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        left.accept(visitor);

        if (arguments != null) {
            arguments.accept(visitor);
        }
    }

    /**
     * Returns a string representation of the test node.
     * <p>
     * The representation includes the left-hand expression, whether the test is negated,
     * and the test name.
     * </p>
     *
     * @return a formatted string representing the test node
     */
    @Override
    public String toString() {
        return "IS %s %s%s(%s)".formatted(left, negated ? "NOT " : "", name, arguments);
    }
}
