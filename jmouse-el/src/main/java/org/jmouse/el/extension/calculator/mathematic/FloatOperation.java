package org.jmouse.el.extension.calculator.mathematic;

public class FloatOperation extends NumberOperation<Float> {

    @Override
    public Object plus(Float primary, Object operand) {
        return ((Double)super.plus(primary, operand)).floatValue();
    }

    @Override
    public Object minus(Float primary, Object operand) {
        return ((Double)super.minus(primary, operand)).floatValue();
    }

    @Override
    public Object divide(Float primary, Object operand) {
        return ((Double)super.divide(primary, operand)).floatValue();
    }

    @Override
    public Object multiply(Float primary, Object operand) {
        return ((Double)super.multiply(primary, operand)).floatValue();
    }

    @Override
    public Object exponential(Float primary, Object operand) {
        return ((Double)super.exponential(primary, operand)).floatValue();
    }

    @Override
    public Object modulus(Float primary, Object operand) {
        return ((Double)super.modulus(primary, operand)).floatValue();
    }
}
