package org.jmouse.formula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

/**
 * Compiles an expression into a straight-line program — a tape — that gives its value and its partial derivatives by
 * chosen symbols in one forward and one backward pass (reverse-mode automatic differentiation).
 *
 * <p>Why not a value function and one symbolic derivative per symbol, as {@link RealCompiler} and
 * {@link Differentiation} give: a circuit model is a few lines whose locals and smooth helpers expand into large trees,
 * and every symbolic derivative carries its own copy of them — a junction's {@code exp}, a knee's {@code ln(1 + exp)}
 * — so one Newton iteration evaluated the same subexpressions dozens of times. Here every distinct subexpression is
 * one instruction, computed once, whatever the number of derivatives; a defined function is inlined, and its repeated
 * arguments merge like any other repeated subexpression.</p>
 *
 * <p>Both branches of a condition are computed (a tape has no jumps) and the condition selects; the derivative flows
 * only into the selected branch. Sums, products and integrals are not taped: {@link #compile} answers null for them
 * and the caller keeps the symbolic route.</p>
 */
public final class GradientCompiler {

    private GradientCompiler() {
    }

    /** A compiled tape. Not safe for use by two threads at once: it evaluates into its own scratch arrays. */
    public interface GradientFunction {

        /**
         * The value at {@code arguments}; {@code gradient[k]} receives the partial derivative by the argument at
         * {@code targets[k]} of the compile call.
         */
        double evaluate(double[] arguments, double[] gradient);

        /**
         * The same tape with scratch space of its own, for another caller — compiling a tape costs far more than
         * copying one. The program is shared, and so is its code once it is compiled.
         */
        GradientFunction copy();

        /** Whether the tape is turned into code: a larger one, or one where no compiler is present, stays interpreted. */
        boolean becomesCode();
    }

    /** What {@link TapeCompiler} reads of a tape to write it out as code. */
    interface TapeView {
        int result();

        int[][] inputs();

        double[] constants();

        int[] arguments();

        int[] targetInstructions();

        String operation(int index);

        /** The function an instruction calls, by its enum name. */
        String function(int index);

        /** A comparison as the Java operator. */
        String relation(int index);

        /** Whether an instruction reads constants and invariant arguments alone. */
        boolean invariant(int index);
    }

    private enum Operation {
        CONSTANT, ARGUMENT, NEGATE, ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER, INTEGER_POWER, CALL, COMPARE, AND, OR, NOT, SELECT
    }

    /**
     * The tape of {@code expression} over the symbols' argument layout, differentiating by the arguments at
     * {@code targets}; null when the expression holds something a tape cannot carry.
     */
    public static GradientFunction compile(Formula expression, Symbols symbols, int[] targets) {
        return compile(expression, symbols, targets, Integer.MAX_VALUE);
    }

    /**
     * The same, told that the arguments from {@code invariantFrom} on — a model's parameters — hold their values over
     * many evaluations: what the tape computes from them and constants alone is computed again only when one of them
     * changes, and no derivative flows into it.
     */
    public static GradientFunction compile(Formula expression, Symbols symbols, int[] targets, int invariantFrom) {
        try {
            Builder builder = new Builder(symbols);
            int result = builder.emit(expression, Map.of());
            return builder.finish(result, targets, invariantFrom);
        } catch (Untaped untaped) {
            return null;
        }
    }

    private static final class Untaped extends RuntimeException {
        Untaped() {
            super(null, null, false, false);
        }
    }

    private static final class Builder {

        private final Symbols symbols;
        private final List<Operation> operations = new ArrayList<>();
        private final List<int[]> operands = new ArrayList<>();
        private final List<Object> payloads = new ArrayList<>();
        private final Map<String, Integer> seen = new HashMap<>();

        Builder(Symbols symbols) {
            this.symbols = symbols;
        }

