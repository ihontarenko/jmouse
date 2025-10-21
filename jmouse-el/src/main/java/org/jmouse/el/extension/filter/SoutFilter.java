package org.jmouse.el.extension.filter;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.Iterator;

import static java.lang.System.out;

public class SoutFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        if (input instanceof String string) {
            out.println(string);
        } else if (input instanceof Iterator<?> iterator) {
            while (iterator.hasNext()) {
                out.println(iterator.next());
            }
        }

        return null;
    }

    @Override
    public String getName() {
        return "sout";
    }
}
