package org.jmouse.el.extension.function.datetime;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Function;

import java.time.Instant;

public class NowFunction implements Function {

    @Override
    public Object execute(Arguments arguments, EvaluationContext context) {
        return Instant.now();
    }

    @Override
    public String getName() {
        return "now";
    }
}
