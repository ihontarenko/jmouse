package org.jmouse.el.core.extension;

import org.jmouse.el.core.evaluation.EvaluationContext;

/**
 * Represents a function within the expression language.
 * <p>
 * A function is identified by its name and can be executed with a set of arguments
 * in a given evaluation context.
 * </p>
 */
public interface Function {

    /**
     * Executes this function with the given arguments and evaluation context.
     *
     * @param arguments the arguments provided to the function
     * @param context   the evaluation context in which the function is executed
     * @return the result of the function execution
     */
    Object execute(Arguments arguments, EvaluationContext context);

    /**
     * Returns the name of this function.
     *
     * @return the function name
     */
    String getName();
}
