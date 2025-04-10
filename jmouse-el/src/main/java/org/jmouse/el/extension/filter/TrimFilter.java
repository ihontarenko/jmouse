package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

/**
 * A filter that trims leading and trailing whitespace from a String.
 * <p>
 * If the input is a {@code String}, this filter returns the trimmed version of the input.
 * For non-string inputs, the filter returns the input unchanged.
 * </p>
 *
 * Note: The filter's {@link #getName()} method returns "lower", which may indicate a naming inconsistency.
 * </p>
 *
 * @author Ivan Hontarenko
 * @author Mr. Jerry Mouse
 * @author ihontarenko@gmail.com
 */
public class TrimFilter extends AbstractFilter {

    /**
     * Trims the input string if it is a {@code String}; otherwise, returns the input unchanged.
     *
     * @param input     the value to be processed
     * @param arguments the filter arguments (ignored in this implementation)
     * @param context   the evaluation context providing conversion services
     * @param type      the class type inspector (unused)
     * @return the trimmed string if input is a {@code String}, or the original input otherwise
     */
    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        Object result = input;

        if (input instanceof String string) {
            result = string.trim();
        }

        return result;
    }

    /**
     * Returns the name of this filter.
     * <p>
     * Although the filter trims whitespace, the name returned is "trim".
     * </p>
     *
     * @return the string "lower"
     */
    @Override
    public String getName() {
        return "trim";
    }
}
