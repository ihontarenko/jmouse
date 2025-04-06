package org.jmouse.el.core.extension.function;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.extension.Arguments;
import org.jmouse.el.core.extension.Function;

/**
 * Implements the "set" function for the expression language.
 * <p>
 * This function assigns a value to a variable within the evaluation context.
 * It expects at least two arguments:
 * <ul>
 *   <li>The first argument is the name of the variable (as a String).</li>
 *   <li>The second argument is the value to be assigned.</li>
 * </ul>
 * After setting the variable, it returns the value that was assigned.
 * </p>
 */
public class SetFunction implements Function {

    /**
     * Executes the set function.
     * <p>
     * Retrieves the variable name from the first argument, sets its value (the second argument)
     * in the evaluation context, and returns the assigned value.
     * </p>
     *
     * @param arguments the arguments passed to the function; the first should be the variable name,
     *                  and the second should be the value to assign
     * @param context   the evaluation context where the variable value is set
     * @return the value that was assigned to the variable
     */
    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        String name = String.valueOf(arguments.getFirst());
        context.setValue(name, arguments.get(1));
        return arguments.get(1);
    }

    /**
     * Returns the name of the function.
     *
     * @return the string "set"
     */
    @Override
    public String getName() {
        return "set";
    }
}
