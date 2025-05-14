package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class FirstFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        Object first = null;

        if (type.isCollection()) {
            first = new ArrayList<>((Collection<?>)input).getFirst();
        } else if (type.isMap()) {
            first = new ArrayList<>(((Map<?, ?>)input).values()).getFirst();
        } else if (input instanceof String string) {
            first = string.substring(0, 1);
        } else if (input instanceof Object[] array && array.length > 0) {
            first = Array.get(array, 0);
        }

        return first;
    }

    @Override
    public String getName() {
        return "first";
    }
}
