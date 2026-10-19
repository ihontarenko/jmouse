package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentDocuments;
import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.component.ComponentVocabulary;
import org.jmouse.parallax.component.ModelLanguage;

import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Symbols;

import org.jmouse.parallax.numerics.NewtonSolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One part's own heat, as its component's {@code heat { … }} law writes it ({@link ComponentModel.Heat}): its thermal
 * states, advanced by the engine after every accepted step from the power the part's equations dissipated, and the
 * temperature they make — which, on a realistic bench, the part's equations read as {@code temperature}.
 *
 * <p>The law is the document's; only the numerics are here, and they are the engine's own: the law is compiled as any
 * formula is, and advanced by backward Euler solved with {@link NewtonSolver} — stable for any step. Heat moves in
 * seconds and a circuit's step in microseconds, so the engine advances it outside the circuit's Newton, from the energy
 * a part took over a stretch of steps ({@link LiveIntegrator}). A part's power is the engine's definition, never a formula of
 * the law: the sum of what its equations carry across the pins they flow between ({@link Device#power}).</p>
 *
 * <p>The law reads {@code power} (W), {@code ambient} — the bench's temperature, kelvin — and the part's parameters;
 * {@code temperature} is what it defines, so it may not read it.</p>
 */
final class PartHeat {

    /** The bench's temperature, kelvin, as the law reads it. */
    static final String AMBIENT = "ambient";

    private static final int NEWTON_ITERATIONS = 8;
    /** How closely a step of the law is solved, kelvin: far below anything a drift can show. */
    private static final double SETTLED = 1e-9;

    /**
     * A law compiled for one list of parameter names: its states, their rates and the temperature they make, all read
     * from one argument array — states, then power and ambient, then the parameters. The values are arguments, so every
     * part that warms by the same law with the same parameters shares it.
     */
    private record CompiledLaw(String[] states, RealFunction[] rates, RealFunction temperature) {
    }

    /** What decides a compiled law: the law, and the names of the parameters its part is read with. */
    private record LawForm(ComponentModel.Heat law, List<String> parameterNames) {
    }

    /** How many compiled laws are kept: a catalogue's worth, as {@link ModelEquations} keeps its forms. */
    private static final int KEPT_LAWS = 256;

    /**
     * The laws compiled so far. A law was parsed and compiled for every part on every run before — two hundred resistors
     * of a ladder compiled the one shared law two hundred times.
     */
    private static final Map<LawForm, CompiledLaw> COMPILED = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<LawForm, CompiledLaw> eldest) {
                    return size() > KEPT_LAWS;
                }
            });

    private final String[] states;
    private final RealFunction[] rates;
    private final RealFunction temperature;
    /** States, then power and ambient, then the parameters. */
    private final double[] arguments;
    private final int powerPosition;
    /** The temperature the states make now, kelvin: computed once a step, read on every Newton iteration. */
    private double temperatureNow;

    private PartHeat(String[] states, RealFunction[] rates, RealFunction temperature, double[] arguments) {
        this.states = states;
        this.rates = rates;
        this.temperature = temperature;
        this.arguments = arguments;
        this.powerPosition = states.length;
        this.temperatureNow = temperature.evaluate(arguments);
    }

    /**
     * The heat of a part of {@code model} with these parameter values on a bench at {@code ambient} kelvin; null for a
     * part that does not warm — its component has no law, or gives no {@code thermal_resistance} above nought.
     *
     * @throws IllegalArgumentException when the law does not read, naming the document it is written in
     */
    static PartHeat of(ComponentModel model, Map<String, Double> parameters, double ambient) {
        ComponentModel.Heat law = model.heat();
        Double resistance = parameters.get(ComponentDocuments.THERMAL_RESISTANCE);
        if (law == null || resistance == null || !(resistance > 0)) {
            return null;
        }
        List<String> parameterNames = parameters.keySet().stream().filter(name -> !name.equals(ComponentVocabulary.TEMPERATURE)).toList();
        LawForm form = new LawForm(law, parameterNames);
        CompiledLaw compiled = COMPILED.get(form);
        if (compiled == null) {
            compiled = compile(law, parameterNames);
            COMPILED.put(form, compiled);
        }
        int count = compiled.states().length;
        double[] arguments = new double[count + 2 + parameterNames.size()];
        arguments[count + 1] = ambient;
        for (int index = 0; index < parameterNames.size(); index++) {
            arguments[count + 2 + index] = parameters.get(parameterNames.get(index));
        }
        return new PartHeat(compiled.states(), compiled.rates(), compiled.temperature(), arguments);
    }

    /** The law read and compiled over its states, power, ambient and these parameters, in that order. */
    private static CompiledLaw compile(ComponentModel.Heat law, List<String> parameterNames) {
        List<String> stateNames = List.copyOf(law.rates().keySet());
        List<String> names = new ArrayList<>(stateNames);
        names.add(ComponentVocabulary.POWER);
        names.add(AMBIENT);
        names.addAll(parameterNames);
        Symbols symbols = Symbols.of(List.of(), names);
        List<Formula.Definition> library = definitions(law);
        RealFunction[] rates = new RealFunction[stateNames.size()];
        for (int index = 0; index < rates.length; index++) {
            String text = law.rates().get(stateNames.get(index));
            Formula rate = ModelLanguage.read(law.source() + ", heat", text, 0, library);
            rates[index] = compile(law, rate, symbols, text);
        }
        RealFunction temperature = compile(law, ModelLanguage.read(law.source() + ", heat", law.temperature(), 0, library),
                symbols, law.temperature());
        return new CompiledLaw(stateNames.toArray(String[]::new), rates, temperature);
    }

    /** The definitions the law may call: its document's formulas, then its own lets, each a definition of nothing. */
    private static List<Formula.Definition> definitions(ComponentModel.Heat law) {
        StringBuilder text = new StringBuilder();
        law.formulas().forEach(formulas -> text.append(formulas).append('\n'));
        law.locals().forEach((name, expression) -> text.append(name).append(" = ").append(expression).append('\n'));
        try {
            return ModelLanguage.LANGUAGE.definitions(text.toString());
        } catch (FormulaException | IllegalArgumentException unreadable) {
            throw new IllegalArgumentException(law.source() + ", heat: " + unreadable.getMessage(), unreadable);
        }
    }

    private static RealFunction compile(ComponentModel.Heat law, Formula formula, Symbols symbols, String text) {
        try {
            return RealCompiler.compile(formula, symbols, text);
        } catch (RuntimeException unreadable) {
            throw new IllegalArgumentException(law.source() + ", heat: '" + text + "' does not read — " + unreadable.getMessage()
                    + ". A heat law reads power, ambient and the part's parameters; temperature is what it defines.", unreadable);
        }
    }

    /** Advances the states over {@code seconds} at {@code power}, by backward Euler: x₁ = x₀ + h · f(x₁). */
    void advance(double power, double seconds) {
        if (!(seconds > 0) || !Double.isFinite(power)) {
            return;
        }
        arguments[powerPosition] = power;
        int count = states.length;
        double[] start = new double[count];
        System.arraycopy(arguments, 0, start, 0, count);
        double[] at = arguments.clone();
        NewtonSolver.solve((point, residual) -> {
            System.arraycopy(point, 0, at, 0, count);
            for (int index = 0; index < count; index++) {
                residual[index] = point[index] - start[index] - seconds * rates[index].evaluate(at);
            }
        }, start, NEWTON_ITERATIONS, SETTLED).ifPresent(solved -> System.arraycopy(solved, 0, arguments, 0, count));
        temperatureNow = temperature.evaluate(arguments);
    }

    /** Its own temperature now, kelvin. */
    double temperature() {
        return temperatureNow;
    }

    /** How far above the bench it is now, kelvin. */
    double rise() {
        return temperature() - arguments[powerPosition + 1];
    }

    /** Goes on from where {@code previous} — the same law of the part it replaces — had brought its states. */
    void continueFrom(PartHeat previous) {
        if (previous != null && previous.states.length == states.length) {
            System.arraycopy(previous.arguments, 0, arguments, 0, states.length);
            temperatureNow = temperature.evaluate(arguments);
        }
    }
}
