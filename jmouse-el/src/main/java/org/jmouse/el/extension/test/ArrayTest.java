package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class ArrayTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        return value instanceof Object[];
    }

    @Override
    public String getName() {
        return "array";
    }

}
