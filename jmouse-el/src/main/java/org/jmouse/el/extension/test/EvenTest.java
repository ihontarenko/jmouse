package org.jmouse.el.extension.test;

import org.jmouse.el.extension.Arguments;
import org.jmouse.el.evaluation.EvaluationContext;

/**
 * A test implementation that determines whether a given value is even.
 */
public class EvenTest extends AbstractTest {

    /**
     * Evaluates whether the provided instance represents an even number.
     */
    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context) {
        Double number = context.getConversion().convert(value, Double.class);
        return number % 2 == 0;
    }

    /**
     * Returns the name of this test implementation.
     *
     * @return the string "even"
     */
    @Override
    public String getName() {
        return "even";
    }
}
