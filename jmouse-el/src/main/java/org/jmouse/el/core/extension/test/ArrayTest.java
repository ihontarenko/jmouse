package org.jmouse.el.core.extension.test;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.core.extension.Arguments;

public class ArrayTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context) {
        return value instanceof Object[];
    }

    @Override
    public String getName() {
        return "array";
    }

}
