package org.jmouse.el.core.extension.test;

import org.jmouse.el.core.extension.Arguments;
import org.jmouse.el.core.evaluation.EvaluationContext;

/**
 * A test implementation that determines whether a given value is odd.
 */
public class OddTest extends AbstractTest {

    /**
     * Evaluates whether the given value satisfies the odd condition.
     */
    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context) {
        return !new EvenTest().test(value, arguments, context);
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
