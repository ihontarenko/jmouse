package org.jmouse.parallax.component;

import org.jmouse.formula.Formula;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.el.FormulaLanguage;

import java.util.List;
import java.util.Map;

/**
 * The language a component document's expressions are written in — its equations, its lets, its heat law, its
 * instruments and its displays alike: jMF, with the rate of a charge {@code d(q)/dt}, a noise current
 * {@code noise(density)}, a pin's members ({@code anode.current}, {@link PinQuantities}), and SPICE's spellings read only
 * to be refused with what this language writes instead. One language, so a document reads the same wherever it is read:
 * by the dimension check, the engine, an instrument.
 */
public final class ModelLanguage {

    /** The call a written {@code d(q)/dt} reaches the solver as: not a name, so an author can never write it. */
    public static final String RATE  = "d/dt";

    /**
     * A noise current an equation adds, {@code noise(density)}: nothing to the solution, to a noise analysis a random
     * current along the equation of that one-sided power spectral density, A²/Hz — a junction's {@code 2 * q * I}.
     */
    public static final String NOISE = "noise";

    /** SPICE's spellings, read as calls only to be refused with what this language writes instead. */
    private static final Map<String, String> BORROWED = Map.of(
            "V", "a pin's name is its voltage — write 'anode - j', not V(anode, j), and 'output', not V(output).",
            "I", "a current is written on the left: 'current anode -> j = …', or read as 'anode.current'.",
            "ddt", "a rate is written d(q)/dt, not ddt(q).");

    /**
     * The language every model expression is read in: jMF, with the solver's rate — {@code d(q)/dt}, recognised in the
     * tree — and noise, a pin's members, and SPICE's spellings, read only to be refused.
     */
    public static final FormulaLanguage LANGUAGE = FormulaLanguage.standard()
            .withHostCalls(NOISE, PinQuantities.CURRENT, PinQuantities.VOLTAGE)
            .withHostCalls(BORROWED.keySet().toArray(String[]::new))
            .withRate("t", RATE);

    private ModelLanguage() {
    }

    /**
     * A model's expression as jMF reads it — every {@code d(q)/dt} already the solver's rate, recognised in the tree —,
     * with SPICE's spellings ({@code V(a, b)}, {@code I(…)}, {@code ddt(q)}) refused saying what to write instead.
     *
     * @throws IllegalArgumentException when it does not read, naming the model, the line and the text
     */
    public static Formula read(String model, String text, int line, List<Formula.Definition> library) {
        String where = model + (line > 0 ? ", line " + line : "") + ": ";
        Formula parsed;
        try {
            parsed = LANGUAGE.parse(text, library);
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException(where + failure.getMessage() + " — in: " + text, failure);
        }
        return refuseBorrowed(where, parsed);
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

    /** The jMF definitions a model's equations may call, as one text. */
    public static String libraryText(ComponentModel model) {
        StringBuilder text = new StringBuilder();
        for (String definitions : model.formulas()) {
            text.append(definitions).append('\n');
        }
        return text.toString();
    }
}
