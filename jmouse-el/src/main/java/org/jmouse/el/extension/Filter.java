package org.jmouse.el.extension;

import org.jmouse.el.evaluation.EvaluationContext;

/**
 * Represents a filter in the expression language.
 * <p>
 * A filter transforms or processes an input value based on the provided arguments and evaluation context.
 * Filters are identified by a name and can be used to modify data during expression evaluation.
 * </p>
 */
public interface Filter {

    /**
     * Applies this filter to the specified input.
     *
     * @param input     the input value to be processed by the filter
     * @param arguments the arguments that customize the filter's behavior
     * @param context   the evaluation context in which the filter is applied
     * @return the result of applying the filter to the input
     */
    Object apply(Object input, Arguments arguments, EvaluationContext context);

    /**
     * Returns the name of this filter.
     *
     * @return the filter name
     */
    String getName();
}
