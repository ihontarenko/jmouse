package org.jmouse.formula;

import java.util.Arrays;
import java.util.List;

/**
 * Compiles an expression into a closure tree once, so sampling a 256×256 grid evaluates closures rather than
 * walking the syntax tree a million times. Constant subtrees are folded at compile time.
 *
 * <p>The compiled function reads the caller's symbols from one array laid out by {@link Symbols}. A name that a
 * sum, an integral or a defined function binds gets a slot of its own after them — every binder its own, so an
 * inner one never overwrites an outer one — and the function then evaluates in a private frame copied from the
 * caller's array. A formula with no binders evaluates in the caller's array directly and allocates nothing.
 */
public final class RealCompiler {

    /** A finite sum or product runs at most this many terms; more is refused as NaN rather than left to hang. */
    static final long MAXIMUM_TERMS = 1_000_000;
    /** A series stops once this many consecutive terms no longer move it. */
    private static final int SETTLED_TERMS = 8;
    /** What the last term of a series cut off by the term budget may still change, relative to the sum. */
    private static final double SLOW_CONVERGENCE = 1e-8;

    private RealCompiler() {
    }

    public static RealFunction compile(Formula expression, Symbols symbols, String source) {
        if (expression.mentionsImaginaryUnit()) {
            throw new FormulaException("This expression is real-valued; 'i' belongs to complex expressions.", source, -1);
        }
        symbols.requireDeclares(expression, source);
        Frame frame = new Frame(symbols.size());
        RealFunction body = build(expression, Scope.of(symbols), frame);
        if (frame.size() == symbols.size()) {
            return body;
        }
        int size = frame.size();
        return values -> body.evaluate(Arrays.copyOf(values, Math.max(size, values.length)));
    }

    /** The value of a constant expression. */
    public static double fold(Formula expression) {
        return compile(expression, Symbols.of(), null).evaluate(new double[0]);
    }

    static boolean isConstant(Formula expression) {
        return expression.variables().isEmpty() && expression.slots().isEmpty() && !expression.mentionsImaginaryUnit();
    }

    private static RealFunction build(Formula expression, Scope scope, Frame frame) {
        if (!(expression instanceof Formula.Constant) && isConstant(expression)) {
            Frame folding = new Frame(0);
            RealFunction constant = build(expression, Scope.empty(), folding, true);
            double value = constant.evaluate(new double[folding.size]);
            return values -> value;
        }
        return build(expression, scope, frame, false);
    }

    private static RealFunction build(Formula expression, Scope scope, Frame frame, boolean folding) {
        return switch (expression) {
            case Formula.Constant constant -> {
                double value = constant.value();
                yield values -> value;
            }
            case Formula.NamedConstant named -> {
                double value = named.constant().value();
                yield values -> value;
            }
            case Formula.ImaginaryUnit unit -> throw new IllegalStateException("refused before building");
            case Formula.Variable variable -> {
                int slot = scope.slotOf(variable.name());
                yield values -> values[slot];
            }
            case Formula.Slot slot -> {
                int index = slot.index();
                yield values -> values[index];
            }
            case Formula.HostCall hostCall -> throw new IllegalStateException("refused before building");
            case Formula.Negation negation -> {
                RealFunction operand = child(negation.operand(), scope, frame, folding);
                yield values -> -operand.evaluate(values);
            }
            case Formula.Binary binary -> binary(binary, scope, frame, folding);
            case Formula.Call call -> call(call, scope, frame, folding);
            case Formula.Comparison comparison -> {
                RealFunction left = child(comparison.left(), scope, frame, folding);
                RealFunction right = child(comparison.right(), scope, frame, folding);
                Formula.Relation relation = comparison.relation();
                yield values -> {
                    double first = left.evaluate(values);
                    double second = right.evaluate(values);
                    if (Double.isNaN(first) || Double.isNaN(second)) {
                        return Double.NaN;
                    }
                    return relation.holds(first, second) ? 1 : 0;
                };
            }
            case Formula.Logical logical -> logical(logical, scope, frame, folding);
            case Formula.Not not -> {
                RealFunction operand = child(not.operand(), scope, frame, folding);
                yield values -> {
                    double value = operand.evaluate(values);
                    if (Double.isNaN(value)) {
                        return Double.NaN;
                    }
                    return value == 0 ? 1 : 0;
                };
            }
            case Formula.Equation equation -> {
                RealFunction left = child(equation.left(), scope, frame, folding);
                RealFunction right = child(equation.right(), scope, frame, folding);
                yield values -> left.evaluate(values) - right.evaluate(values);
            }
            case Formula.Piecewise piecewise -> piecewise(piecewise, scope, frame, folding);
            case Formula.Accumulation accumulation -> accumulation(accumulation, scope, frame, folding);
            case Formula.Integral integral -> integral(integral, scope, frame, folding);
            case Formula.Application application -> application(application, scope, frame, folding);
        };
    }

    private static RealFunction child(Formula expression, Scope scope, Frame frame, boolean folding) {
        return folding ? build(expression, scope, frame, true) : build(expression, scope, frame);
    }

