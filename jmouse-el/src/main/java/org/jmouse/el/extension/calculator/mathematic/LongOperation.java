package org.jmouse.el.extension.calculator.mathematic;

public class LongOperation extends NumberOperation<Long> {

    @Override
    public Object plus(Long primary, Object operand) {
        return ((Double)super.plus(primary, operand)).longValue();
    }

    @Override
    public Object minus(Long primary, Object operand) {
        return ((Double)super.minus(primary, operand)).longValue();
    }

    @Override
    public Object divide(Long primary, Object operand) {
        return ((Double)super.divide(primary, operand)).longValue();
    }

    @Override
    public Object multiply(Long primary, Object operand) {
        return ((Double)super.multiply(primary, operand)).longValue();
    }

    @Override
    public Object exponential(Long primary, Object operand) {
        return ((Double)super.exponential(primary, operand)).longValue();
    }

    @Override
    public Object modulus(Long primary, Object operand) {
        return ((Double)super.modulus(primary, operand)).longValue();
    }
}
