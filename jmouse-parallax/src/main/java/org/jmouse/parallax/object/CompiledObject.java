package org.jmouse.parallax.object;

import org.jmouse.formula.Complex;
import org.jmouse.formula.ComplexCompiler;
import org.jmouse.formula.Differentiation;
import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Symbols;

import org.jmouse.parallax.circuit.elaboration.SheetHierarchy;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A definition that has been checked against its parameters, with their values chosen: the one thing every
 * computation reads. It binds an expression of the definition to the kind's variables and hands back something
 * that evaluates; symbolic derivatives come from the same place, so a gradient agrees with its surface.
 */
public final class CompiledObject {

    private final ObjectDefinition definition;
    private final List<ParameterDefinition> parameters;
    private final List<String> parameterNames;
    private final double[] parameterValues;
    private final ComponentLibrary library;
    private final Map<String, BigInteger> exactIntegerValues;
    /** The definition as it was written, before its sheets were unfolded — what other values are unfolded from again. */
    private final ObjectDefinition written;
    /** The blocks the written circuit unfolded; empty for anything that places no sheet. */
    private final List<SheetHierarchy.UnfoldedBlock> unfoldedBlocks;
    /** The heaviest expression bound so far, in elementary evaluations per evaluation ({@link Formula#evaluationWeight()}). */
    private final AtomicLong evaluationWeight = new AtomicLong(1);

    CompiledObject(ObjectDefinition definition, List<ParameterDefinition> parameters, double[] parameterValues) {
        this(definition, parameters, parameterValues, ComponentLibrary.empty());
    }

    CompiledObject(ObjectDefinition definition, List<ParameterDefinition> parameters, double[] parameterValues,
                   ComponentLibrary library) {
        this(definition, parameters, parameterValues, library, Map.of(), definition, List.of());
    }

    /** A circuit whose sheets were unfolded: what it runs as, and what it was written as. */
    CompiledObject(ObjectDefinition definition, List<ParameterDefinition> parameters, double[] parameterValues,
                   ComponentLibrary library, ObjectDefinition written, List<SheetHierarchy.UnfoldedBlock> unfoldedBlocks) {
        this(definition, parameters, parameterValues, library, Map.of(), written, unfoldedBlocks);
    }

    private CompiledObject(ObjectDefinition definition, List<ParameterDefinition> parameters, double[] parameterValues,
                           ComponentLibrary library, Map<String, BigInteger> exactIntegerValues, ObjectDefinition written,
                           List<SheetHierarchy.UnfoldedBlock> unfoldedBlocks) {
        this.written = written;
        this.unfoldedBlocks = List.copyOf(unfoldedBlocks);
        this.library = library;
        this.definition = definition;
        this.parameters = List.copyOf(parameters);
        this.parameterNames = parameters.stream().map(ParameterDefinition::name).toList();
        this.parameterValues = parameterValues.clone();
        this.exactIntegerValues = Map.copyOf(exactIntegerValues);
    }

    public ObjectDefinition definition() {
        return definition;
    }

    public List<ParameterDefinition> parameters() {
        return parameters;
    }

    public double parameterValue(String name) {
        int index = parameterNames.indexOf(name);
        if (index < 0) {
            throw new IllegalArgumentException("'" + name + "' is not a parameter of this object.");
        }
        return parameterValues[index];
    }

    public Map<String, Double> parameterValues() {
        Map<String, Double> values = new LinkedHashMap<>();
        for (int index = 0; index < parameterNames.size(); index++) {
            values.put(parameterNames.get(index), parameterValues[index]);
        }
        return values;
    }

    public Map<String, BigInteger> exactIntegerValues() {
        return exactIntegerValues;
    }

