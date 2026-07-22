package org.jmouse.el.extension.function;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;

public class GetFunction implements Function {

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        String name  = String.valueOf(arguments.getFirst());
        return context.getValue(name);
    }

    @Override
    public String getName() {
        return "get";
    }
}
