package org.jmouse.libre.pcb;

import org.jmouse.sexpression.node.SExpressionList;

import java.util.Optional;

/** How the format writes a value, wherever it is written: a yes as {@code true}, a nothing as {@code none}, a point as two numbers. */
final class LibrePcbValues {

    private static final String TRUE = "true";
    private static final String NONE = "none";

    private LibrePcbValues() {
    }

    /** Whether a list of that name says yes. */
    static boolean flag(SExpressionList list, LibrePcbKeyword name) {
        return list.wordOf(name).map(TRUE::equals).orElse(false);
    }

    /** The word a list of that name says; empty for {@code none} and for no such list. */
    static Optional<String> reference(SExpressionList list, LibrePcbKeyword name) {
        return list.wordOf(name).filter(word -> !NONE.equals(word));
    }

    static String text(SExpressionList list, LibrePcbKeyword name) {
        return list.wordOf(name).orElse("");
    }

    static double number(SExpressionList list, LibrePcbKeyword name) {
        return list.requireList(name).number(1);
    }

    static double numberOr(SExpressionList list, LibrePcbKeyword name, double otherwise) {
        return list.list(name).map(found -> found.number(1)).orElse(otherwise);
    }

    /** A number a list says, or nothing where it says a word instead: {@code (default_trace_width inherit)}. */
    static Optional<Double> numberIfOne(SExpressionList list, LibrePcbKeyword name) {
        return list.wordOf(name).flatMap(LibrePcbValues::parsed);
    }

    /** A word as a number, where it is one. */
    static Optional<Double> parsed(String word) {
        try {
            return Optional.of(Double.valueOf(word));
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    static LibrePcbPoint position(SExpressionList list) {
        SExpressionList position = list.requireList(LibrePcbKeyword.POSITION);
        return new LibrePcbPoint(position.number(1), position.number(2));
    }
}