    /**
     * The same object with some of its exact integers set differently: a program's integer inputs, a region's
     * {@code exact} parameters — Tupper's {@code k} for another picture.
     */
    public CompiledObject withExactIntegers(Map<String, String> values) {
        Map<String, String> declared = switch (definition) {
            case ObjectDefinition.Program program -> program.integerInputs();
            case ObjectDefinition.Region region -> region.exactIntegers();
            default -> Map.of();
        };
        if (declared.isEmpty()) {
            if (!values.isEmpty()) {
                throw new IllegalArgumentException("Exact integer inputs belong to a program or a region with exact parameters.");
            }
            return this;
        }
        if (values.size() > 16) {
            throw new IllegalArgumentException("An object accepts at most 16 exact integer inputs.");
        }
        Map<String, BigInteger> parsed = new LinkedHashMap<>();
        values.forEach((name, value) -> {
            if (!declared.containsKey(name)) {
                throw new IllegalArgumentException("'" + name + "' is not a declared exact integer input.");
            }
            if (!name.matches("[A-Za-z_][A-Za-z_0-9]*") || value == null || !value.matches("-?[0-9]{1,4096}")) {
                throw new IllegalArgumentException("'" + name + "' must be a decimal integer of at most 4096 digits.");
            }
            parsed.put(name, new BigInteger(value));
        });
        return new CompiledObject(definition, parameters, parameterValues, library, parsed, written, unfoldedBlocks);
    }

    /** The same object with some parameters set differently — a sweep point, a timeline frame. */
    public CompiledObject withParameters(Map<String, Double> overrides) {
        double[] values = parameterValues.clone();
        overrides.forEach((name, value) -> {
            int index = parameterNames.indexOf(name);
            if (index < 0) {
                throw new IllegalArgumentException("'" + name + "' is not a parameter of this object.");
            }
            values[index] = parameters.get(index).clamp(value);
        });
        if (!unfoldedBlocks.isEmpty()) {
            // A placed sheet's numbers are worked out over these values: it is unfolded again from what was written.
            Map<String, Double> named = new LinkedHashMap<>();
            for (int index = 0; index < parameterNames.size(); index++) {
                named.put(parameterNames.get(index), values[index]);
            }
            return DefinitionCompiler.compile(written, parameters, named, library);
        }
        return new CompiledObject(definition, parameters, values, library, exactIntegerValues, written, unfoldedBlocks);
    }

    /** The same parameters, values and library over another definition — a part of a circuit split for solving. */
    public CompiledObject withDefinition(ObjectDefinition replacement) {
        return new CompiledObject(replacement, parameters, parameterValues, library, exactIntegerValues, written, unfoldedBlocks);
    }

    /**
     * The blocks this circuit's sheets were unfolded from — an inner block before the block that places it —, each
     * with the parts it became; empty when it places no sheet.
     */
    public List<SheetHierarchy.UnfoldedBlock> unfoldedBlocks() {
        return unfoldedBlocks;
    }

    /** The models this object's circuit may reference. */
    public ComponentLibrary library() {
        return library;
    }

    /** The jMC source of a referenced model; refuses a reference the library does not hold. */
    public String componentSource(String reference) {
        return library.source(reference).orElseThrow(() -> new IllegalArgumentException(
                "No component '" + reference + "': a built-in model is named alone (lm358), a library one with its revision (my-part@2)."));
    }

    public BoundFunction function(String source, List<String> variables) {
        Formula expression = FormulaLanguage.standard().parse(source);
        return bind(expression, variables, source);
    }

    public BoundFunction function(Formula expression, List<String> variables) {
        return bind(expression, variables, null);
    }

    /** ∂f/∂variable, exact. */
    public BoundFunction derivative(String source, List<String> variables, String variable) {
        Formula derivative = Differentiation.derivative(FormulaLanguage.standard().parse(source), variable);
        return bind(derivative, variables, source);
    }

    public Formula derivativeExpression(String source, String variable) {
        return Differentiation.derivative(FormulaLanguage.standard().parse(source), variable);
    }

    /**
     * The derivative of an expression already read. A second derivative is taken from the first as a tree: printed to
     * text and read again, it loses the functions the document defined, and `f` is no longer known.
     */
    public Formula derivativeExpression(Formula expression, String variable) {
        return Differentiation.derivative(expression, variable);
    }

    /** An expression read from its source, with the functions it defines. */
    public Formula expression(String source) {
        return FormulaLanguage.standard().parse(source);
    }

    public List<BoundFunction> functions(List<String> sources, List<String> variables) {
        List<BoundFunction> functions = new ArrayList<>(sources.size());
        for (String source : sources) {
            functions.add(function(source, variables));
        }
        return functions;
    }

