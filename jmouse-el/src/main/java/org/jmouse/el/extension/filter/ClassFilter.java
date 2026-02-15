package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.Reflections;
import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class ClassFilter extends AbstractFilter {


    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        if (input == null) {
            return Void.class;
        }

        try {
            return Reflections.getClassFor(String.valueOf(input));
        } catch (Exception ignore) {
            return Void.class;
        }
    }

    @Override
    public String getName() {
        return "class";
    }
}
