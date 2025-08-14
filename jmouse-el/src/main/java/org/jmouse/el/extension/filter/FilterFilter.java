package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Lambda;
import org.jmouse.util.Iterables;

import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public class FilterFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        Iterable<?> iterable = Iterables.toIterable(input);

        if (iterable == null) {
            throw new IllegalArgumentException(
                    "Filter 'filter' expects an Iterable, got: " + type);
        }

        if (!(arguments.getFirst() instanceof Lambda lambda)) {
            throw new IllegalArgumentException(
                    "Filter 'filter' expects a lambda argument");
        } else {
            Predicate<Object> predicate = item
                    -> Boolean.TRUE.equals(lambda.execute(Arguments.forArray(item), context));
            return StreamSupport.stream(iterable.spliterator(), false)
                    .filter(predicate).iterator();
        }
    }

    @Override
    public String getName() {
        return "filter";
    }

}
