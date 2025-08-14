package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class LastFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        Object last = null;

        if (type.isCollection()) {
            last = new ArrayList<>((Collection<?>)input).getLast();
        } else if (type.isMap()) {
            last = new ArrayList<>(((Map<?, ?>)input).values()).getLast();
        } else if (input instanceof String string) {
            last = string.substring(string.length() - 1);
        } else if (input instanceof Object[] array && array.length > 0) {
            last = Array.get(array, array.length - 1);
        }

        return last;
    }

    @Override
    public String getName() {
        return "last";
    }
}
