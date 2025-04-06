package org.jmouse.el.core.node.expression;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.evaluation.EvaluationException;
import org.jmouse.el.core.extension.Arguments;
import org.jmouse.el.core.extension.Function;
import org.jmouse.el.core.node.AbstractExpressionNode;
import org.jmouse.el.core.node.ExpressionNode;

/**
 * Represents a function call expression node.
 * <p>
 * This node retrieves a function by name from the extension container, evaluates its arguments,
 * and then executes the function with those arguments in the given evaluation context.
 * </p>
 */
public class FunctionNode extends AbstractExpressionNode {

    private final String         name;
    private       ExpressionNode arguments;

    /**
     * Constructs a FunctionNode with the specified function name.
     *
     * @param name the name of the function to be called
     */
    public FunctionNode(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the function.
     *
     * @return the function name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the expression node representing the function arguments.
     *
     * @return the arguments node, or {@code null} if no arguments are provided
     */
    public ExpressionNode getArguments() {
        return arguments;
    }

    /**
     * Sets the expression node representing the function arguments.
     *
     * @param arguments the arguments node to set
     */
    public void setArguments(ExpressionNode arguments) {
        this.arguments = arguments;
    }

    /**
     * Evaluates the function call.
     * <p>
     * This method retrieves the {@link Function} from the extension container using the function name.
     * It then evaluates the arguments (if any) and passes them to the function's execute method.
     * If the function is not found, an {@link EvaluationException} is thrown.
     * </p>
     *
     * @param context the evaluation context
     * @return the result of executing the function
     * @throws EvaluationException if the function cannot be found
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        Function  function  = context.getExtensions().getFunction(getName());
        Arguments arguments = Arguments.empty();

        if (function == null) {
            throw new FunctionNotFoundException("Function '%s' not found".formatted(getName()));
        }

        if (getArguments() != null) {
            Object evaluatedArguments = getArguments().evaluate(context);
            if (evaluatedArguments instanceof Object[] array) {
                arguments = Arguments.forArray(array);
            }
        }

        return function.execute(arguments, context);
    }

    /**
     * Returns a string representation of the function call.
     *
     * @return a string in the format "functionName(ARGUMENTS[n])" where n is the number of arguments
     */
    @Override
    public String toString() {
        return "%s(%s)".formatted(name, arguments);
    }
}
