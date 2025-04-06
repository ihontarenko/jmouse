package org.jmouse.el.core.extension.filter;

import org.jmouse.el.core.extension.Filter;

/**
 * An abstract base class for filters.
 * <p>
 * This class provides a default {@code toString()} implementation that returns a string
 * in the format "Filter: {filterName}". Subclasses should implement {@link Filter#getName()}
 * and {@link Filter#apply(Object, org.jmouse.el.core.extension.Arguments, org.jmouse.core.reflection.ClassTypeInspector, org.jmouse.el.core.evaluation.EvaluationContext)}
 * to provide specific filtering behavior.
 * </p>
 *
 * @author Ivan Hontarenko
 * @author Mr. Jerry Mouse
 * @author ihontarenko@gmail.com
 */
abstract public class AbstractFilter implements Filter {

    /**
     * Returns a string representation of this filter.
     * <p>
     * The format is "Filter: {filterName}", where {filterName} is the name provided
     * by {@link #getName()}.
     * </p>
     *
     * @return a string representation of this filter
     */
    @Override
    public String toString() {
        return "Filter: " + getName();
    }
}