        /** One instruction, or the existing one that computes the same thing. */
        private int add(Operation operation, Object payload, int... inputs) {
            StringBuilder key = new StringBuilder(operation.name()).append(':').append(payload);
            for (int input : inputs) {
                key.append(',').append(input);
            }
            return seen.computeIfAbsent(key.toString(), ignored -> {
                operations.add(operation);
                operands.add(inputs);
                payloads.add(payload);
                return operations.size() - 1;
            });
        }

        private int constant(double value) {
            return add(Operation.CONSTANT, value);
        }

        int emit(Formula expression, Map<String, Integer> bound) {
            return switch (expression) {
                case Formula.Constant constant -> constant(constant.value());
                case Formula.NamedConstant named -> constant(named.constant().value());
                case Formula.Variable variable -> {
                    Integer local = bound.get(variable.name());
                    if (local != null) {
                        yield local;
                    }
                    int slot = symbols.slotOf(variable.name());
                    if (slot < 0) {
                        throw new Untaped();
                    }
                    yield add(Operation.ARGUMENT, slot);
                }
                case Formula.Slot slot -> add(Operation.ARGUMENT, slot.index());
                case Formula.Negation negation -> add(Operation.NEGATE, null, emit(negation.operand(), bound));
                case Formula.Binary binary -> binary(binary, bound);
                case Formula.Call call -> {
                    int[] inputs = new int[call.arguments().size()];
                    for (int index = 0; index < inputs.length; index++) {
                        inputs[index] = emit(call.arguments().get(index), bound);
                    }
                    yield add(Operation.CALL, call.function(), inputs);
                }
                case Formula.Comparison comparison -> add(Operation.COMPARE, comparison.relation(),
                        emit(comparison.left(), bound), emit(comparison.right(), bound));
                case Formula.Logical logical -> add(logical.connective() == Formula.Connective.AND ? Operation.AND : Operation.OR,
                        null, emit(logical.left(), bound), emit(logical.right(), bound));
                case Formula.Not not -> add(Operation.NOT, null, emit(not.operand(), bound));
                case Formula.Equation equation -> add(Operation.SUBTRACT, null, emit(equation.left(), bound), emit(equation.right(), bound));
                case Formula.Piecewise piecewise -> {
                    int otherwise = piecewise.otherwise() == null ? constant(Double.NaN) : emit(piecewise.otherwise(), bound);
                    for (int index = piecewise.branches().size() - 1; index >= 0; index--) {
                        Formula.Piecewise.Branch branch = piecewise.branches().get(index);
                        otherwise = add(Operation.SELECT, null, emit(branch.condition(), bound), emit(branch.value(), bound), otherwise);
                    }
                    yield otherwise;
                }
                case Formula.Application application -> {
                    // Inlined: the body sees each parameter as the instruction its argument computed.
                    Map<String, Integer> parameters = new HashMap<>();
                    List<String> names = application.definition().parameters();
                    for (int index = 0; index < names.size(); index++) {
                        parameters.put(names.get(index), emit(application.arguments().get(index), bound));
                    }
                    yield emit(application.definition().body(), parameters);
                }
                case Formula.ImaginaryUnit unit -> throw new Untaped();
                case Formula.HostCall host -> throw new Untaped();
                case Formula.Accumulation accumulation -> throw new Untaped();
                case Formula.Integral integral -> throw new Untaped();
            };
        }

        private int binary(Formula.Binary binary, Map<String, Integer> bound) {
            int left = emit(binary.left(), bound);
            if (binary.operator() == Formula.Operator.POWER && binary.right() instanceof Formula.Constant exponent
                    && exponent.value() == Math.rint(exponent.value()) && Math.abs(exponent.value()) <= 16) {
                return add(Operation.INTEGER_POWER, (int) exponent.value(), left);
            }
            int right = emit(binary.right(), bound);
            return switch (binary.operator()) {
                case ADD -> add(Operation.ADD, null, left, right);
                case SUBTRACT -> add(Operation.SUBTRACT, null, left, right);
                case MULTIPLY -> add(Operation.MULTIPLY, null, left, right);
                case DIVIDE -> add(Operation.DIVIDE, null, left, right);
                case POWER -> add(Operation.POWER, null, left, right);
            };
        }

