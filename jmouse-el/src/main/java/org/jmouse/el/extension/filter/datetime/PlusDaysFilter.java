package org.jmouse.el.extension.filter.datetime;

import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.filter.AbstractFilter;
import org.jmouse.helpers.DateTimeHelper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;

public class PlusDaysFilter extends AbstractFilter {

    @Override
    public Object apply(Object input, Arguments arguments, EvaluationContext context, TypeClassifier type) {
        Integer daysToIncrement = context.getConversion().convert(
                arguments.getFirst(), Integer.class
        );

        if (input instanceof Instant instant) {
            return instant.plus(daysToIncrement, ChronoUnit.DAYS);
        }

        if (input instanceof String string) {
            return DateTimeHelper.parseInstant(string).plus(daysToIncrement, ChronoUnit.DAYS);
        }

        return null;
    }

    @Override
    public String getName() {
        return "plusDays";
    }

}
