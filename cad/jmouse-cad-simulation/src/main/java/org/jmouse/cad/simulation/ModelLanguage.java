package org.jmouse.cad.simulation;

import org.jmouse.cad.component.Equation;
import org.jmouse.formula.Formula;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.el.FormulaLanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The language a component's expressions are written in — its equations, its lets, its heat law, its instruments and its
 * displays alike: jMF, with the rate of a charge {@code d(q)/dt} — Leibniz's notation, recognised by jMF in the tree —, a
 * noise current {@code noise(density)}, a pin's members ({@code anode.current}, {@link PinQuantities}), and SPICE's
 * spellings read only to be refused with what this language writes instead. One language, so a document reads the same
 * wherever it is read.
 */
public final class ModelLanguage {

    /** What jMF reads {@code d(q)/dt} as: a rate the solver integrates, bound by this name, which no author can write. */
    public static final String RATE = "d/dt";

    /**
     * A noise current an equation adds, {@code noise(density)}: nothing to the solution, to a noise analysis a random
     * current along the equation of that one-sided power spectral density, A²/Hz — a junction's {@code 2 * q * I}.
     */
    public static final String NOISE = "noise";

    /** The variable a rate is over: time. */
    private static final String TIME = "t";

    /** SPICE's spellings, read as calls only to be refused with what this language writes instead. */
    private static final Map<String, String> BORROWED = Map.of(
            "V", "a pin's name is its voltage — write 'anode - j', not V(anode, j), and 'output', not V(output).",
            "I", "a current is written on the left: 'current anode -> j = …', or read as 'anode.current'.",
            "ddt", "a rate is written d(q)/dt, not ddt(q).");

    public static final FormulaLanguage LANGUAGE = FormulaLanguage.standard()
            .withHostCalls(NOISE, PinQuantities.CURRENT, PinQuantities.VOLTAGE)
            .withHostCalls(BORROWED.keySet().toArray(String[]::new))
            .withRate(TIME, RATE);

    private ModelLanguage() {
    }

    /**
     * The state a left side is the rate of — {@code q} for {@code d(q)/dt} —, as jMF reads it; empty for any other left
     * side.
     */
    public static Optional<String> rateOf(org.jmouse.cad.Formula left) {
        if (LANGUAGE.parse(left.written()) instanceof Formula.HostCall call && call.name().equals(RATE)
                && call.arguments().size() == 1 && call.arguments().getFirst() instanceof Formula.Variable variable) {
            return Optional.of(variable.name());
        }
        return Optional.empty();
    }

    /**
     * An expression as jMF reads it against a library of definitions, with SPICE's spellings refused.
     *
     * @param where what a refusal names: the component, the line
     */
    public static Formula read(String where, String text, List<Formula.Definition> library) {
        Formula parsed;
        try {
            parsed = LANGUAGE.parse(text, library);
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException(where + ": " + failure.getMessage() + " — in: " + text, failure);
        }
        return refuseBorrowed(where + ": ", parsed);
    }

    /**
     * An expression that reads a part — what an instrument, a display, a limit reads —, refused where it writes a rate or
     * a noise current: those add to a part, and belong in its equations.
     *
     * @param where what a refusal names: the component
     */
    public static Formula readings(String where, String text, List<Formula.Definition> library) {
        Formula read = read(where, text, library);
        return Substitution.replaceHostCalls(read, call -> {
            if (RATE.equals(call.name()) || NOISE.equals(call.name())) {
                throw new IllegalArgumentException(where + ": '" + text + "' reads the part, it adds nothing to it — "
                        + (RATE.equals(call.name()) ? "d(q)/dt" : "noise(…)") + " belongs in an equation.");
            }
            return call;
        });
    }

    /** {@code parsed}, refused where it calls one of SPICE's spellings. */
    public static Formula refuseBorrowed(String where, Formula parsed) {
        return Substitution.replaceHostCalls(parsed, call -> {
            String instead = BORROWED.get(call.name());
            if (instead != null) {
                throw new IllegalArgumentException(where + instead);
            }
            return call;
        });
    }

    /**
     * The definitions a component's equations may call — every library it uses, then its own —, each statement read as
     * jMF reads it, one at a time, each able to call those before it: no text is ever put together for jMF to read.
     */
    public static List<Formula.Definition> definitions(String where, List<Equation> formulas) {
        List<Formula.Definition> library = new ArrayList<>();
        for (Equation formula : formulas) {
            if (!(formula instanceof Equation.Defined defined)) {
                continue;
            }
            try {
                library.addAll(LANGUAGE.definitions(defined.statement().written(), library));
            } catch (RuntimeException failure) {
                throw new IllegalArgumentException(where + ": " + failure.getMessage() + " — in: " + defined.statement(), failure);
            }
        }
        return library;
    }
}