        GradientFunction finish(int result, int[] targets, int invariantFrom) {
            int size = operations.size();
            Operation[] operation = operations.toArray(Operation[]::new);
            int[][] inputs = operands.toArray(int[][]::new);
            Object[] payload = payloads.toArray();
            double[] constants = new double[size];
            int[] arguments = new int[size];
            MathematicalFunction.RealImplementation[] functions = new MathematicalFunction.RealImplementation[size];
            RealFunction[][] derivatives = new RealFunction[size][];
            Formula.Relation[] relations = new Formula.Relation[size];
            for (int index = 0; index < size; index++) {
                switch (operation[index]) {
                    case CONSTANT -> constants[index] = (Double) payload[index];
                    case ARGUMENT -> arguments[index] = (Integer) payload[index];
                    case INTEGER_POWER -> constants[index] = (Integer) payload[index];
                    case CALL -> {
                        MathematicalFunction function = (MathematicalFunction) payload[index];
                        functions[index] = function.real();
                        derivatives[index] = Derivatives.of(function, inputs[index].length);
                    }
                    case COMPARE -> relations[index] = (Formula.Relation) payload[index];
                    default -> {
                    }
                }
            }
            // Which instruction reads each target: the gradient is the adjoint found there.
            int[] targetInstructions = new int[targets.length];
            for (int target = 0; target < targets.length; target++) {
                targetInstructions[target] = -1;
                for (int index = 0; index < size; index++) {
                    if (operation[index] == Operation.ARGUMENT && arguments[index] == targets[target]) {
                        targetInstructions[target] = index;
                    }
                }
            }
            MathematicalFunction[] callees = new MathematicalFunction[size];
            for (int index = 0; index < size; index++) {
                if (operation[index] == Operation.CALL) {
                    callees[index] = (MathematicalFunction) payload[index];
                }
            }
            // An instruction is invariant when it reads constants and invariant arguments alone.
            boolean[] invariant = new boolean[size];
            for (int index = 0; index < size; index++) {
                invariant[index] = switch (operation[index]) {
                    case CONSTANT -> true;
                    case ARGUMENT -> arguments[index] >= invariantFrom;
                    default -> {
                        boolean fixed = true;
                        for (int input : inputs[index]) {
                            fixed &= invariant[input];
                        }
                        yield fixed;
                    }
                };
            }
            Tape tape = new Tape(size, result, operation, inputs, constants, arguments, functions, derivatives, relations,
                    targetInstructions, callees, invariant);
            tape.accelerate();
            return tape;
        }
    }

    /** The derivative of each built-in function by each of its arguments, compiled once per function and shared. */
    private static final class Derivatives {

        private static final Map<String, RealFunction[]> CACHE = new ConcurrentHashMap<>();

        /**
         * The functions that stand still between their jumps: their derivative is 0 wherever there is one, which is what
         * Newton is given, as SPICE gives it. Any other function a derivative cannot be found for is no function a tape
         * carries — its partial would be a guess, and a tape that guesses 0 for {@code min} left Newton blind.
         */
        private static final Set<MathematicalFunction> STEPPED = EnumSet.of(MathematicalFunction.SIGN, MathematicalFunction.FLOOR,
                MathematicalFunction.CEIL, MathematicalFunction.ROUND, MathematicalFunction.STEP);

        /** @throws Untaped when a derivative of a function that is not {@link #STEPPED} cannot be found */
        static RealFunction[] of(MathematicalFunction function, int arity) {
            return CACHE.computeIfAbsent(function.name() + "/" + arity, ignored -> {
                List<String> names = new ArrayList<>();
                List<Formula> variables = new ArrayList<>();
                for (int index = 0; index < arity; index++) {
                    names.add("argument" + index);
                    variables.add(new Formula.Variable("argument" + index));
                }
                Symbols table = Symbols.of(names.toArray(String[]::new));
                Formula call = new Formula.Call(function, variables);
                RealFunction[] derivatives = new RealFunction[arity];
                for (int index = 0; index < arity; index++) {
                    if (STEPPED.contains(function)) {
                        derivatives[index] = values -> 0;
                        continue;
                    }
                    try {
                        derivatives[index] = RealCompiler.compile(Differentiation.derivative(call, names.get(index)), table, function.name());
                    } catch (RuntimeException undifferentiable) {
                        throw new Untaped();
                    }
                }
                return derivatives;
            });
        }
    }