    private static RealFunction binary(Formula.Binary binary, Scope scope, Frame frame, boolean folding) {
        RealFunction left = child(binary.left(), scope, frame, folding);
        RealFunction right = child(binary.right(), scope, frame, folding);
        return switch (binary.operator()) {
            case ADD -> values -> left.evaluate(values) + right.evaluate(values);
            case SUBTRACT -> values -> left.evaluate(values) - right.evaluate(values);
            case MULTIPLY -> values -> left.evaluate(values) * right.evaluate(values);
            case DIVIDE -> values -> left.evaluate(values) / right.evaluate(values);
            case POWER -> power(binary, left, right);
        };
    }

    /** Small integer exponents multiply instead of calling pow, and keep a negative base defined: (-2)^3 = -8. */
    private static RealFunction power(Formula.Binary binary, RealFunction left, RealFunction right) {
        if (binary.right() instanceof Formula.Constant constant && constant.value() == Math.rint(constant.value())
                && Math.abs(constant.value()) <= 8) {
            int exponent = (int) constant.value();
            return switch (exponent) {
                case 0 -> values -> 1;
                case 1 -> left;
                case 2 -> values -> {
                    double base = left.evaluate(values);
                    return base * base;
                };
                case 3 -> values -> {
                    double base = left.evaluate(values);
                    return base * base * base;
                };
                default -> values -> integerPower(left.evaluate(values), exponent);
            };
        }
        return values -> realPower(left.evaluate(values), right.evaluate(values));
    }

    private static double integerPower(double base, int exponent) {
        double result = 1;
        double factor = exponent < 0 ? 1 / base : base;
        for (int remaining = Math.abs(exponent); remaining > 0; remaining--) {
            result *= factor;
        }
        return result;
    }

    /** A negative base under an odd-denominator rational exponent keeps its real root: (-8)^(1/3) = -2. */
    static double realPower(double base, double exponent) {
        if (base < 0 && exponent != Math.rint(exponent)) {
            double reciprocal = 1 / exponent;
            if (Math.abs(reciprocal - Math.rint(reciprocal)) < 1e-12 && ((long) Math.rint(reciprocal)) % 2 != 0) {
                return -Math.pow(-base, exponent);
            }
        }
        return Math.pow(base, exponent);
    }

    private static RealFunction call(Formula.Call call, Scope scope, Frame frame, boolean folding) {
        MathematicalFunction.RealImplementation implementation = call.function().real();
        List<Formula> arguments = call.arguments();
        if (arguments.size() == 1) {
            RealFunction argument = child(arguments.getFirst(), scope, frame, folding);
            return values -> implementation.apply(new double[]{argument.evaluate(values)});
        }
        RealFunction first = child(arguments.get(0), scope, frame, folding);
        RealFunction second = child(arguments.get(1), scope, frame, folding);
        return values -> implementation.apply(new double[]{first.evaluate(values), second.evaluate(values)});
    }

    /** Both sides are evaluated, as a condition is a value; `and` holds when both are non-zero. */
    private static RealFunction logical(Formula.Logical logical, Scope scope, Frame frame, boolean folding) {
        RealFunction left = child(logical.left(), scope, frame, folding);
        RealFunction right = child(logical.right(), scope, frame, folding);
        boolean conjunction = logical.connective() == Formula.Connective.AND;
        return values -> {
            double first = left.evaluate(values);
            double second = right.evaluate(values);
            if (Double.isNaN(first) || Double.isNaN(second)) {
                return Double.NaN;
            }
            boolean holds = conjunction ? first != 0 && second != 0 : first != 0 || second != 0;
            return holds ? 1 : 0;
        };
    }

    private static RealFunction piecewise(Formula.Piecewise piecewise, Scope scope, Frame frame, boolean folding) {
        int count = piecewise.branches().size();
        RealFunction[] conditions = new RealFunction[count];
        RealFunction[] branches = new RealFunction[count];
        for (int index = 0; index < count; index++) {
            conditions[index] = child(piecewise.branches().get(index).condition(), scope, frame, folding);
            branches[index] = child(piecewise.branches().get(index).value(), scope, frame, folding);
        }
        RealFunction otherwise = piecewise.otherwise() == null ? values -> Double.NaN
                : child(piecewise.otherwise(), scope, frame, folding);
        return values -> {
            for (int index = 0; index < count; index++) {
                double condition = conditions[index].evaluate(values);
                if (Double.isNaN(condition)) {
                    return Double.NaN;
                }
                if (condition != 0) {
                    return branches[index].evaluate(values);
                }
            }
            return otherwise.evaluate(values);
        };
    }

