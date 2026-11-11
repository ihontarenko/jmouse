package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.component.Equation;
import org.jmouse.cad.simulation.ComponentCompilation;
import org.jmouse.cad.simulation.EngineName;
import org.jmouse.cad.simulation.HeatLaw;
import org.jmouse.cad.simulation.ModelLanguage;
import org.jmouse.cad.simulation.SimulatedComponent;

import org.jmouse.formula.Formula;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Symbols;

import org.jmouse.numerics.NewtonSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One part's own heat, as its component's {@code heat { … }} law writes it ({@link HeatLaw}): its thermal
 * states, advanced by the engine after every accepted step from the power the part's equations dissipated, and the
 * temperature they make — which, on a realistic bench, the part's equations read as {@code temperature}.
 *
 * <p>The law is the document's; only the numerics are here, and they are the engine's own: the law is compiled as any
 * formula is, and advanced by backward Euler solved with {@link NewtonSolver} — stable for any step. Heat moves in
 * seconds and a circuit's step in microseconds, so the engine advances it outside the circuit's Newton, from the energy
 * a part took over a stretch of steps. A part's power is the engine's definition, never a formula of
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
    private record LawForm(HeatLaw law, List<String> parameterNames) {
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
     * The heat of a part of {@code component} with these parameter values on a bench at {@code ambient} kelvin; null for a
     * part that does not warm — its component has no law, or gives no {@code thermal_resistance} above nought.
     *
     * @throws IllegalArgumentException when the law does not read, naming the document it is written in
     */
    static PartHeat of(SimulatedComponent component, Map<String, Double> parameters, double ambient) {
        HeatLaw law = component.heat();
        Double resistance = parameters.get(ComponentCompilation.THERMAL_RESISTANCE);
        if (law == null || resistance == null || !(resistance > 0)) {
            return null;
        }
        List<String> parameterNames = parameters.keySet().stream().filter(name -> !name.equals(EngineName.TEMPERATURE.written())).toList();
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
    private static CompiledLaw compile(HeatLaw law, List<String> parameterNames) {
        String where = law.source() + ", heat";
        List<Equation.Derivative> rateLines = new ArrayList<>();
        Equation.Defined temperatureLine = null;
        List<Formula.Definition> library = new ArrayList<>(ModelLanguage.definitions(where, law.formulas()));
        for (Equation line : law.lines()) {
            switch (line) {
                case Equation.Derivative rate -> rateLines.add(rate);
                case Equation.Defined defined when defined.left().written().strip().equals(EngineName.TEMPERATURE.written()) ->
                        temperatureLine = defined;
                // A let is a definition of nothing, read against the library and the lets before it.
                case Equation.Local local -> library.add(new Formula.Definition(local.name(), List.of(),
                        ModelLanguage.read(where + ", let " + local.name(), local.value().written(), library)));
                default -> throw new IllegalArgumentException(where + ": a heat law writes rates d(x)/dt = …, lets and "
                        + EngineName.TEMPERATURE.written() + " = …, not '" + line + "'");
            }
        }
        if (temperatureLine == null) {
            throw new IllegalArgumentException(where + ": the law says no " + EngineName.TEMPERATURE.written() + " = …");
        }
        List<String> stateNames = rateLines.stream().map(Equation.Derivative::state).toList();
        List<String> names = new ArrayList<>(stateNames);
        names.add(EngineName.POWER.written());
        names.add(AMBIENT);
        names.addAll(parameterNames);
        Symbols symbols = Symbols.of(List.of(), names);
        RealFunction[] rates = new RealFunction[stateNames.size()];
        for (int index = 0; index < rates.length; index++) {
            String text = rateLines.get(index).rate().written();
            rates[index] = compile(where, ModelLanguage.read(where, text, library), symbols, text);
        }
        String text = temperatureLine.right().written();
        RealFunction temperature = compile(where, ModelLanguage.read(where, text, library), symbols, text);
        return new CompiledLaw(stateNames.toArray(String[]::new), rates, temperature);
    }

    private static RealFunction compile(String where, Formula formula, Symbols symbols, String text) {
        try {
            return RealCompiler.compile(formula, symbols, text);
        } catch (RuntimeException unreadable) {
            throw new IllegalArgumentException(where + ": '" + text + "' does not read — " + unreadable.getMessage()
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

    /** Its states where they stand, kept apart from it. */
    double[] snapshot() {
        return Arrays.copyOf(arguments, states.length);
    }

    /** Goes on from where {@code previous} — the same law of the part it replaces — had brought its states. */
    void continueFrom(PartHeat previous) {
        if (previous != null) {
            continueFrom(previous.snapshot());
        }
    }

    /** Goes on from states the same law had reached: of the part it replaces, or of itself earlier in the run. */
    void continueFrom(double[] earlier) {
        if (earlier != null && earlier.length == states.length) {
            System.arraycopy(earlier, 0, arguments, 0, states.length);
            temperatureNow = temperature.evaluate(arguments);
        }
    }
}
