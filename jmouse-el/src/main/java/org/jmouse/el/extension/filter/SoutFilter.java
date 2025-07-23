package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class SoutFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        if (input instanceof String string) {
            System.out.println(string);
        }

        return null;
    }

    @Override
    public String getName() {
        return "sout";
    }
}
