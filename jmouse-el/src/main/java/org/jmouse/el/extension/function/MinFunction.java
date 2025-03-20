package org.jmouse.el.extension.function;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;

public class MinFunction implements Function {

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        Conversion conversion = context.getConversion();

        int a = conversion.convert(arguments.getFirst(), int.class);
        int b = conversion.convert(arguments.get(1), int.class);

        return Math.min(a, b);
    }

    @Override
    public String getName() {
        return "min";
    }
}
