package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

/**
 * A filter that provides a default value.
 * <p>
 * This filter returns the first argument from the provided arguments if the input is {@code null}.
 * Otherwise, it returns the original input unchanged.
 * This allows expressions to specify a default value when no input is provided.
 * </p>
 *
 * @author Ivan Hontarenko
 * @author Mr. Jerry Mouse
 * @author ihontarenko@gmail.com
 */
public class DefaultFilter extends AbstractFilter {

    /**
     * Applies the default filter.
     * <p>
     * If the input is {@code null}, the filter returns the first argument from the provided arguments.
     * If the input is not {@code null}, it is returned unchanged.
     * </p>
     *
     * @param input     the value to process; may be {@code null}
     * @param arguments the arguments that may contain a default value
     * @param inspector the class type inspector (unused in this filter)
     * @param context   the evaluation context (unused in this filter)
     * @return the original input if not {@code null}, otherwise the first argument
     */
    @Override
    public Object apply(Object input, Arguments arguments, ClassTypeInspector inspector, EvaluationContext context) {
        return input == null ? arguments.getFirst() : input;
    }

    /**
     * Returns the name of this filter.
     *
     * @return the string "default"
     */
    @Override
    public String getName() {
        return "default";
    }
}
