package org.jmouse.el.extension.filter.converter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.filter.AbstractFilter;

import java.util.List;

public class ToArrayFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        return context.getConversion().convert(input, Object[].class);
    }

    /**
     * Returns the name of this filter.
     */
    @Override
    public String getName() {
        return "array";
    }

}
