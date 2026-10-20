package org.jmouse.parallax.circuit;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.Function;

import java.util.Arrays;
import java.util.List;
import java.util.function.LongBinaryOperator;

/**
 * The arithmetic of a digital part's behaviour: a register, a counter, a bus read as a number, a byte on a wire. Each is
 * a function rather than an operator: {@code ^} is jMS's power and {@code |} its filter, and a document already written
 * with them must keep meaning what it meant.
 *
 * <h3>Numbers</h3>
 * <ul>
 *   <li>{@code bit(value, index)} — 0 or 1, bit 0 the least significant;</li>
 *   <li>{@code with_bit(value, index, level)} — the value with that bit set to the level (0 or 1);</li>
 *   <li>{@code bit_and(a, b)}, {@code bit_or(a, b)}, {@code bit_xor(a, b)};</li>
 *   <li>{@code bit_not(value, width)} — every one of the low {@code width} bits turned over;</li>
 *   <li>{@code shift_left(value, count, width)} — moved up, keeping the low {@code width} bits, as a register of that
 *       width drops what it shifts out; {@code shift_right(value, count)} — moved down;</li>
 *   <li>{@code low_bits(value, width)} — the low {@code width} bits: a counter wrapping, a byte taken from a word.</li>
 * </ul>
 *
 * <h3>Rows of levels</h3>
 * <p>A register held level by level — a row of states, {@code shift[8] = X discrete}, bit 0 its first slot — keeps an
 * unknown bit where it is: eight clock edges push the unknowns of a register at power-up out of its end, as they do in
 * the part. A number cannot: one unknown bit makes the whole number unknown, and shifting it keeps it so for ever.</p>
 * <ul>
 *   <li>{@code shift_in(row, level)} — the row moved up by one, the level in its first slot, the last slot dropped;</li>
 *   <li>{@code value_of(row)} — the number the row makes;</li>
 *   <li>{@code row_of(value, width)} — a number as a row of that many levels.</li>
 * </ul>
 *
 * <h3>An unknown stays unknown</h3>
 * <p>A level a part cannot know — an input left floating, a bus with a bit released — reaches the behaviour as {@code X}
 * or {@code Z}. Whatever is worked out from it is {@code X} too, as a real register holds garbage after a clock edge with
 * garbage on its input; it never quietly becomes a number. A number that is not whole, or is below zero, is a mistake in
 * the document and is refused with the function's name.</p>
 */
final class LogicFunctions implements Extension {

    /** What a bit operation gives when an operand is not known: the unknown level, as a behaviour writes it. */
    static final Object UNKNOWN = LogicLevel.UNKNOWN.scriptValue();

    /** The widest value a behaviour works on: room for a 62-bit register without reaching the sign. */
    private static final int MAXIMUM_WIDTH = 62;

    @Override
    public List<Function> getFunctions() {
        return List.of(
                numeric("bit", 2, (values) -> (values[0] >>> index(values[1], "bit")) & 1),
                numeric("with_bit", 3, (values) -> {
                    long mask = 1L << index(values[1], "with_bit");
                    return values[2] == 0 ? values[0] & ~mask : values[0] | mask;
                }),
                binary("bit_and", (left, right) -> left & right),
                binary("bit_or", (left, right) -> left | right),
                binary("bit_xor", (left, right) -> left ^ right),
                numeric("bit_not", 2, (values) -> ~values[0] & maskOf(values[1], "bit_not")),
                numeric("shift_left", 3, (values) -> (values[0] << index(values[1], "shift_left")) & maskOf(values[2], "shift_left")),
                numeric("shift_right", 2, (values) -> values[0] >>> index(values[1], "shift_right")),
                numeric("low_bits", 2, (values) -> values[0] & maskOf(values[1], "low_bits")),
                function("shift_in", 2, (arguments) -> {
                    Object[] row = DigitalUnit.arrayOf(arguments.get(0));
                    Object[] shifted = new Object[row.length];
                    shifted[0] = levelOf(arguments.get(1));
                    System.arraycopy(row, 0, shifted, 1, row.length - 1);
                    return shifted;
                }),
                function("value_of", 1, (arguments) -> DigitalUnit.busValue(Arrays.asList(DigitalUnit.arrayOf(arguments.get(0))))),
                function("row_of", 2, (arguments) -> {
                    int width = index(wholeOf(arguments.get(1), "row_of"), "row_of");
                    Object value = arguments.get(0);
                    Object[] row = new Object[width];
                    for (int position = 0; position < width; position++) {
                        row[position] = value instanceof Number ? (int) ((wholeOf(value, "row_of") >>> position) & 1) : levelOf(value);
                    }
                    return row;
                }));
    }

    /** How one function works its whole operands out, once every one of them is known. */
    @FunctionalInterface
    private interface Operation {
        long apply(long[] values);
    }

    /** What a function of rows does with what it is given. */
    @FunctionalInterface
    private interface Body {
        Object apply(Arguments arguments);
    }

    private static Function binary(String name, LongBinaryOperator operator) {
        return numeric(name, 2, (values) -> operator.applyAsLong(values[0], values[1]));
    }

    /** A function of whole numbers: unknown when any operand is not a number. */
    private static Function numeric(String name, int arity, Operation operation) {
        return function(name, arity, (arguments) -> {
            long[] values = new long[arity];
            for (int index = 0; index < arity; index++) {
                Object argument = arguments.get(index);
                if (!(argument instanceof Number)) {
                    return UNKNOWN;
                }
                values[index] = wholeOf(argument, name);
            }
            return narrowest(operation.apply(values));
        });
    }

    private static Function function(String name, int arity, Body body) {
        return new Function() {
            @Override
            public Object execute(Arguments arguments, EvaluationContext context) {
                if (arguments.size() != arity) {
                    throw new IllegalArgumentException(name + " takes " + arity + " values, not " + arguments.size() + ".");
                }
                return body.apply(arguments);
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    /** A level as a slot of a row holds it: 0, 1, X or Z. */
    private static Object levelOf(Object written) {
        return LogicLevel.of(written).scriptValue();
    }

    /** A whole number a bit operation may take: a count, a register, a level. */
    private static long wholeOf(Object argument, String function) {
        if (!(argument instanceof Number number)) {
            throw new IllegalArgumentException(function + " takes a whole number here, not " + argument + ".");
        }
        double value = number.doubleValue();
        if (value < 0 || value != Math.rint(value) || value >= 0x1p62) {
            throw new IllegalArgumentException(function + " works on whole numbers from 0 up, not " + argument + ".");
        }
        return (long) value;
    }

    private static int index(long value, String function) {
        if (value > MAXIMUM_WIDTH) {
            throw new IllegalArgumentException(function + " reaches bits 0 to " + MAXIMUM_WIDTH + ", not " + value + ".");
        }
        return (int) value;
    }

    private static long maskOf(long width, String function) {
        return (1L << index(width, function)) - 1;
    }

    /** An int when the value fits one, as a literal in the document is — so comparing them is comparing like with like. */
    static Number narrowest(long value) {
        return value <= Integer.MAX_VALUE ? Integer.valueOf((int) value) : Long.valueOf(value);
    }
}
