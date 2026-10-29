package org.jmouse.cad.simulation.engine;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Arguments;
import org.jmouse.el.extension.Extension;
import org.jmouse.el.extension.Function;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.LongBinaryOperator;
import java.util.regex.Pattern;

/**
 * The arithmetic of a digital part's behaviour: a register, a counter, a bus read as a number, a byte on a wire. Each is
 * a function rather than an operator: {@code ^} is jMS's power and {@code |} its filter, and a document already written
 * with them must keep meaning what it meant.
 *
 * <h3>Numbers</h3>
 * <p>Whole numbers in two's complement, as a register holds them: {@code bit(-20, 0)} is 0 and {@code low_bits(-20, 9)}
 * is the 9-bit pattern of −20 — what a temperature sensor writes below nought.</p>
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
 * the part. A number cannot: one unknown bit makes the whole number unknown, and shifting it keeps it so for ever. A
 * row is a value: every function gives a new one and none changes the row it is given.</p>
 * <ul>
 *   <li>{@code item(row, index)} — the slot at that index, 0 the first; {@code with(row, index, value)} — the row with
 *       that slot written. An index past the row is refused; an index not known reads {@code X}, and a write to one
 *       leaves every slot {@code X}, since any of them may have been written;</li>
 *   <li>{@code shift_in(row, level)} — the row moved up by one, the level in its first slot, the last slot dropped;</li>
 *   <li>{@code value_of(row)} — the number the row makes, unknown if any slot is; {@code value_of(row, otherwise)} — a
 *       slot not known read as {@code otherwise}, as a bus with a floating line is read by a part that reads it anyway;</li>
 *   <li>{@code row_of(value, width)} — a number as a row of that many levels;</li>
 *   <li>{@code filled(value, length)} — a row of that many slots, each the value: memory erased, a screen of spaces;</li>
 *   <li>{@code bytes_of(text)} — the bytes a text writes, decimal or {@code 0x} hexadecimal, parted by spaces or commas
 *       ({@code "0x60, 128 0"}): what a bench instrument is told to send. Empty for a blank text; a word that is not a
 *       byte is refused, naming it.</li>
 * </ul>
 *
 * <h3>Levels</h3>
 * <ul>
 *   <li>{@code known(value)} — whether a value is a number at all: what a behaviour asks before counting on, rather than
 *       comparing a number with {@code X};</li>
 *   <li>{@code level_or(level, otherwise)} — 1 or 0 as it is; a level not known — an input left floating, a line let go
 *       — read as {@code otherwise}: an address pin pulled to ground inside the part, a bit shifted in as 0.</li>
 * </ul>
 *
 * <h3>An unknown stays unknown</h3>
 * <p>A level a part cannot know reaches the behaviour as {@code X} or {@code Z}. Whatever is worked out from it is
 * {@code X} too, as a real register holds garbage after a clock edge with garbage on its input; it never quietly becomes
 * a number — unless the behaviour says what it reads instead, with {@code level_or} or {@code value_of(row, otherwise)}.
 * A number that is not whole is a mistake in the document and is refused with the function's name.</p>
 */
final class LogicFunctions implements Extension {

    /** What a bit operation gives when an operand is not known: the unknown level, as a behaviour writes it. */
    static final Object UNKNOWN = LogicLevel.UNKNOWN.scriptValue();

    /** What parts the bytes of a written list: spaces, commas, or both. */
    private static final Pattern BYTE_SEPARATOR = Pattern.compile("[\\s,]+");

