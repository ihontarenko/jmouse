package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Filter;

import java.math.BigDecimal;

/**
 * A filter that converts the input value to a {@link BigDecimal}.
 *
 * @author Ivan Hontarenko
 * @author Mr. Jerry Mouse
 * @author ihontarenko@gmail.com
 */
public class ToBigDecimalFilter implements Filter {

    /**
     * Converts the input value to a {@link BigDecimal} using the conversion service.
     */
    @Override
    public Object apply(Object input, Arguments arguments, ClassTypeInspector inspector, EvaluationContext context) {
        return context.getConversion().convert(input, BigDecimal.class);
    }

    /**
     * Returns the name of this filter (e.g. "toBigDecimal").
     */
    @Override
    public String getName() {
        return "toBigDecimal";
    }
}
