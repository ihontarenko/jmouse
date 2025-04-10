package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class InsetTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        return arguments.toList().contains(value);
    }

    @Override
    public String getName() {
        return "inset";
    }

}
