package org.jmouse.el.core.extension.test;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.extension.Arguments;

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
