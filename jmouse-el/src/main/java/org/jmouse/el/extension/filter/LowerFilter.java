package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

/**
 * A filter that converts the input value to lowercase.
 * <p>
 * If the input is a {@code String}, this filter returns the lowercase version of the string.
 * For any other type of input, the original value is returned.
 * </p>
 *
 * @author Ivan Hontarenko
 * @author Mr. Jerry Mouse
 * @author ihontarenko@gmail.com
 */
public class LowerFilter extends AbstractFilter {

    /**
     * Applies the filter by converting the input string to lowercase.
     * <p>
     * If the input is an instance of {@code String}, it returns {@code string.toLowerCase()}.
     * Otherwise, the input is returned unmodified.
     * </p>
     *
     * @param input     the value to process; if it is a string, it will be converted to lowercase
     * @param arguments the filter arguments (unused in this filter)
     * @param inspector the class type inspector (unused in this filter)
     * @param context   the evaluation context (unused in this filter)
     * @return the lowercase string if input is a string; otherwise, the original input
     */
    @Override
    public Object apply(Object input, Arguments arguments, ClassTypeInspector inspector, EvaluationContext context) {
        Object result = input;

        if (input instanceof String string) {
            result = string.toLowerCase();
        }

        return result;
    }

    /**
     * Returns the name of this filter.
     * <p>
     * The name is used to identify the filter in the expression language.
     * </p>
     *
     * @return the string "lower"
     */
    @Override
    public String getName() {
        return "lower";
    }

}
