package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.Collection;
import java.util.StringJoiner;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.joining;

public class JoinFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        String separator = toString(context, arguments.getFirst());
        String result    = null;
        String prefix    = "";
        String suffix    = "";

        if (arguments.get(1) instanceof String before) {
            prefix = before;
        }

        if (arguments.get(2) instanceof String after) {
            suffix = after;
        }

        if (type.isIterable()) {
            result = StreamSupport.stream(((Iterable<?>) input).spliterator(), false)
                    .map(String::valueOf).collect(joining(separator, prefix, suffix));
        } else if (type.isArray()) {
            StringJoiner joiner = new StringJoiner(separator, prefix, suffix);

            if (input instanceof Object[] objects) {
                for (Object value : objects) {
                    joiner.add(String.valueOf(value));
                }
            }

            result = joiner.toString();
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
