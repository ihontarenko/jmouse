package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.core.support.ArraySupport;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * ✅ Returns {@code true} if the <em>actual</em> value contains <em>any</em> of the <em>expected</em> values.
 *
 * <p>Accepted shapes:</p>
 * <ul>
 *   <li>Actual: {@code Collection}, array, {@code CharSequence} (treated as characters), or scalar.</li>
 *   <li>Expected: multiple args, or a single arg that is a {@code Collection}/array/{@code CharSequence}.</li>
 * </ul>
 *
 * <p>Examples:</p>
 * <pre>
 * containsAny(value: List.of("a","b","c"),  "x","b")  => true
 * containsAny(value: "abc",                 "x","b")  => true
 * containsAny(value: "abc",                 List.of("x","y")) => false
 * containsAny(value: 42,                    1, 2, 42) => true
 * </pre>
 */
public class ContainsAny extends AbstractContainsTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        Collection<?> expected = toExpected(arguments);
        Collection<?> actual   = ArraySupport.toCollection(value);

        if (expected.isEmpty() || actual.isEmpty()) {
            return false;
        }

        Collection<?> unique = new LinkedHashSet<>(actual);

        for (Object element : expected) {
            if (unique.contains(element)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return "containsAny";
    }



}
