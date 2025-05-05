package org.jmouse.el.extension.calculator.mathematic;

public class IntegerOperation extends NumberOperation<Integer> {

    @Override
    public Object plus(Integer primary, Object operand) {
        return ((Double)super.plus(primary, operand)).intValue();
    }

    @Override
    public Object minus(Integer primary, Object operand) {
        return ((Double)super.minus(primary, operand)).intValue();
    }

    @Override
    public Object divide(Integer primary, Object operand) {
        return super.divide(primary, operand);
    }

    @Override
    public Object multiply(Integer primary, Object operand) {
        return ((Double)super.multiply(primary, operand)).intValue();
    }

    @Override
    public Object exponential(Integer primary, Object operand) {
        return ((Double)super.exponential(primary, operand)).intValue();
    }

    @Override
    public Object modulus(Integer primary, Object operand) {
        return ((Double)super.modulus(primary, operand)).intValue();
    }
}
