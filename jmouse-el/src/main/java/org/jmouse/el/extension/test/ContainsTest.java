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
            return new HashSet<>(arguments.toList()).containsAll(collection);
        }

        return arguments.toList().contains(value);
    }

    @Override
    public String getName() {
        return "contains";
    }

}
