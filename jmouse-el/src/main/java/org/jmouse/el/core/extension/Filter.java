package org.jmouse.el.core.extension;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.core.evaluation.EvaluationContext;

/**
 * Represents a filter in the expression language.
 */
public interface Filter {

    /**
     * Applies this filter to the specified input.
     */
    Object apply(Object input, Arguments arguments, ClassTypeInspector inspector, EvaluationContext context);

    /**
     * Returns the name of this filter.
     */
    String getName();
}
