package org.jmouse.cad.simulation;

import org.jmouse.formula.Formula;
import org.jmouse.formula.Substitution;

import java.util.List;
import java.util.function.Function;

/**
 * What a part's own expressions may ask of one of its pins: {@code anode.current} — the current into it — and
 * {@code anode.voltage} — its voltage, the same as the bare {@code anode}. jMF reads a member its host declared as a
 * call, so {@code anode.current} and {@code current(anode)} are one host call. A voltage is the pin's name at once; a
 * current stays a call — {@code current(k)}, k the pin's position among the part's pins — until whoever reads the
 * expression binds it to what carries that current ({@link #bindCurrents}). By its position, not its name: a pin's name
 * is also its voltage, and binding the voltages must not reach the pin a current asks of.
 */
public final class PinQuantities {

    /** The current into a pin: {@code anode.current}, or {@code current(anode)}. */
    public static final String CURRENT = "current";

    /** The voltage of a pin: {@code anode.voltage}, or the bare {@code anode}. */
    public static final String VOLTAGE = "voltage";

    private PinQuantities() {
    }

    /**
     * {@code parsed} with each pin's voltage read as the pin's name and each pin's current checked to ask of one of
     * {@code pins}; every other host call left as it is, for its own host.
     *
     * @param refusal the words that refuse a member of what is no pin, given what was written
     */
    public static Formula bind(Formula parsed, List<String> pins, Function<String, String> refusal) {
        return Substitution.replaceHostCalls(parsed, call -> {
            if (!CURRENT.equals(call.name()) && !VOLTAGE.equals(call.name()) || positionOf(call) >= 0) {
                return call;
            }
            if (call.arguments().size() != 1 || !(call.arguments().getFirst() instanceof Formula.Variable pin)
                    || !pins.contains(pin.name())) {
                throw new IllegalArgumentException(refusal.apply(describe(call)) + " — a pin's " + call.name()
                        + " is asked of one of its pins (" + String.join(", ", pins) + ")");
            }
            return CURRENT.equals(call.name())
                    ? new Formula.HostCall(CURRENT, List.of(new Formula.Constant(pins.indexOf(pin.name())))) : pin;
        });
    }

    /** {@code bound} with each pin's current replaced by what {@code binding} says carries the current into that pin. */
    public static Formula bindCurrents(Formula bound, List<String> pins, Function<String, Formula> binding) {
        return Substitution.replaceHostCalls(bound, call -> {
            int position = positionOf(call);
            return position < 0 ? call : binding.apply(pins.get(position));
        });
    }

    /** Whether a bound expression reads a pin's current, inside the definitions it calls too. */
    public static boolean readsCurrent(Formula bound) {
        boolean[] reads = {false};
        Substitution.replaceHostCalls(bound, call -> {
            reads[0] |= positionOf(call) >= 0;
            return call;
        });
        return reads[0];
    }

    /** Whether a host call is a pin's current, bound or not by {@link #bind}. */
    public static boolean isCurrent(Formula.HostCall call) {
        return positionOf(call) >= 0;
    }

    private static int positionOf(Formula.HostCall call) {
        return CURRENT.equals(call.name()) && call.arguments().size() == 1 && call.arguments().getFirst() instanceof Formula.Constant position
                ? (int) position.value() : -1;
    }

    private static String describe(Formula.HostCall call) {
        return call.arguments().size() == 1 && call.arguments().getFirst() instanceof Formula.Variable owner
                ? owner.name() + "." + call.name() : call.name() + "(…)";
    }
}
