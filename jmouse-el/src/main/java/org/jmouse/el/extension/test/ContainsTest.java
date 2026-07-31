package org.jmouse.el.extension.test;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

public class ContainsTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        if (value instanceof String stringValue) {
            return stringValue.contains(
                    context.getConversion().convert(
                            arguments.getFirst(), String.class
                    )
            );
        }
        return false;
    }

    @Override
    public String getName() {
        return "contains";
    }
}
