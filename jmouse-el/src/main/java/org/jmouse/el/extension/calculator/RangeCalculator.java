package org.jmouse.el.extension.calculator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.calculator.operation.CalculationOperationException;

import java.util.Arrays;
import java.util.stream.IntStream;

public class RangeCalculator implements Calculator<Object> {

    /**
     * 🏗️ Performs a calculation based on the given operator.
     *
     * @param operands 📦 The operands for the operation
     * @return 🔢 The result of the calculation
     */
    @Override
    public Object calculate(Object... operands) {
        if (operands.length == 2) {
            if (operands[0] instanceof Number x && operands[1] instanceof Number y) {
                return (Iterable<Integer>) () -> IntStream.rangeClosed(x.intValue(), y.intValue()).boxed().iterator();
            } else if (operands[0] instanceof String x && operands[1] instanceof String y) {
                char      cx = x.charAt(0);
                char      cy = y.charAt(0);
                IntStream points;

                if (cx < cy) {
                    points = IntStream.rangeClosed(cx, cy);
                } else {
                    points = IntStream.iterate(cx, point -> point - 1).limit(cx - cy + 1);
                }

                return (Iterable<String>) () -> points.mapToObj(Character::toString).iterator();
            }
        }

        throw new CalculationOperationException(
                "Illegal operands '%s' for range calculation.".formatted(Arrays.toString(operands)));
    }

}
