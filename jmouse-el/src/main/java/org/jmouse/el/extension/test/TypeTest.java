package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.Collection;

/**
 * Test implementation that checks if a value is an {@link Iterable}.
 * <p>
 * Returns true if the provided value implements the Iterable interface.
 * </p>
 */
public class TypeTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        boolean result = false;

        if (arguments.getFirst() instanceof String string) {
            DataType dataType = DataType.valueOf(string.toUpperCase());

            result = switch (dataType) {
                case STRING -> value instanceof String;
                case NUMERIC -> value instanceof Number;
                case ITERABLE -> value instanceof Iterable<?>;
                case COLLECTION -> value instanceof Collection<?>;
                case ARRAY -> value instanceof Object[];
            };
        }

        return result;
    }

    /**
     * Returns the name of this test.
     *
     * @return the string "iterable"
     */
    @Override
    public String getName() {
        return "type";
    }

    enum DataType {
        STRING, NUMERIC, ITERABLE, COLLECTION, ARRAY
    }

}
