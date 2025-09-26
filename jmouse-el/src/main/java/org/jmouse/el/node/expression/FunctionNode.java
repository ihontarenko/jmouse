package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.EvaluationException;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;
import org.jmouse.el.extension.Lambda;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Visitor;

/**
 * Represents a function call expression node.
 * <p>
 * This node retrieves a function by name from the extension container, evaluates its arguments,
 * and then executes the function with those arguments in the given evaluation context.
 * </p>
 */
public class FunctionNode extends AbstractExpression {

    private final String     name;
    private       Expression arguments;

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
    public Expression getArguments() {
        return arguments;
    }

    /**
     * Sets the expression node representing the function arguments.
     *
     * @param arguments the arguments node to set
     */
    public void setArguments(Expression arguments) {
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
            if (context.getValue(getName()) instanceof Lambda lambda) {
                lambda.setName(getName());
                function = lambda;
            }

            if (function == null) {
                throw new FunctionNotFoundException(
                        "No function or lambda with name '%s' can be found.".formatted(getName()));
            }
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
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    /**
     * Returns a string representation of the function call.
     *
     * @return a string in the format "functionName(ARGUMENTS[n])" where n is the number of arguments
     */
    @Override
    public String toString() {
        return "f:%s(%s)".formatted(name, arguments == null ? "" : arguments);
    }
}
