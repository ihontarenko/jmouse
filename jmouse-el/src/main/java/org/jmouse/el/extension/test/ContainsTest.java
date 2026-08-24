package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

/**
 * Whether something contains something — text inside text, or an element inside a collection.
 *
 * <p>⚠️ <strong>The collection case used to answer {@code false} for everything</strong>, while
 * {@code 'smd' in tags} answered {@code true} about the same data. Two ways of asking one question, two
 * answers, and neither raised. A test that cannot answer should say so; one that returns {@code false}
 * is indistinguishable from one that looked and did not find it.</p>
 */
public class ContainsTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        if (arguments.isEmpty()) {
            return false;
        }

        if (value instanceof String text) {
            return text.contains(context.getConversion().convert(arguments.getFirst(), String.class));
        }

        Object wanted = arguments.getFirst();

        if (value instanceof java.util.Collection<?> elements) {
            return elements.stream().anyMatch(element -> java.util.Objects.equals(element, wanted));
        }

        if (value != null && value.getClass().isArray()) {
            return java.util.Arrays.stream(org.jmouse.core.support.ArraySupport.toList(value).toArray())
                    .anyMatch(element -> java.util.Objects.equals(element, wanted));
        }

        return false;
    }

    @Override
    public String getName() {
        return "contains";
    }
}
