package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.core.support.ArraySupport;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * 🚫 Returns {@code true} iff the <em>actual</em> value contains <em>none</em> of the <em>expected</em> values.
 *
 * <p>Accepted shapes & parsing are identical to {@code ContainsAny/ContainsAll}.</p>
 *
 * <p>Edge cases:</p>
 * <ul>
 *   <li>Expected is empty ⇒ {@code true} (there's nothing to match).</li>
 *   <li>Actual is empty ⇒ {@code true} (no elements present).</li>
 * </ul>
 */
public class HasNoneTest extends AbstractContainsTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        Collection<?> expected = toExpected(arguments, value);
        Collection<?> actual   = ArraySupport.toCollection(value);

        if (value instanceof String string) {
            actual = new ArrayList<>() {{ add(string); }};
        }

        if (expected.isEmpty() || actual.isEmpty()) {
            return true;
        }

        Collection<?> unique = new LinkedHashSet<>(actual);

        for (Object element : expected) {
            if (unique.contains(element)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getName() {
        return "hasNone";
    }

}
