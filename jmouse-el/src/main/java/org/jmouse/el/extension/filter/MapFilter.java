package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Lambda;
import org.jmouse.util.helper.Iterables;

import java.util.stream.StreamSupport;

public class MapFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        Iterable<?> iterable = Iterables.toIterable(input);

        if (iterable == null) {
            throw new IllegalArgumentException(
                    "Filter 'map' expects an Iterable, got: " + type);
        }

        if (!(arguments.getFirst() instanceof Lambda lambda)) {
            throw new IllegalArgumentException(
                    "Filter 'map' expects a lambda argument");
        } else {
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(item -> lambda.execute(Arguments.forArray(item), context)).iterator();
        }
    }

    @Override
    public String getName() {
        return "map";
    }

}