    /** A tape's compiled code, once {@link TapeCompiler} has it: a new instance of it for each copy of the tape. */
    private static final class Code {
        private volatile Supplier<ToDoubleBiFunction<double[], double[]>> instances;
    }

    private static final class Tape implements GradientFunction, TapeView {

        private final int size;
        private final int result;
        private final Operation[] operation;
        private final int[][] inputs;
        private final double[] constants;
        private final int[] arguments;
        private final MathematicalFunction.RealImplementation[] functions;
        private final RealFunction[][] derivatives;
        private final Formula.Relation[] relations;
        private final int[] targetInstructions;
        private final double[] values;
        private final double[] adjoints;
        private final double[] one = new double[1];
        private final double[] two = new double[2];
        private final MathematicalFunction[] callees;
        /** Which instructions read constants and invariant arguments alone; computed only when those arguments change. */
        private final boolean[] invariant;
        /** The invariant arguments' positions, and their values when the invariant instructions were last computed. */
        private final int[] invariantPositions;
        private final double[] held;
        private boolean primed;
        /** Where the tape's compiled code arrives, shared by every copy of it. */
        private final Code code;
        /** This copy's own instance of that code — it holds the invariant values —; until there is one, it interprets. */
        private ToDoubleBiFunction<double[], double[]> compiled;

        Tape(int size, int result, Operation[] operation, int[][] inputs, double[] constants, int[] arguments,
             MathematicalFunction.RealImplementation[] functions, RealFunction[][] derivatives,
             Formula.Relation[] relations, int[] targetInstructions, MathematicalFunction[] callees, boolean[] invariant) {
            this.callees = callees;
            this.invariant = invariant;
            this.invariantPositions = IntStream.range(0, size)
                    .filter(index -> operation[index] == Operation.ARGUMENT && invariant[index])
                    .map(index -> arguments[index]).distinct().toArray();
            this.held = new double[invariantPositions.length];
            this.size = size;
            this.result = result;
            this.operation = operation;
            this.inputs = inputs;
            this.constants = constants;
            this.arguments = arguments;
            this.functions = functions;
            this.derivatives = derivatives;
            this.relations = relations;
            this.targetInstructions = targetInstructions;
            this.values = new double[size];
            this.adjoints = new double[size];
            this.code = new Code();
        }

        /** A copy: the same program and code, its own scratch arrays and invariant values. */
        private Tape(Tape original) {
            this.callees = original.callees;
            this.invariant = original.invariant;
            this.invariantPositions = original.invariantPositions;
            this.held = new double[invariantPositions.length];
            this.size = original.size;
            this.result = original.result;
            this.operation = original.operation;
            this.inputs = original.inputs;
            this.constants = original.constants;
            this.arguments = original.arguments;
            this.functions = original.functions;
            this.derivatives = original.derivatives;
            this.relations = original.relations;
            this.targetInstructions = original.targetInstructions;
            this.values = new double[size];
            this.adjoints = new double[size];
            this.code = original.code;
        }

        @Override
        public GradientFunction copy() {
            return new Tape(this);
        }

        @Override
        public boolean becomesCode() {
            return TapeCompiler.available() && result + 1 <= TapeCompiler.largestTape();
        }

