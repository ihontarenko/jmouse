package org.jmouse.el.extension.calculator;

import org.jmouse.el.extension.Calculator;

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class ConcatCalculator implements Calculator<String> {

    /**
     * 🏗️ Performs a calculation based on the given operator.
     *
     * @param operands 📦 The operands for the operation
     * @return 🔢 The result of the calculation
     */
    @Override
    public String calculate(Object... operands) {
        String result = "";

        if (operands != null && operands.length > 0) {
            result = Stream.of(operands).map(String::valueOf).collect(joining());
        }

        return result;
    }

}
