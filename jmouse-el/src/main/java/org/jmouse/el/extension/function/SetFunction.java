package org.jmouse.el.extension.function;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;

public class SetFunction implements Function {

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        String name = String.valueOf(arguments.getFirst());
        context.setValue(name, arguments.get(1));
        return arguments.get(1);
    }

    @Override
    public String getName() {
        return "set";
    }

}
