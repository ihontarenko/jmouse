package org.jmouse.el.extension.calculator;

import org.jmouse.el.extension.calculator.mathematic.MathematicOperationException;

public interface MathematicOperation<T> {
    
    default Object plus(T primary, Object operand) {
        return null;
    }

    default Object minus(T primary, Object operand) {
        return null;
    }

    default Object divide(T primary, Object operand) {
        return null;
    }

    default Object multiply(T primary, Object operand) {
        return null;
    }

    default Object exponential(T primary, Object operand) {
        return null;
    }

    default Object modulus(T primary, Object operand) {
        return null;
    }
    
}
