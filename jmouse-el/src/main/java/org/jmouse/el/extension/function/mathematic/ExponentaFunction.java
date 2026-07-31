package org.jmouse.el.extension.function.mathematic;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;

public class ExponentaFunction implements Function {

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        Conversion conversion = context.getConversion();
        return Math.exp(conversion.convert(arguments.getFirst(), Double.class));
    }

    @Override
    public String getName() {
        return "e";
    }
}
