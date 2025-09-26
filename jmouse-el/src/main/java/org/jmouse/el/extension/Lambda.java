package org.jmouse.el.extension;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ScopedChain;
import org.jmouse.el.node.Expression;

import java.util.*;

/**
 * Represents a user-defined lambda function with named parameters, default values,
 * and an expression body. The lambda captures its execution context using a scoped chain.
 * <p>
 * Example:
 * <pre>{@code
 *   Lambda lambda = new Lambda(bodyNode);
 *   lambda.addParameter("x");
 *   lambda.setDefault("x", 10);
 *   Object result = lambda.execute(new Arguments(5), context);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Lambda implements Function {

    private final List<String>        parameters    = new ArrayList<>();
    private final Map<String, Object> defaultValues = new HashMap<>();
    private final Expression          body;
    private       String              name;

    /**
     * Constructs a new Lambda with the given expression node as its body.
     * @param body the AST node representing the function body to evaluate when invoked
     */
    public Lambda(Expression body) {
        this.body = body;
    }

    /**
     * Executes the lambda by binding supplied arguments to parameter names,
     * using default values when an argument is null, evaluating the body,
     * and then restoring the previous scope.
     *
     * @param arguments a collection of argument values for the lambda parameters
     * @param context   the evaluation context providing scoped variable support
     * @return the result of evaluating the lambda body
     */
    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        ScopedChain chain = context.getScopedChain();

        chain.push();  // enter new scope

        int index = 0;

        for (String parameter : parameters) {
            Object value = arguments.get(index);

            if (value == null) {
                value = getDefault(parameter);

                if (value == null) {
                    throw new IllegalLambdaExecutionException(
                            "Lambda function '%s' requires parameter '%s'"
                                    .formatted(getName(), parameter));
                }
            }

            chain.setValue(parameter, value);

            index++;
        }

        Object result = null;

        if (body != null) {
            result = body.evaluate(context);
        }

        chain.pop();  // restore previous scope

        return result;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the default value for a given parameter name.
     * @param name the parameter name
     * @return the default value or null if not set
     */
    public Object getDefault(String name) {
        return defaultValues.get(name);
    }

    /**
     * Sets a default value for a given parameter name.
     * @param name  the parameter name
     * @param value the default value to use when argument is null
     */
    public void setDefault(String name, Object value) {
        defaultValues.put(name, value);
    }

    /**
     * Adds a parameter name to this lambda's signature.
     * @param name the parameter name to add
     */
    public void addParameter(String name) {
        parameters.add(name);
    }

    /**
     * Returns the list of parameter names in declaration order.
     * @return an unmodifiable list of parameter names (insertion order)
     */
    public List<String> getParameters() {
        return List.copyOf(parameters);
    }

    /**
     * The name used to reference this function in the expression language.
     * @return the literal string "lambda"
     */
    @Override
    public String getName() {
        return name == null ? "UNNAMED_LAMBDA" : name;
    }

    /**
     * Returns a string representation showing parameter names.
     * @return a string in the form "LAMBDA[param1, param2, ...]"
     */
    @Override
    public String toString() {
        return "LAMBDA" + parameters;
    }
}
