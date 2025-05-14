package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

/**
 * A filter that converts the input string to uppercase.
 * <p>
 * If the input is an instance of {@code String}, this filter returns the string in uppercase.
 * Otherwise, it returns the input unchanged.
 * </p>
 *
 * @author Ivan Hontarenko
 * @author Mr. Jerry Mouse
 * @author ihontarenko@gmail.com
 */
public class UpperFilter extends AbstractFilter {

    /**
     * Applies the filter to convert the input string to uppercase.
     *
     * @param input     the value to process; if it is a string, it is converted to uppercase
     * @param arguments filter arguments (unused)
     * @param context   the evaluation context (unused)
     * @param type      the class type inspector (unused)
     * @return the uppercase string if input is a string, or the original input otherwise
     */
    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        Object result = input;

        if (input instanceof String string) {
            result = string.toUpperCase();
        }

        return result;
    }

    /**
     * Returns the name of this filter.
     *
     * @return the string "upper"
     */
    @Override
    public String getName() {
        return "upper";
    }
}
