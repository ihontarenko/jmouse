package org.jmouse.cad.component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * What a component is to a bench that stands it at a connector of a design to run it as it is used — written in its head,
 * {@code meta { bench: source }}. A bench device is a component like any other: this field is all that sets it apart.
 */
public enum BenchRole {

    /** What feeds the design: the mains socket, a transformer's winding, a bench supply. */
    SOURCE("source"),

    /** What the design drives: a dummy load. */
    LOAD("load"),

    /** What a hand turns while the design runs: a potentiometer. */
    CONTROL("control"),

    /** What reads the design: an ammeter, a voltmeter. */
    INSTRUMENT("instrument");

    /** The field of a component's head the role is written in. */
    public static final String FIELD = "bench";

    private final String word;

    BenchRole(String word) {
        this.word = word;
    }

    public String word() {
        return word;
    }

    /** Every role's word, in order. */
    public static List<String> words() {
        return Arrays.stream(values()).map(BenchRole::word).toList();
    }

    /** The role a word names; empty for none. */
    public static Optional<BenchRole> of(String word) {
        return Arrays.stream(values()).filter(role -> role.word.equals(word)).findFirst();
    }

    /** The role a word names, refused — saying what may be written — when it names none. */
    public static BenchRole named(String word) {
        return of(word).orElseThrow(() -> new IllegalArgumentException("A bench device is " + listed() + ", not '" + word + "'"));
    }

    /** Every role as a sentence says them: {@code a source, a load, a control or an instrument}. */
    private static String listed() {
        List<String> named = words().stream().map(role -> ("aeiou".indexOf(role.charAt(0)) >= 0 ? "an " : "a ") + role).toList();
        return String.join(", ", named.subList(0, named.size() - 1)) + " or " + named.getLast();
    }
}
