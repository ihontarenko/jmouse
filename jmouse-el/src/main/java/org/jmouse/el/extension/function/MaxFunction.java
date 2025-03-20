package org.jmouse.el.extension.function;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;

public class MaxFunction implements Function {

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        int a = (int) arguments.getFirst();
        int b = (int) arguments.get(1);

        return Math.max(a, b);
    }

    @Override
    public String getName() {
        return "max";
    }
}
