package org.jmouse.el.extension.function.string;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;

public class UcfirstFunction implements Function {

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        String result = null;

        if (arguments.getFirst() instanceof String string) {
            result = string.substring(0, 1).toUpperCase() + string.substring(1);
        }

        return result;
    }

    @Override
    public String getName() {
        return "ucfirst";
    }
}
