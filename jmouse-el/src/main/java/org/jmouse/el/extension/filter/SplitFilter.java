package org.jmouse.el.extension.filter;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class SplitFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        Object[] result = new Object[0];

        if (input instanceof String string) {
            Conversion conversion = context.getConversion();
            result = string.split(conversion.convert(arguments.getFirst(), String.class));
        }

        return result;
    }

    /**
     * Returns the name of this filter.
     */
    @Override
    public String getName() {
        return "split";
    }
}
