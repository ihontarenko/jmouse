package org.jmouse.el.extension.function.string;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;

public class UclastFunction implements Function {

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        String result = null;

        if (arguments.getFirst() instanceof String string) {
            int length = string.length();
            result = string.substring(0, length - 1) + string.substring(length - 1, length).toUpperCase();
        }

        return result;
    }

    @Override
    public String getName() {
        return "uclast";
    }
}
