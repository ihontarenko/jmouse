package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.core.support.ArraySupport;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * ✅ Returns {@code true} iff the <em>actual</em> value contains <em>all</em> of the <em>expected</em> values.
 *
 * <p>Accepted shapes:</p>
 * <ul>
 *   <li>Actual: {@code Collection}, array, {@code CharSequence} (as characters), or scalar.</li>
 *   <li>Expected: multiple args, or a single arg that is a {@code Collection}/array/{@code CharSequence}.</li>
 * </ul>
 *
 * <p>Edge cases:</p>
 * <ul>
 *   <li>Expected is empty ⇒ {@code true} (vacuously all elements are contained).</li>
 *   <li>Actual is empty and expected non-empty ⇒ {@code false}.</li>
 * </ul>
 */
public class ContainsAll extends AbstractContainsTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        Collection<?> expected = toExpected(arguments);
        Collection<?> actual   = ArraySupport.toCollection(value);

        if (expected.isEmpty()) {
            return true;
        }

        if (actual.isEmpty()) {
            return false;
        }

        Collection<?> unique = new LinkedHashSet<>(actual);

        for (Object element : expected) {
            if (!unique.contains(element)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getName() {
        return "containsAll";
    }

}
