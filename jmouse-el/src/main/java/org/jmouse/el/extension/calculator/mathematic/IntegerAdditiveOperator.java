package org.jmouse.el.extension.calculator.mathematic;

import java.util.function.BinaryOperator;

public class IntegerAdditiveOperator implements BinaryOperator<Object> {

    @Override
    public Object apply(Object a, Object b) {
        if (a instanceof Integer integer && b instanceof Number number) {
            return integer + number.intValue();
        }

        throw new MathematicOperationException("Operand a and operand b is not compatible");
    }

}
