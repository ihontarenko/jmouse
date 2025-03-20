package org.jmouse.el.extension;

import org.jmouse.el.evaluation.EvaluationContext;

public interface Filter {

    Object apply(Object input, Arguments arguments, EvaluationContext context);

    String getName();

}
