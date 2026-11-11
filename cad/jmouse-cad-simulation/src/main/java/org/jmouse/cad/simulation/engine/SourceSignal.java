package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.component.Equation;
import org.jmouse.cad.simulation.EngineName;
import org.jmouse.cad.simulation.ModelLanguage;
import org.jmouse.cad.simulation.SimulatedComponent;
import org.jmouse.cad.component.Pin;

import org.jmouse.formula.Formula;
import org.jmouse.formula.MathematicalConstant;
import org.jmouse.formula.MathematicalFunction;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Symbols;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A generator's voltage as a function of time: a component whose only equation is {@code voltage positive -> negative
 * = f(time, parameters)} — no state, no behaviour, no pin read on the right — is an ideal source, and the engine treats it
 * as one: it holds its node against the other, it cuts the circuit there, it drives the frequency response. What it
 * does is written in its document, never in this class.
 *
 * <p>Its edges come from the expression too. Every place where it can jump — a comparison {@code l < r}, a
 * {@code step(x)} or {@code sign(x)}, a {@code floor(x)}, a {@code mod(x, p)} — gives an edge function that changes
 * sign exactly there ({@code l − r}, {@code x}, {@code sin(πx)}, {@code sin(πx/p)}); the solver finds the root within a
 * step and lands on it, as it lands on a timer's edge. A square written as {@code mod(frequency * time, 1) < duty} gets
 * both its edges that way, with no square wave known to the engine.</p>
 */
final class SourceSignal {

    /** The kind of a branch equation that drives a current rather than holds a voltage. */
    private static final String CURRENT = "current";

    /** How many pieces a span is cut into when its edges are looked for: a pulse narrower than a piece can be missed. */
    private static final int SCAN_PIECES = 16;
    /** How close a located edge is to the true instant, as a share of the span it was found in. */
    private static final int BISECTIONS = 60;
    /** How many times the shortest stretch between edges is longer than a step may be. */
    private static final double EDGE_SHARE = 2.5;

    /**
     * A generator's signal compiled for one list of parameter names — its value and its edge functions over the time and
     * the parameters —, or none where the model turned out not to be an ideal source. The values are arguments, so every
     * generator of one document shares it.
     */
    private record CompiledSignal(RealFunction value, List<RealFunction> edges) {
    }

    /** What decides a compiled signal: what the model writes of it, and the names of its parameters. */
    private record SignalForm(String name, List<Equation> formulas, List<Equation.Local> locals, Equation.Branch equation,
                              List<String> pinNames, List<String> parameterNames) {
    }

    /** How many compiled signals are kept, as {@link ModelEquations} keeps its forms. */
    private static final int KEPT_SIGNALS = 256;

