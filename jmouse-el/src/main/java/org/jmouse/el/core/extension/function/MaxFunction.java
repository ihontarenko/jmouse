package org.jmouse.el.core.extension.function;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.extension.Arguments;
import org.jmouse.el.core.extension.Function;

public class MaxFunction implements Function {

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        Conversion conversion = context.getConversion();

        int a = conversion.convert(arguments.getFirst(), int.class);
        int b = conversion.convert(arguments.get(1), int.class);

        return Math.max(a, b);
    }

    @Override
    public String getName() {
        return "max";
    }
}
