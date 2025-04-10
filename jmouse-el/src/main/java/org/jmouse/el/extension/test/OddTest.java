package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.evaluation.EvaluationContext;

/**
 * A test implementation that determines whether a given value is odd.
 */
public class OddTest extends AbstractTest {

    /**
     * Evaluates whether the given value satisfies the odd condition.
     */
    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        return !new EvenTest().test(value, arguments, context, type);
    }

    /**
     * Returns the name of this test.
     *
     * @return the string "odd"
     */
    @Override
    public String getName() {
        return "odd";
    }
}
