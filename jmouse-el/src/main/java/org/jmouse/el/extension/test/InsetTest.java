package org.jmouse.el.extension.test;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class InsetTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context) {
        return arguments.toList().contains(value);
    }

    @Override
    public String getName() {
        return "inset";
    }

}
