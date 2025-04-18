package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.util.helper.Iterables;

public class LengthFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        int length = -1;

        if (input != null) {
            length = Iterables.size(Iterables.toIterable(input));
        }

        return length;
    }

    @Override
    public String getName() {
        return "length";
    }
}
