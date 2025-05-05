package org.jmouse.el.extension.calculator.mathematic;

import org.jmouse.el.extension.calculator.MathematicOperation;
import org.jmouse.util.helper.Maths;

abstract public class NumberOperation<N extends Number> implements MathematicOperation<N> {

    @Override
    public Object plus(N primary, Object operand) {
        if (operand instanceof Number number) {
            return Maths.sum(primary, number);
        }

        throw new MathematicOperationException(
                "Addition not supported for '%s' and '%s'".formatted(primary, operand));
    }

    @Override
    public Object minus(N primary, Object operand) {
        if (operand instanceof Number number) {
            return Maths.subtract(primary, number);
        }

        throw new MathematicOperationException(
                "Subtraction not supported for '%s' and '%s'".formatted(primary, operand));
    }

    @Override
    public Object divide(N primary, Object operand) {
        if (operand instanceof Number number) {
            return Maths.divide(primary, number);
        }

        throw new MathematicOperationException(
                "Division not supported for '%s' and '%s'".formatted(primary, operand));
    }

    @Override
    public Object multiply(N primary, Object operand) {
        if (operand instanceof Number number) {
            return Maths.multiply(primary, number);
        }

        throw new MathematicOperationException(
                "Multiplication not supported for '%s' and '%s'".formatted(primary, operand));
    }

    @Override
    public Object exponential(N primary, Object operand) {
        if (operand instanceof Number number) {
           Maths.power(primary, number);
        }

        throw new MathematicOperationException(
                "Exponentiation not supported for '%s' and '%s'".formatted(primary, operand));
    }

    @Override
    public Object modulus(N primary, Object operand) {
        if (operand instanceof Number number) {
            Maths.modulus(primary, number);
        }

        throw new MathematicOperationException(
                "Modulus not supported for '%s' and '%s'".formatted(primary, operand));
    }
}