    /** The widest value a behaviour works on: room for a 62-bit register without reaching the sign. */
    static final int MAXIMUM_WIDTH = 62;

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
                function("shift_in", 2, 2, (arguments) -> {
                    Object[] row = rowOf(arguments.get(0), "shift_in");
                    Object[] shifted = new Object[row.length];
                    shifted[0] = levelOf(arguments.get(1));
                    System.arraycopy(row, 0, shifted, 1, row.length - 1);
                    return shifted;
                }),
                function("value_of", 1, 2, (arguments) -> {
                    Object[] row = rowOf(arguments.get(0), "value_of");
                    if (arguments.size() == 2) {
                        Object otherwise = arguments.get(1);
                        row = Arrays.stream(row).map(slot -> slot instanceof Number ? slot : otherwise).toArray();
                    }
                    return DigitalUnit.busValue(Arrays.asList(row));
                }),
                function("known", 1, 1, (arguments) -> arguments.get(0) instanceof Number),
                function("level_or", 2, 2, (arguments) -> {
                    Object level = arguments.get(0);
                    return level instanceof Number number && (number.intValue() == 0 || number.intValue() == 1) ? level : arguments.get(1);
                }),
                function("row_of", 2, 2, (arguments) -> {
                    int width = width(arguments.get(1), "row_of");
                    Object value = arguments.get(0);
                    Object[] row = new Object[width];
                    for (int position = 0; position < width; position++) {
                        row[position] = value instanceof Number ? (int) ((wholeOf(value, "row_of") >>> position) & 1) : levelOf(value);
                    }
                    return row;
                }),
                function("item", 2, 2, (arguments) -> {
                    Object[] row = rowOf(arguments.get(0), "item");
                    Object index = arguments.get(1);
                    return index instanceof Number ? row[slot(row, index, "item")] : UNKNOWN;
                }),
                function("with", 3, 3, (arguments) -> {
                    Object[] row = rowOf(arguments.get(0), "with").clone();
                    Object index = arguments.get(1);
                    if (index instanceof Number) {
                        row[slot(row, index, "with")] = arguments.get(2);
                    } else {
                        Arrays.fill(row, UNKNOWN);
                    }
                    return row;
                }),
                function("bytes_of", 1, 1, (arguments) -> bytesOf(String.valueOf(arguments.get(0)))),
                function("filled", 2, 2, (arguments) -> {
                    Object[] row = new Object[length(arguments.get(1), "filled")];
                    Arrays.fill(row, arguments.get(0));
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
        return function(name, arity, arity, (arguments) -> {
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

    private static Function function(String name, int fewest, int most, Body body) {
        return new Function() {
            @Override
            public Object execute(Arguments arguments, EvaluationContext context) {
                if (arguments.size() < fewest || arguments.size() > most) {
                    String expected = fewest == most ? String.valueOf(fewest) : fewest + " or " + most;
                    throw new IllegalArgumentException(name + " takes " + expected + " values, not " + arguments.size() + ".");
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

    /** A row a function is given: refused, with the function's name, when it is none or holds nothing. */
    private static Object[] rowOf(Object argument, String function) {
        Object[] row = DigitalUnit.arrayOf(argument);
        if (row == null) {
            throw new IllegalArgumentException(function + " takes a row of levels here, not " + argument + ".");
        }
        if (row.length == 0) {
            throw new IllegalArgumentException(function + " takes a row of at least one level.");
        }
        return row;
    }

    /** A whole number a bit operation may take, in two's complement: a count, a register, a level, a temperature below 0. */
    private static long wholeOf(Object argument, String function) {
        if (!(argument instanceof Number number)) {
            throw new IllegalArgumentException(function + " takes a whole number here, not " + argument + ".");
        }
        double value = number.doubleValue();
        if (value != Math.rint(value) || Math.abs(value) >= 0x1p62) {
            throw new IllegalArgumentException(function + " works on whole numbers below 2^62 either way, not " + argument + ".");
        }
        return (long) value;
    }

    /** A bit position, a shift or a width: from 0 to {@link #MAXIMUM_WIDTH}. */
    private static int index(long value, String function) {
        if (value < 0 || value > MAXIMUM_WIDTH) {
            throw new IllegalArgumentException(function + " reaches bits 0 to " + MAXIMUM_WIDTH + ", not " + value + ".");
        }
        return (int) value;
    }

    private static int width(Object argument, String function) {
        return index(wholeOf(argument, function), function);
    }

    /** The bytes a text writes, in the order written. */
    private static Object[] bytesOf(String text) {
        String written = text.strip();
        if (written.isEmpty()) {
            return new Object[0];
        }
        String[] words = BYTE_SEPARATOR.split(written);
        Object[] bytes = new Object[words.length];
        for (int index = 0; index < words.length; index++) {
            bytes[index] = byteOf(words[index]);
        }
        return bytes;
    }

    private static int byteOf(String word) {
        String lowered = word.toLowerCase(Locale.ROOT);
        int value;
        try {
            value = lowered.startsWith("0x") ? Integer.parseInt(lowered.substring(2), 16) : Integer.parseInt(lowered);
        } catch (NumberFormatException unreadable) {
            throw new IllegalArgumentException("bytes_of reads bytes in decimal or 0x hexadecimal, and '" + word + "' is neither.");
        }
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("bytes_of reads bytes, 0 to 255, and " + word + " is not one.");
        }
        return value;
    }

    /** A slot of a row: from 0 to one short of its length. */
    private static int slot(Object[] row, Object argument, String function) {
        long slot = wholeOf(argument, function);
        if (slot < 0 || slot >= row.length) {
            throw new IllegalArgumentException(function + " reaches slots 0 to " + (row.length - 1) + " of this row, not " + slot + ".");
        }
        return (int) slot;
    }

    /** A row's length: from 1 up. */
    private static int length(Object argument, String function) {
        long length = wholeOf(argument, function);
        if (length < 1 || length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(function + " makes a row of at least one slot, not " + length + ".");
        }
        return (int) length;
    }

    private static long maskOf(long width, String function) {
        return (1L << index(width, function)) - 1;
    }

    /** An int when the value fits one, as a literal in the document is — so comparing them is comparing like with like. */
    static Number narrowest(long value) {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE ? Integer.valueOf((int) value) : Long.valueOf(value);
    }
}
