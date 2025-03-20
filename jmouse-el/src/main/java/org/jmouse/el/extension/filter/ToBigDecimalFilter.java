package org.jmouse.el.extension.filter;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Filter;

import java.math.BigDecimal;

public class ToBigDecimalFilter implements Filter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context) {
        return context.getConversion().convert(input, BigDecimal.class);
    }

    @Override
    public String getName() {
        return "toBigDecimal";
    }

}
