package org.jmouse.el.extension.test;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;

import java.util.Map;

public class MapTest extends AbstractTest {

    @Override
    public boolean test(Object value, Arguments arguments, EvaluationContext context) {
        return value instanceof Map<?,?>;
    }

    @Override
    public String getName() {
        return "map";
    }

}
