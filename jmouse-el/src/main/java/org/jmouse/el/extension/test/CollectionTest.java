package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

/**
 * Test implementation that checks if a value is an {@link Iterable}.
 * <p>
 * Returns true if the provided value implements the Iterable interface.
 * </p>
 */
public class CollectionTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        return type.isCollection();
    }

    /**
     * Returns the name of this test.
     *
     * @return the string "iterable"
     */
    @Override
    public String getName() {
        return "collection";
    }
}
