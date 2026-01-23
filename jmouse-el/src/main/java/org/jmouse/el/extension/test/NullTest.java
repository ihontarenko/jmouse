package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class NullTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        return value == null;
    }

    /**
     * Returns the name of this test extension.
     *
     * @return the name of the test extension
     */
    @Override
    public String getName() {
        return "null";
    }
}
