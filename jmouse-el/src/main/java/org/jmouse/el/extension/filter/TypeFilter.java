package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class TypeFilter extends AbstractFilter {

    /**
     * Applies this filter to the specified input.
     */
    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        return input == null ? "NULL" : InferredType.forInstance(input).getRawType().getName();
    }

    /**
     * Returns the name of this filter.
     */
    @Override
    public String getName() {
        return "type";
    }
}
