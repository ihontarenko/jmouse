package org.jmouse.el.extension.test;

import org.jmouse.el.extension.Arguments;
import org.jmouse.el.evaluation.EvaluationContext;

/**
 * A test implementation that determines whether a given value is odd.
 */
public class OddTest extends AbstractTest {

    /**
     * Evaluates whether the given instance satisfies the odd condition.
     */
    @Override
    public boolean test(Object instance, Arguments arguments, EvaluationContext context) {
        return !new EvenTest().test(instance, arguments, context);
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
