package org.jmouse.el.extension.filter;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class SubFilter extends AbstractFilter {

    /**
     * Applies this filter to the specified input.
     *
     * @param input     the input value to be processed by the filter
     * @param arguments the arguments that customize the filter's behavior
     * @param context   the evaluation context in which the filter is applied
     * @return the result of applying the filter to the input
     */
    @Override
    public Object apply(Object input, Arguments arguments, ClassTypeInspector inspector, EvaluationContext context) {
        String     value      = input.toString();
        Conversion conversion = context.getConversion();

        if (arguments.size() == 1) {
            int begin = conversion.convert(arguments.getFirst(), int.class);
            value = value.substring(begin);
        } else if (arguments.size() == 2) {
            int begin = conversion.convert(arguments.getFirst(), int.class);
            int end   = conversion.convert(arguments.getLast(), int.class);
            value = value.substring(begin, end);
        }

        return value;
    }

    /**
     * Returns the name of this filter.
     *
     * @return the filter name
     */
    @Override
    public String getName() {
        return "sub";
    }

}
