package org.jmouse.j2js;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * 🌐 A value that crosses between a page and an {@link Export exported} method, by the Java type it is in the method.
 *
 * <p>These are the values JavaScript has the same as Java — a number, a truth value, text, an array of one of them — so
 * every compiler can pass them without a wrapper. A {@code long} is not one: JavaScript's number cannot hold it whole.
 * Anything structured crosses as {@link #TEXT}, written as JSON — jmouse-json reads and writes it on either side.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum BoundaryValue {

    /** A truth value. */
    TRUTH("boolean", "boolean", Position.EVERYWHERE),
    /** A whole number, as JavaScript's number holds it. */
    WHOLE_NUMBER("int", "number", Position.EVERYWHERE),
    /** A number. */
    NUMBER("double", "number", Position.EVERYWHERE),
    /** Text — and anything structured, written as JSON. */
    TEXT("java.lang.String", "string", Position.EVERYWHERE),
    /** Truth values in order. */
    TRUTHS("boolean[]", "boolean[]", Position.EVERYWHERE),
    /** Whole numbers in order. */
    WHOLE_NUMBERS("int[]", "number[]", Position.EVERYWHERE),
    /** Numbers in order. */
    NUMBERS("double[]", "number[]", Position.EVERYWHERE),
    /** Texts in order. */
    TEXTS("java.lang.String[]", "string[]", Position.EVERYWHERE),
    /** No value: a method that answers nothing. */
    NOTHING("void", "void", EnumSet.of(Position.RESULT));

    private final String        javaType;
    private final String        typeScriptType;
    private final Set<Position> positions;

    BoundaryValue(String javaType, String typeScriptType, Set<Position> positions) {
        this.javaType = javaType;
        this.typeScriptType = typeScriptType;
        this.positions = Set.copyOf(positions);
    }

    /**
     * The value a Java type crosses as, written as the compiler writes it — {@code double}, {@code java.lang.String},
     * {@code int[]}; empty for a type that cannot cross.
     */
    public static Optional<BoundaryValue> ofJavaType(String javaType) {
        return Arrays.stream(values()).filter(value -> value.javaType.equals(javaType)).findFirst();
    }

    /** The Java type, as the compiler writes it. */
    public String javaType() {
        return javaType;
    }

    /** The type a page's TypeScript sees it as: {@code number} for both numbers — JavaScript has one. */
    public String typeScriptType() {
        return typeScriptType;
    }

    /** Whether the value may stand where {@code position} is. */
    public boolean crossesAt(Position position) {
        return positions.contains(position);
    }

    /** Where in an exported method a value stands. */
    public enum Position {

        /** A parameter the page passes. */
        PARAMETER,
        /** The result the page is answered. */
        RESULT;

        private static final Set<Position> EVERYWHERE = EnumSet.allOf(Position.class);
    }
}
