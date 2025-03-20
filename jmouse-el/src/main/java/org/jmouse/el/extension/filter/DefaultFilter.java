package org.jmouse.el.extension.filter;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class DefaultFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context) {
        return input == null ? arguments.getFirst() : input;
    }

    @Override
    public String getName() {
        return "default";
    }

}
