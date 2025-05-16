package org.jmouse.el.extension.test;

import org.jmouse.core.convert.Conversion;
import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * A test implementation that determines whether a given value is even.
 */
public class EvenTest extends AbstractTest {

    /**
     * Evaluates whether the provided instance represents an even number.
     */
    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, ClassTypeInspector type) {
        Conversion conversion = context.getConversion();
        boolean    isEven     = false;

        if (value instanceof Number number) {
            isEven = (number.longValue() % 2) == 0;
        } else if (type.isScalar()) {
            isEven = (conversion.convert(value, Double.class) % 2) == 0;
        } else if (type.isArray()) {
            isEven = (Array.getLength(value) & 1) == 0;
        } else if (type.isCollection()) {
            isEven = (((Collection<?>) value).size() & 1) == 0;
        } else if (type.isMap()) {
            isEven = (((Map<?, ?>)value).size() & 1) == 0;
        }

        return isEven;
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
