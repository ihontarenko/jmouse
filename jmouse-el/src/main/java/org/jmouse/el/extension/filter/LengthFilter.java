package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

public class LengthFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        int length = -1;

        if (type.isCollection()) {
            length = ((Collection<?>)input).size();
        } else if (type.isMap()) {
            length = ((Map<?, ?>)input).size();
        } else if (type.isArray()) {
            length = Array.getLength(input);
        } else if (type.isString()) {
            length = ((String) input).length();
        }

        return length;
    }

    @Override
    public String getName() {
        return "length";
    }
}
