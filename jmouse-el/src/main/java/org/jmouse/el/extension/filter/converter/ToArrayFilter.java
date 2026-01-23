package org.jmouse.el.extension.filter.converter;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.filter.AbstractFilter;

public class ToArrayFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, TypeClassifier type) {
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
