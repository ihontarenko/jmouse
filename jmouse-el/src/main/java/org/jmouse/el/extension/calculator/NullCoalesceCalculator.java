package org.jmouse.el.extension.calculator;

import org.jmouse.el.extension.Calculator;

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class NullCoalesceCalculator implements Calculator<Object> {

    /**
     * 🏗️ Performs a calculation based on the given operator.
     *
     * @param operands 📦 The operands for the operation
     * @return 🔢 The result of the calculation
     */
    @Override
    public Object calculate(Object... operands) {
        Object result = null;

        if (operands.length == 1) {
            result = operands[0];
        } else if (operands.length == 2) {
            result = operands[0] == null ? operands[1] : operands[0];
        }

        return result;
    }

}
