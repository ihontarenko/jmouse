package org.jmouse.spice.node;

import org.jmouse.spice.SpiceNumbers;
import org.jmouse.spice.SpicePosition;

/**
 * A value as a SPICE card writes it: a number with its scale factor, a parameter's name, or an expression.
 */
public sealed interface SpiceValue {

    SpicePosition position();

    /** The value as SPICE writes it back. */
    String written();

    /**
     * {@code 4.7k}, {@code 10uF}, {@code 1meg}.
     *
     * @param value   the number in SI
     * @param written the number as written, its scale factor and unit letters kept
     */
    record Number(double value, String written, SpicePosition position) implements SpiceValue {
    }

    /** {@code R1} where a value is wanted: a parameter, named without braces as some SPICEs allow. */
    record Reference(String name, SpicePosition position) implements SpiceValue {
        @Override
        public String written() {
            return name;
        }
    }

    /** {@code {2*R1}} or {@code '2*R1'}. */
    record Expression(SpiceExpression expression) implements SpiceValue {
        @Override
        public SpicePosition position() {
            return expression.position();
        }

        @Override
        public String written() {
            return expression.written();
        }
    }

    /** A number a program writes: in its shortest form, exponent and all — {@code 1000}, {@code 2.52e-09}. */
    static SpiceValue number(double value) {
        return new Number(value, SpiceNumbers.write(value), SpicePosition.NONE);
    }

    /** An expression a program writes, in braces. */
    static SpiceValue expression(String text) {
        return new Expression(new SpiceExpression(text, SpiceExpression.Delimiter.BRACES, SpicePosition.NONE));
    }
}
