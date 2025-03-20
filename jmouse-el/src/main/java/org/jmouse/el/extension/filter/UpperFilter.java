package org.jmouse.el.extension.filter;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class UpperFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context) {
        Object result = input;

        if (input instanceof String string) {
            result = string.toUpperCase();
        }

        return result;
    }

    @Override
    public String getName() {
        return "upper";
    }

}
