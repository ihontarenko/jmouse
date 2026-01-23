package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class EndsTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        if (value instanceof String string && arguments.getFirst() instanceof String suffix) {
            return string.endsWith(suffix);
        }

        throw new IllegalArgumentException("Test 'ends' expects an String, got: " + type);
    }

    @Override
    public String getName() {
        return "ends";
    }
}