        /** Asks for this tape as code; it keeps interpreting until the code arrives. */
        void accelerate() {
            if (!becomesCode()) {
                return;
            }
            List<String> slots = new ArrayList<>();
            String template = TapeCompiler.source("TAPE_CLASS", this, slots);
            String name = "Tape" + Integer.toHexString(template.hashCode()) + Integer.toHexString(template.length());
            @SuppressWarnings("unchecked")
            ToDoubleFunction<double[]>[] calls = new ToDoubleFunction[slots.size()];
            @SuppressWarnings("unchecked")
            ToDoubleFunction<double[]>[][] partials = new ToDoubleFunction[slots.size()][];
            for (int slot = 0; slot < slots.size(); slot++) {
                int index = Integer.parseInt(slots.get(slot));
                MathematicalFunction.RealImplementation implementation = functions[index];
                calls[slot] = implementation::apply;
                RealFunction[] byArgument = derivatives[index];
                partials[slot] = new ToDoubleFunction[byArgument.length];
                for (int argument = 0; argument < byArgument.length; argument++) {
                    partials[slot][argument] = byArgument[argument]::evaluate;
                }
            }
            TapeCompiler.submit(new TapeCompiler.Request(template.replace("TAPE_CLASS", name), name,
                    new Object[] {calls, partials}, instances -> code.instances = instances));
        }

        @Override
        public int result() {
            return result;
        }

        @Override
        public int[][] inputs() {
            return inputs;
        }

        @Override
        public double[] constants() {
            return constants;
        }

        @Override
        public int[] arguments() {
            return arguments;
        }

        @Override
        public int[] targetInstructions() {
            return targetInstructions;
        }

        @Override
        public String operation(int index) {
            return operation[index].name();
        }

        @Override
        public String function(int index) {
            return callees[index].name();
        }

        @Override
        public boolean invariant(int index) {
            return invariant[index];
        }

        @Override
        public String relation(int index) {
            return switch (relations[index]) {
                case LESS -> "<";
                case LESS_OR_EQUAL -> "<=";
                case GREATER -> ">";
                case GREATER_OR_EQUAL -> ">=";
                case EQUAL -> "==";
                case NOT_EQUAL -> "!=";
            };
        }

        @Override
        public double evaluate(double[] argument, double[] gradient) {
            ToDoubleBiFunction<double[], double[]> own = compiled;
            if (own == null) {
                Supplier<ToDoubleBiFunction<double[], double[]>> instances = code.instances;
                if (instances != null) {
                    own = instances.get();
                    compiled = own;
                }
            }
            if (own != null) {
                return own.applyAsDouble(argument, gradient);
            }
            if (!holds(argument)) {
                for (int index = 0; index <= result; index++) {
                    if (invariant[index]) {
                        values[index] = compute(index, argument);
                    }
                }
            }
            for (int index = 0; index <= result; index++) {
                if (!invariant[index]) {
                    values[index] = compute(index, argument);
                }
            }
            if (gradient != null && gradient.length > 0) {
                backward();
                for (int target = 0; target < targetInstructions.length; target++) {
                    int instruction = targetInstructions[target];
                    // An invariant target owes nothing, as in the compiled code.
                    gradient[target] = instruction < 0 || invariant[instruction] ? 0 : adjoints[instruction];
                }
            }
            return values[result];
        }

        /**
         * Whether the invariant instructions still hold for these arguments; if not, records the arguments they are about
         * to be computed for.
         */
        private boolean holds(double[] argument) {
            boolean same = primed;
            for (int index = 0; index < invariantPositions.length; index++) {
                double now = argument[invariantPositions[index]];
                if (Double.doubleToRawLongBits(now) != Double.doubleToRawLongBits(held[index])) {
                    held[index] = now;
                    same = false;
                }
            }
            primed = true;
            return same;
        }