    private static RealFunction accumulation(Formula.Accumulation accumulation, Scope scope, Frame frame,
                                             boolean folding) {
        RealFunction from = child(accumulation.from(), scope, frame, folding);
        RealFunction to = child(accumulation.to(), scope, frame, folding);
        int slot = frame.allocate();
        RealFunction body = child(accumulation.body(), scope.bind(accumulation.index(), slot), frame, folding);
        boolean sum = accumulation.accumulator() == Formula.Accumulator.SUM;
        return values -> {
            Range range = Range.of(from.evaluate(values), to.evaluate(values));
            if (range == null || (!range.infinite() && range.end() - range.start() >= MAXIMUM_TERMS)) {
                return Double.NaN;
            }
            double result = sum ? 0 : 1;
            if (!range.infinite()) {
                for (long index = range.start(); index <= range.end(); index++) {
                    values[slot] = index;
                    double term = body.evaluate(values);
                    result = sum ? result + term : result * term;
                }
                return result;
            }
            int settled = 0;
            double change = Double.POSITIVE_INFINITY;
            double quarter = Double.NaN;
            double half = Double.NaN;
            for (long index = range.start(); index - range.start() < MAXIMUM_TERMS; index++) {
                values[slot] = index;
                double term = body.evaluate(values);
                double next = sum ? result + term : result * term;
                if (Double.isNaN(next)) {
                    return Double.NaN;
                }
                change = Math.abs(next - result);
                settled = change <= 1e-16 * Math.abs(next) ? settled + 1 : 0;
                result = next;
                if (settled >= SETTLED_TERMS) {
                    return result;
                }
                long terms = index - range.start() + 1;
                if (terms == MAXIMUM_TERMS / 4) {
                    quarter = result;
                } else if (terms == MAXIMUM_TERMS / 2) {
                    half = result;
                }
            }
            // Out of terms: a slowly converging series (1/k²) is accepted once its terms barely move it, a
            // diverging one (1/k) is not — and an accepted one is carried to its limit, not cut off.
            if (!(change <= SLOW_CONVERGENCE * Math.abs(result))) {
                return Double.NaN;
            }
            return withTail(quarter, half, result);
        };
    }

    /**
     * The limit of a slowly converging series or product from three of its partial results, at N/4, N/2 and N terms.
     * Its tail shrinks like a power of N, so each doubling of N leaves a fixed share {@code r} of the previous
     * difference; the differences still to come add up as a geometric series (Aitken's Δ²): {@code ∑1/k²} cut at a
     * million terms is short by 10⁻⁶, and gets it back. A result whose differences do not shrink steadily — r outside
     * (0, 1) — is left as it is.
     */
    static double withTail(double quarter, double half, double full) {
        double earlier = half - quarter;
        double later = full - half;
        if (!Double.isFinite(earlier) || !Double.isFinite(later) || earlier == 0) {
            return full;
        }
        double share = later / earlier;
        if (!(share > 0 && share < 0.95)) {
            return full;
        }
        return full + later * share / (1 - share);
    }

    private static RealFunction integral(Formula.Integral integral, Scope scope, Frame frame, boolean folding) {
        RealFunction lower = child(integral.lower(), scope, frame, folding);
        RealFunction upper = child(integral.upper(), scope, frame, folding);
        int slot = frame.allocate();
        RealFunction body = child(integral.body(), scope.bind(integral.variable(), slot), frame, folding);
        return values -> Quadrature.integrate(point -> {
            values[slot] = point;
            return body.evaluate(values);
        }, lower.evaluate(values), upper.evaluate(values));
    }

    /**
     * The body is compiled once per call site, in the scope it was defined in — the top level — with each
     * parameter on a slot of its own; the arguments are evaluated first, then written there.
     */
    private static RealFunction application(Formula.Application application, Scope scope, Frame frame,
                                            boolean folding) {
        List<String> parameters = application.definition().parameters();
        int count = parameters.size();
        RealFunction[] arguments = new RealFunction[count];
        int[] slots = new int[count];
        Scope definitionScope = scope.topLevel();
        for (int index = 0; index < count; index++) {
            arguments[index] = child(application.arguments().get(index), scope, frame, folding);
            slots[index] = frame.allocate();
            definitionScope = definitionScope.bind(parameters.get(index), slots[index]);
        }
        RealFunction body = child(application.definition().body(), definitionScope, frame, folding);
        if (count == 0) {
            return body;
        }
        return values -> {
            double[] evaluated = new double[count];
            for (int index = 0; index < count; index++) {
                evaluated[index] = arguments[index].evaluate(values);
            }
            for (int index = 0; index < count; index++) {
                values[slots[index]] = evaluated[index];
            }
            return body.evaluate(values);
        };
    }

    /** The integers a sum runs over: from ⌈from⌉ to ⌊to⌋, or on without end for an infinite upper bound. */
    record Range(long start, long end, boolean infinite) {

        static Range of(double from, double to) {
            if (Double.isNaN(from) || Double.isNaN(to) || Double.isInfinite(from) || to == Double.NEGATIVE_INFINITY) {
                return null;
            }
            long start = (long) Math.ceil(from - 1e-9);
            if (to == Double.POSITIVE_INFINITY) {
                return new Range(start, Long.MAX_VALUE, true);
            }
            return new Range(start, (long) Math.floor(to + 1e-9), false);
        }
    }

    /** How many slots the evaluation frame needs; every binder takes the next one. */
    static final class Frame {

        private int size;

        Frame(int size) {
            this.size = size;
        }

        int allocate() {
            return size++;
        }

        int size() {
            return size;
        }
    }
}