    /** The signals compiled so far: a netlist is built for every run and every piece, and read its generators each time. */
    private static final Map<SignalForm, Optional<CompiledSignal>> COMPILED = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<SignalForm, Optional<CompiledSignal>> eldest) {
                    return size() > KEPT_SIGNALS;
                }
            });

    /**
     * Where a signal jumps: the last instant at its old level and the first at its new, no further apart than the edge
     * was located to.
     */
    record Edge(double before, double after) {
    }

    private final RealFunction value;
    private final List<RealFunction> edges;
    private final double[] arguments;
    /** Where each edge function was last read, and what it read: a step starts where the one before ended. */
    private final double[] lastTime;
    private final double[] lastValue;
    /** The longest step that cannot hold two of its edges, found once; infinite for a signal without edges. */
    private double maximumStep = Double.NaN;

    /** The generator's own pins, which its current is reported into: `positive`, not a primitive's "+". */
    private final String positiveTerminal;
    private final String negativeTerminal;

    private SourceSignal(RealFunction value, List<RealFunction> edges, double[] arguments, String positiveTerminal,
                         String negativeTerminal) {
        this.positiveTerminal = positiveTerminal;
        this.negativeTerminal = negativeTerminal;
        this.value = value;
        this.edges = List.copyOf(edges);
        this.arguments = arguments;
        this.lastTime = new double[edges.size()];
        this.lastValue = new double[edges.size()];
        Arrays.fill(lastTime, Double.NaN);
    }

    /**
     * The longest step the signal allows: under half the shortest stretch between two of its edges, so no step holds two
     * ({@link #edgeWithinStep} sees one sign change), infinite for a signal that never jumps. It was a tenth — ten steps
     * through every flat stretch of a square wave, 670 000 a second for 20 kHz into a resistor, where the error asked for
     * two; how finely the circuit's answer is stepped between edges is the error's to decide.
     */
    synchronized double maximumStep() {
        if (Double.isNaN(maximumStep)) {
            maximumStep = Double.POSITIVE_INFINITY;
            List<Double> found = new ArrayList<>();
            double from = 0;
            double span = 1e-9;
            while (found.size() < 3 && span < 1e3) {
                double edge = firstEdge(from, from + span, span * 1e-6, SCAN_PIECES);
                if (Double.isNaN(edge)) {
                    from += span;
                    span *= 2;
                } else {
                    found.add(edge);
                    from = edge;
                }
            }
            for (int index = 1; index < found.size(); index++) {
                maximumStep = Math.min(maximumStep, (found.get(index) - found.get(index - 1)) / EDGE_SHARE);
            }
        }
        return maximumStep;
    }

    /**
     * The model as an ideal source, when it is one: one voltage equation between its only two pins, no state, no
     * behaviour, no internal node, and a right side that reads nothing but the time and the parameters.
     */
    static Optional<SourceSignal> of(SimulatedComponent component, Map<String, Double> parameters) {
        Optional<Equation.Branch> drive = component.generated();
        if (drive.isEmpty()) {
            return Optional.empty();
        }
        List<String> parameterNames = List.copyOf(parameters.keySet());
        List<String> pinNames = component.pins().stream().map(Pin::name).toList();
        SignalForm form = new SignalForm(component.title(), component.formulas(), component.locals(), drive.get(), pinNames,
                parameterNames);
        Optional<CompiledSignal> compiled = COMPILED.get(form);
        if (compiled == null) {
            compiled = compile(component, drive.get(), pinNames, parameterNames);
            COMPILED.put(form, compiled);
        }
        if (compiled.isEmpty()) {
            return Optional.empty();
        }
        double[] arguments = new double[1 + parameterNames.size()];
        for (int index = 0; index < parameterNames.size(); index++) {
            arguments[1 + index] = parameters.get(parameterNames.get(index));
        }
        return Optional.of(new SourceSignal(compiled.get().value(), compiled.get().edges(), arguments, drive.get().from(),
                drive.get().to()));
    }

    /**
     * The generator's signal read and compiled over the time and these parameters; none where it reads a pin or a host
     * call. Its lets are definitions of nothing, as a component's are — each read by jMF against the library and the
     * lets before it, built from what jMF read and never from a text put together for it.
     */
    private static Optional<CompiledSignal> compile(SimulatedComponent component, Equation.Branch drive, List<String> pinNames,
                                                    List<String> parameterNames) {
        String title = component.title();
        List<Formula.Definition> known = new ArrayList<>(ModelLanguage.definitions(title, component.formulas()));
        for (Equation.Local local : component.locals()) {
            known.add(new Formula.Definition(local.name(), List.of(),
                    ModelLanguage.read(title + ", let " + local.name(), local.value().written(), known)));
        }
        Formula written = ModelLanguage.read(title, drive.value().written(), known);
        // A current is written through the part, positive to negative; a source is driven by what it pushes out of its
        // positive pin — the same current, the other way.
        Formula formula = drive.kind().equals(CURRENT) ? new Formula.Negation(written) : written;
        if (formula.mentionsHostCall()) {
            return Optional.empty();
        }
        for (String pin : pinNames) {
            if (formula.dependsOn(pin)) {
                return Optional.empty();
            }
        }
        Symbols table = Symbols.of(List.of(EngineName.TIME.written()), parameterNames);
        List<RealFunction> edges = new ArrayList<>();
        for (Formula edge : edgeFunctions(formula)) {
            edges.add(RealCompiler.compile(edge, table, title + " edge"));
        }
        return Optional.of(new CompiledSignal(RealCompiler.compile(formula, table, drive.value().written()), List.copyOf(edges)));
    }

    /**
     * What a generator is to the solver: an ideal voltage source for a {@code voltage a -> b = …}, an ideal current
     * source for a {@code current a -> b = …}.
     */
    static Netlist.Element.Kind kindOf(Equation.Branch drive) {
        return drive.kind().equals(CURRENT) ? Netlist.Element.Kind.CURRENT_SOURCE : Netlist.Element.Kind.VOLTAGE_SOURCE;
    }

    /** Its own pins, positive first: the terminals its current is reported into. */
    String[] pinNames() {
        return new String[]{positiveTerminal, negativeTerminal};
    }



    double valueAt(double time) {
        synchronized (arguments) {
            arguments[0] = time;
            return value.evaluate(arguments);
        }
    }

    /**
     * The first edge after {@code from} and no later than {@code to} within one step no longer than
     * {@link #maximumStep()}, which cannot hold two edges of one function — one reading at its end, the one at its start
     * kept from the step before —, or null when the step has none.
     */
    Edge edgeWithinStep(double from, double to, double resolution) {
        if (edges.isEmpty() || !(to > from)) {
            return null;
        }
        Edge first = null;
        synchronized (arguments) {
            for (int index = 0; index < edges.size(); index++) {
                RealFunction edge = edges.get(index);
                double before = lastTime[index] == from ? lastValue[index] : sampled(edge, from);
                double after = sampled(edge, to);
                lastTime[index] = to;
                lastValue[index] = after;
                if (changesSign(before, after)) {
                    Edge found = root(edge, from, to, before, resolution);
                    first = first == null || found.after() < first.after() ? found : first;
                }
            }
        }
        return first;
    }

    /** The first edge in a span of any length, scanned in {@code pieces}: a pulse narrower than a piece can be missed. */
    private double firstEdge(double from, double to, double resolution, int pieces) {
        if (edges.isEmpty() || !(to > from)) {
            return Double.NaN;
        }
        double first = Double.NaN;
        double piece = (to - from) / pieces;
        synchronized (arguments) {
            for (RealFunction edge : edges) {
                double before = sampled(edge, from);
                for (int index = 1; index <= pieces; index++) {
                    double at = index == pieces ? to : from + piece * index;
                    double after = sampled(edge, at);
                    if (changesSign(before, after)) {
                        double found = root(edge, at - piece, at, before, resolution).after();
                        first = Double.isNaN(first) ? found : Math.min(first, found);
                        break;
                    }
                    before = after;
                }
            }
        }
        return first;
    }

    private double sampled(RealFunction edge, double time) {
        arguments[0] = time;
        return edge.evaluate(arguments);
    }

    private static boolean changesSign(double before, double after) {
        return (before < 0 && after >= 0) || (before >= 0 && after < 0);
    }

    /** Bisection to the instant the sign changes: the two ends that hold it between them. */
    private Edge root(RealFunction edge, double low, double high, double atLow, double resolution) {
        double lowSign = atLow;
        for (int iteration = 0; iteration < BISECTIONS && high - low > resolution; iteration++) {
            double middle = (low + high) / 2;
            double atMiddle = sampled(edge, middle);
            if (changesSign(lowSign, atMiddle)) {
                high = middle;
            } else {
                low = middle;
                lowSign = atMiddle;
            }
        }
        return new Edge(low, high);
    }

    /**
     * The functions whose sign change marks each place the expression can jump. A {@code floor} or a {@code mod} read by
     * a comparison needs none of its own: the value jumps only where the comparison flips — {@code mod(f·t, 1) < duty}
     * flips at the wrap too, since it goes from 1 − duty to −duty —, so one edge function covers both its edges.
     */
    private static List<Formula> edgeFunctions(Formula formula) {
        List<Formula> edges = new ArrayList<>();
        collect(formula, edges, false);
        return edges;
    }

    private static void collect(Formula formula, List<Formula> edges, boolean compared) {
        switch (formula) {
            case Formula.Comparison comparison -> {
                edges.add(new Formula.Binary(Formula.Operator.SUBTRACT, comparison.left(), comparison.right()));
                collect(comparison.left(), edges, true);
                collect(comparison.right(), edges, true);
            }
            case Formula.Call call -> {
                List<Formula> arguments = call.arguments();
                MathematicalFunction function = call.function();
                if ((function == MathematicalFunction.STEP || function == MathematicalFunction.SIGN) && arguments.size() == 1) {
                    edges.add(arguments.getFirst());
                } else if (!compared && function == MathematicalFunction.FLOOR && arguments.size() == 1) {
                    edges.add(sinePi(arguments.getFirst()));
                } else if (!compared && function == MathematicalFunction.MOD && arguments.size() == 2) {
                    edges.add(sinePi(new Formula.Binary(Formula.Operator.DIVIDE, arguments.get(0), arguments.get(1))));
                }
                arguments.forEach(argument -> collect(argument, edges, compared));
            }
            case Formula.Binary binary -> {
                collect(binary.left(), edges, compared);
                collect(binary.right(), edges, compared);
            }
            case Formula.Negation negation -> collect(negation.operand(), edges, compared);
            case Formula.Not not -> collect(not.operand(), edges, compared);
            case Formula.Logical logical -> {
                collect(logical.left(), edges, compared);
                collect(logical.right(), edges, compared);
            }
            case Formula.Piecewise piecewise -> {
                piecewise.branches().forEach(branch -> {
                    collect(branch.condition(), edges, compared);
                    collect(branch.value(), edges, compared);
                });
                if (piecewise.otherwise() != null) {
                    collect(piecewise.otherwise(), edges, compared);
                }
            }
            // A called definition's body reads its own parameters; a jump inside it is not seen from here — a generator
            // writes its jumps where it reads the time.
            case Formula.Application application -> application.arguments().forEach(argument -> collect(argument, edges, compared));
            default -> {
            }
        }
    }

    private static Formula sinePi(Formula argument) {
        return new Formula.Call(MathematicalFunction.SIN, List.of(new Formula.Binary(Formula.Operator.MULTIPLY,
                new Formula.NamedConstant(MathematicalConstant.PI), argument)));
    }
}
