package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.Collection;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.joining;

public class JoinFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        String separator = toString(context, arguments.getFirst());
        String result    = null;

        if (type.isCollection()) {
            result = ((Collection<?>) input).stream()
                    .map(String::valueOf).collect(joining(separator));
        } else if (type.isArray()) {
            result = String.join(separator, ((String[]) input));
        } else if (type.isIterable()) {
            result = StreamSupport.stream(((Iterable<?>) input).spliterator(), false)
                    .map(String::valueOf).collect(joining(separator));
        }

        return result;
    }

    /**
     * Returns the name of this filter.
     */
    @Override
    public String getName() {
        return "join";
    }

}