        private double compute(int index, double[] argument) {
            double[] value = values;
            int[] in = inputs[index];
            return switch (operation[index]) {
                case CONSTANT -> constants[index];
                case ARGUMENT -> argument[arguments[index]];
                case NEGATE -> -value[in[0]];
                case ADD -> value[in[0]] + value[in[1]];
                case SUBTRACT -> value[in[0]] - value[in[1]];
                case MULTIPLY -> value[in[0]] * value[in[1]];
                case DIVIDE -> value[in[0]] / value[in[1]];
                case POWER -> RealCompiler.realPower(value[in[0]], value[in[1]]);
                case INTEGER_POWER -> integerPower(value[in[0]], (int) constants[index]);
                case CALL -> functions[index].apply(argumentsOf(in, value));
                case COMPARE -> Double.isNaN(value[in[0]]) || Double.isNaN(value[in[1]]) ? Double.NaN
                        : relations[index].holds(value[in[0]], value[in[1]]) ? 1 : 0;
                case AND -> Double.isNaN(value[in[0]]) || Double.isNaN(value[in[1]]) ? Double.NaN
                        : value[in[0]] != 0 && value[in[1]] != 0 ? 1 : 0;
                case OR -> Double.isNaN(value[in[0]]) || Double.isNaN(value[in[1]]) ? Double.NaN
                        : value[in[0]] != 0 || value[in[1]] != 0 ? 1 : 0;
                case NOT -> Double.isNaN(value[in[0]]) ? Double.NaN : value[in[0]] == 0 ? 1 : 0;
                case SELECT -> Double.isNaN(value[in[0]]) ? Double.NaN : value[in[0]] != 0 ? value[in[1]] : value[in[2]];
            };
        }

        private void backward() {
            double[] value = values;
            double[] adjoint = adjoints;
            Arrays.fill(adjoint, 0, result + 1, 0);
            adjoint[result] = 1;
            for (int index = result; index >= 0; index--) {
                double seed = adjoint[index];
                // Nothing invariant depends on an unknown, so nothing is owed to it: its adjoint is never read.
                if (seed == 0 || invariant[index]) {
                    continue;
                }
                int[] in = inputs[index];
                switch (operation[index]) {
                    case NEGATE -> adjoint[in[0]] -= seed;
                    case ADD -> {
                        adjoint[in[0]] += seed;
                        adjoint[in[1]] += seed;
                    }
                    case SUBTRACT -> {
                        adjoint[in[0]] += seed;
                        adjoint[in[1]] -= seed;
                    }
                    case MULTIPLY -> {
                        adjoint[in[0]] += seed * value[in[1]];
                        adjoint[in[1]] += seed * value[in[0]];
                    }
                    case DIVIDE -> {
                        double divisor = value[in[1]];
                        adjoint[in[0]] += seed / divisor;
                        adjoint[in[1]] -= seed * value[index] / divisor;
                    }
                    case POWER -> {
                        double base = value[in[0]];
                        double exponent = value[in[1]];
                        adjoint[in[0]] += seed * exponent * RealCompiler.realPower(base, exponent - 1);
                        if (base > 0) {
                            adjoint[in[1]] += seed * value[index] * Math.log(base);
                        }
                    }
                    case INTEGER_POWER -> {
                        int exponent = (int) constants[index];
                        if (exponent != 0) {
                            adjoint[in[0]] += seed * exponent * integerPower(value[in[0]], exponent - 1);
                        }
                    }
                    case CALL -> {
                        double[] at = argumentsOf(in, value);
                        RealFunction[] partials = derivatives[index];
                        for (int argument = 0; argument < in.length; argument++) {
                            adjoint[in[argument]] += seed * partials[argument].evaluate(at);
                        }
                    }
                    case SELECT -> adjoint[value[in[0]] != 0 ? in[1] : in[2]] += seed;
                    default -> {
                    }
                }
            }
        }

        private double[] argumentsOf(int[] in, double[] value) {
            if (in.length == 1) {
                one[0] = value[in[0]];
                return one;
            }
            if (in.length == 2) {
                two[0] = value[in[0]];
                two[1] = value[in[1]];
                return two;
            }
            double[] at = new double[in.length];
            for (int index = 0; index < in.length; index++) {
                at[index] = value[in[index]];
            }
            return at;
        }

        private static double integerPower(double base, int exponent) {
            double product = 1;
            double factor = exponent < 0 ? 1 / base : base;
            for (int remaining = Math.abs(exponent); remaining > 0; remaining--) {
                product *= factor;
            }
            return product;
        }
    }
}