    /** A complex function of the given complex variables; the parameters enter as real numbers. */
    public ComplexBinding complexFunction(String source, List<String> variables) {
        return complexBinding(FormulaLanguage.standard().parse(source), variables, source);
    }

    /** A complex function of an expression already read — a part of a larger one. */
    public ComplexBinding complexFunction(Formula expression, List<String> variables) {
        return complexBinding(expression, variables, null);
    }

    private ComplexBinding complexBinding(Formula expression, List<String> variables, String source) {
        weigh(expression);
        Symbols symbols = Symbols.of(variables, parameterNames);
        ComplexCompiler.ComplexFunction function = ComplexCompiler.compile(expression, symbols, source);
        return new ComplexBinding(function, variables.size(), parameterValues);
    }

    /**
     * An expression that may call the random functions of {@link Randomness}, bound to the given variables; each call
     * becomes a draw made before every evaluation, its arguments evaluated now from the parameters.
     *
     * @param noise whether {@code white_noise()} may be called: true for a derivative only
     */
    public StochasticFunction stochasticFunction(String source, List<String> variables, boolean noise) {
        List<Randomness.BoundDraw> draws = new ArrayList<>();
        Formula bound = Randomness.bind(Randomness.parse(source), draws, argument -> {
            if (!argument.variables().stream().allMatch(parameterNames::contains)) {
                throw new FormulaException("A random function's arguments are numbers or parameters, "
                        + "not " + argument.variables() + ".", source, -1);
            }
            return bind(argument, List.of(), source).at();
        }, noise);
        weigh(bound);
        Symbols symbols = Symbols.withSlots(draws.size(), variables, parameterNames);
        RealFunction compiled = RealCompiler.compile(bound, symbols, source);
        return new StochasticFunction(compiled, draws, variables.size(), parameterValues);
    }

    /** The value of a parameter-only expression, such as a matrix entry. */
    public double constant(String source) {
        return function(source, List.of()).at();
    }

    /**
     * A value as a part line writes it: a quantity with its unit ({@code 10 kΩ}, {@code 50 ppm/K}) read in SI, or an
     * expression of the parameters ({@code P}, {@code 2 * R}) evaluated at their values — the one reader of what a part
     * sets, so a display, a meter and the solver never read one setting two ways.
     */
    public double value(String written) {
        return org.jmouse.parallax.script.Quantity.parse(written).map(org.jmouse.parallax.script.Quantity::value)
                .orElseGet(() -> constant(written));
    }

    /**
     * What one evaluation of this object's heaviest expression costs, in elementary evaluations — known once its
     * expressions were bound, which compiling a definition does. A computation's estimate is per evaluation; this is
     * what turns it into a cost when the expression is itself an integral or a sum.
     */
    public long evaluationWeight() {
        return evaluationWeight.get();
    }

    private void weigh(Formula expression) {
        evaluationWeight.accumulateAndGet(expression.evaluationWeight(), Math::max);
    }

    private BoundFunction bind(Formula expression, List<String> variables, String source) {
        weigh(expression);
        Symbols symbols = Symbols.of(variables, parameterNames);
        RealFunction compiled = RealCompiler.compile(expression, symbols, source);
        return new BoundFunction(expression, compiled, variables.size(), parameterValues);
    }

    /** A compiled complex function with its parameters in place. Not thread-safe, like {@link BoundFunction}. */
    public static final class ComplexBinding {

        private final ComplexCompiler.ComplexFunction function;
        private final Complex[] buffer;
        private final int variableCount;

        ComplexBinding(ComplexCompiler.ComplexFunction function, int variableCount, double[] parameterValues) {
            this.function = function;
            this.variableCount = variableCount;
            this.buffer = new Complex[variableCount + parameterValues.length];
            for (int index = 0; index < parameterValues.length; index++) {
                buffer[variableCount + index] = Complex.of(parameterValues[index]);
            }
        }

        public Complex at(Complex first) {
            buffer[0] = first;
            return function.evaluate(buffer);
        }

        public Complex at(Complex first, Complex second) {
            buffer[0] = first;
            buffer[1] = second;
            return function.evaluate(buffer);
        }

        public int variableCount() {
            return variableCount;
        }
    }
}
