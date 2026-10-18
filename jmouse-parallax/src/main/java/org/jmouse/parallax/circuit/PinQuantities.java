package org.jmouse.parallax.circuit;

import org.jmouse.formula.Formula;
import org.jmouse.formula.Substitution;

import java.util.List;
import java.util.function.Function;

/**
 * What a part's own expressions may ask of one of its pins: {@code anode.current} — the current into it — and
 * {@code anode.voltage} — its voltage, the same as the bare {@code anode}. jMF reads a member its host declared as a
 * call ({@code withHostCalls(CALLS)}), so {@code anode.current} and {@code current(anode)} are one host call; it is
 * bound here, in the tree, to the name the part's own table reads it under. Nothing reads it out of the text.
 */
final class PinQuantities {

    /** The current into a pin: {@code anode.current}, or {@code current(anode)}. */
    static final String CURRENT = "current";
    /** The voltage of a pin: {@code anode.voltage}, or the bare {@code anode}. */
    static final String VOLTAGE = "voltage";
    /** Every member a pin has, as a language declares them. */
    static final String[] CALLS = {CURRENT, VOLTAGE};
    /** What a pin's current is read as once bound: a name no document can write. */
    static final String PREFIX = "current__";

    private PinQuantities() {
    }

    /**
     * {@code parsed} with each pin's current replaced by {@link #PREFIX} and the pin, and each pin's voltage by the
     * pin's name; every other host call left as it is, for its own host.
     *
     * @param refusal the words that refuse a member of what is no pin, given what was written
     * @throws IllegalArgumentException when a pin member names what is not one of {@code pins}
     */
    static Formula bind(Formula parsed, List<String> pins, Function<String, String> refusal) {
        return Substitution.replaceHostCalls(parsed, call -> {
            if (!CURRENT.equals(call.name()) && !VOLTAGE.equals(call.name())) {
                return call;
            }
            if (call.arguments().size() != 1 || !(call.arguments().getFirst() instanceof Formula.Variable pin)
                    || !pins.contains(pin.name())) {
                throw new IllegalArgumentException(refusal.apply(describe(call)) + " — a pin's " + call.name()
                        + " is asked of one of its pins (" + String.join(", ", pins) + "): " + pins.getFirst() + "."
                        + call.name() + ".");
            }
            return CURRENT.equals(call.name()) ? new Formula.Variable(PREFIX + pin.name()) : pin;
        });
    }

    /** Whether a bound expression reads a pin current. */
    static boolean readsCurrent(Formula bound) {
        return bound.variables().stream().anyMatch(name -> name.startsWith(PREFIX));
    }

    /** The pin a bound name stands for, or null for a name that is no pin current. */
    static String pinOf(String name) {
        return name.startsWith(PREFIX) ? name.substring(PREFIX.length()) : null;
    }

    private static String describe(Formula.HostCall call) {
        return call.arguments().size() == 1 && call.arguments().getFirst() instanceof Formula.Variable owner
                ? owner.name() + "." + call.name() : call.name() + "(…)";
    }
}
