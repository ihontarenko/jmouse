package org.jmouse.el.extension;

import org.jmouse.el.evaluation.EvaluationContext;

/**
 * Represents a filter in the expression language.
 */
public interface Filter {

    /**
     * Applies this filter to the specified input.
     */
    Object apply(Object input, Arguments arguments, EvaluationContext context);

    /**
     * Returns the name of this filter.
     */
    String getName();
}
