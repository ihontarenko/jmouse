package org.jmouse.el.extension.filter;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Filter;

import java.math.BigInteger;

public class ToBigIntFilter implements Filter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context) {
        return context.getConversion().convert(input, BigInteger.class);
    }

    @Override
    public String getName() {
        return "toBigInt";
    }

}
