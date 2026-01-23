package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class StartsTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        if (value instanceof String string && arguments.getFirst() instanceof String prefix) {
            return string.startsWith(prefix);
        }

        throw new IllegalArgumentException("Test 'starts' expects an String, got: " + type);
    }

    @Override
    public String getName() {
        return "starts";
    }
}
