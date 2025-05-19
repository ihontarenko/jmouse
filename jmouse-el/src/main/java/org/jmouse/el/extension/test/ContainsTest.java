package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.Collection;
import java.util.HashSet;

public class ContainsTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        if (value instanceof Collection<?> collection) {
            if (arguments.getFirst() instanceof Collection<?> needed) {
                return new HashSet<>(collection).containsAll(needed);
            }

            return new HashSet<>(collection).contains(arguments.getFirst());
        } else if (value instanceof String string) {
            return string.contains(context.getConversion().convert(arguments.getFirst(), String.class));
        }

        return arguments.toList().contains(value);
    }

    @Override
    public String getName() {
        return "contains";
    }

}
