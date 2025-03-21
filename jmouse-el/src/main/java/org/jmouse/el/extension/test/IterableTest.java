package org.jmouse.el.extension.test;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

/**
 * Test implementation that checks if a value is an {@link Iterable}.
 * <p>
 * Returns true if the provided value implements the Iterable interface.
 * </p>
 */
public class IterableTest extends AbstractTest {

    /**
     * Tests whether the given value is an instance of {@link Iterable}.
     *
     * @param value     the object to test
     * @param arguments additional test arguments (unused)
     * @param context   the evaluation context (unused)
     * @return {@code true} if the value is an {@code Iterable}, {@code false} otherwise
     */
    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context) {
        return value instanceof Iterable;
    }

    /**
     * Returns the name of this test.
     *
     * @return the string "iterable"
     */
    @Override
    public String getName() {
        return "iterable";
    }
}
