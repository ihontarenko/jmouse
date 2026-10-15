package org.jmouse.formula;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Compiles an expression over the complex numbers: f(z) = z^2 + c, (z - 1)/(z + 1), exp(i z),
 * sum(k, 0, inf, z^k / k!).
 *
 * <p>Conditions compare real parts — {@code abs(z) < 1} and {@code re(z) > 0} are what a condition on a complex
 * plane means — and a truth value is 1 or 0 on the real axis. A sum runs its index over the integers, an integral
 * its variable over the reals, both possibly with a complex body. Binders get frame slots as in
 * {@link RealCompiler}.
 */
public final class ComplexCompiler {

    @FunctionalInterface
    public interface ComplexFunction {
        Complex evaluate(Complex[] values);
    }

    private static final Complex NOT_A_NUMBER = new Complex(Double.NaN, Double.NaN);
    private static final int SETTLED_TERMS = 8;
    private static final double SLOW_CONVERGENCE = 1e-8;

    private ComplexCompiler() {
    }

    public static ComplexFunction compile(Formula expression, Symbols symbols, String source) {
        symbols.requireDeclares(expression, source);
        RealCompiler.Frame frame = new RealCompiler.Frame(symbols.size());
        ComplexFunction body = build(expression, Scope.of(symbols), frame, source);
        if (frame.size() == symbols.size()) {
            return body;
        }
        int size = frame.size();
        return values -> body.evaluate(Arrays.copyOf(values, Math.max(size, values.length)));
    }

    private static ComplexFunction build(Formula expression, Scope scope, RealCompiler.Frame frame, String source) {
        return switch (expression) {
            case Formula.Constant constant -> {
                Complex value = Complex.of(constant.value());
                yield values -> value;
            }
            case Formula.NamedConstant named -> {
                Complex value = Complex.of(named.constant().value());
                yield values -> value;
            }
            case Formula.ImaginaryUnit unit -> values -> Complex.I;
            case Formula.Variable variable -> {
                int slot = scope.slotOf(variable.name());
                yield values -> values[slot];
            }
            case Formula.Slot slot -> throw new FormulaException("A complex expression reads names, not a host's slots.", source, -1);
            case Formula.HostCall hostCall -> throw new FormulaException("'" + hostCall.name()
                    + "(…)' is its host's to bind before the expression is compiled.", source, -1);
            case Formula.Negation negation -> {
                ComplexFunction operand = build(negation.operand(), scope, frame, source);
                yield values -> operand.evaluate(values).negate();
            }
            case Formula.Binary binary -> {
                ComplexFunction left = build(binary.left(), scope, frame, source);
                ComplexFunction right = build(binary.right(), scope, frame, source);
                yield switch (binary.operator()) {
                    case ADD -> values -> left.evaluate(values).add(right.evaluate(values));
                    case SUBTRACT -> values -> left.evaluate(values).subtract(right.evaluate(values));
                    case MULTIPLY -> values -> left.evaluate(values).multiply(right.evaluate(values));
                    case DIVIDE -> values -> left.evaluate(values).divide(right.evaluate(values));
                    case POWER -> values -> left.evaluate(values).power(right.evaluate(values));
                };
            }
            case Formula.Call call -> {
                Function<Complex[], Complex> implementation = call.function().complex().orElseThrow(() ->
                        new FormulaException(call.function().symbol() + " has no complex form here.", source, -1));
                ComplexFunction[] arguments = call.arguments().stream()
                        .map(argument -> build(argument, scope, frame, source)).toArray(ComplexFunction[]::new);
                yield values -> {
                    Complex[] evaluated = new Complex[arguments.length];
                    for (int index = 0; index < arguments.length; index++) {
                        evaluated[index] = arguments[index].evaluate(values);
                    }
                    return implementation.apply(evaluated);
                };
            }
            case Formula.Comparison comparison -> {
                ComplexFunction left = build(comparison.left(), scope, frame, source);
                ComplexFunction right = build(comparison.right(), scope, frame, source);
                Formula.Relation relation = comparison.relation();
                yield values -> {
                    double first = left.evaluate(values).real();
                    double second = right.evaluate(values).real();
                    if (Double.isNaN(first) || Double.isNaN(second)) {
                        return NOT_A_NUMBER;
                    }
                    return relation.holds(first, second) ? Complex.ONE : Complex.ZERO;
                };
            }
            case Formula.Logical logical -> {
                ComplexFunction left = build(logical.left(), scope, frame, source);
                ComplexFunction right = build(logical.right(), scope, frame, source);
                boolean conjunction = logical.connective() == Formula.Connective.AND;
                yield values -> {
                    Truth first = Truth.of(left.evaluate(values));
                    Truth second = Truth.of(right.evaluate(values));
                    if (first == Truth.UNDEFINED || second == Truth.UNDEFINED) {
                        return NOT_A_NUMBER;
                    }
                    boolean holds = conjunction ? first == Truth.TRUE && second == Truth.TRUE
                            : first == Truth.TRUE || second == Truth.TRUE;
                    return holds ? Complex.ONE : Complex.ZERO;
                };
            }
            case Formula.Not not -> {
                ComplexFunction operand = build(not.operand(), scope, frame, source);
                yield values -> switch (Truth.of(operand.evaluate(values))) {
                    case TRUE -> Complex.ZERO;
                    case FALSE -> Complex.ONE;
                    case UNDEFINED -> NOT_A_NUMBER;
                };
            }
            case Formula.Equation equation -> {
                ComplexFunction left = build(equation.left(), scope, frame, source);
                ComplexFunction right = build(equation.right(), scope, frame, source);
                yield values -> left.evaluate(values).subtract(right.evaluate(values));
            }
            case Formula.Piecewise piecewise -> piecewise(piecewise, scope, frame, source);
            case Formula.Accumulation accumulation -> accumulation(accumulation, scope, frame, source);
            case Formula.Integral integral -> integral(integral, scope, frame, source);
            case Formula.Application application -> application(application, scope, frame, source);
        };
    }

    private enum Truth {
        TRUE, FALSE, UNDEFINED;

        static Truth of(Complex value) {
            if (Double.isNaN(value.real()) || Double.isNaN(value.imaginary())) {
                return UNDEFINED;
            }
            return value.real() != 0 || value.imaginary() != 0 ? TRUE : FALSE;
        }
    }

    private static ComplexFunction piecewise(Formula.Piecewise piecewise, Scope scope, RealCompiler.Frame frame,
                                             String source) {
        int count = piecewise.branches().size();
        ComplexFunction[] conditions = new ComplexFunction[count];
        ComplexFunction[] branches = new ComplexFunction[count];
        for (int index = 0; index < count; index++) {
            conditions[index] = build(piecewise.branches().get(index).condition(), scope, frame, source);
            branches[index] = build(piecewise.branches().get(index).value(), scope, frame, source);
        }
        ComplexFunction otherwise = piecewise.otherwise() == null ? values -> NOT_A_NUMBER
                : build(piecewise.otherwise(), scope, frame, source);
        return values -> {
            for (int index = 0; index < count; index++) {
                Truth condition = Truth.of(conditions[index].evaluate(values));
                if (condition == Truth.UNDEFINED) {
                    return NOT_A_NUMBER;
                }
                if (condition == Truth.TRUE) {
                    return branches[index].evaluate(values);
                }
            }
            return otherwise.evaluate(values);
        };
    }

    private static ComplexFunction accumulation(Formula.Accumulation accumulation, Scope scope,
                                                RealCompiler.Frame frame, String source) {
        ComplexFunction from = build(accumulation.from(), scope, frame, source);
        ComplexFunction to = build(accumulation.to(), scope, frame, source);
        int slot = frame.allocate();
        ComplexFunction body = build(accumulation.body(), scope.bind(accumulation.index(), slot), frame, source);
        boolean sum = accumulation.accumulator() == Formula.Accumulator.SUM;
        return values -> {
            RealCompiler.Range range = RealCompiler.Range.of(from.evaluate(values).real(), to.evaluate(values).real());
            if (range == null || (!range.infinite() && range.end() - range.start() >= RealCompiler.MAXIMUM_TERMS)) {
                return NOT_A_NUMBER;
            }
            Complex result = sum ? Complex.ZERO : Complex.ONE;
            if (!range.infinite()) {
                for (long index = range.start(); index <= range.end(); index++) {
                    values[slot] = Complex.of(index);
                    Complex term = body.evaluate(values);
                    result = sum ? result.add(term) : result.multiply(term);
                }
                return result;
            }
            int settled = 0;
            double change = Double.POSITIVE_INFINITY;
            for (long index = range.start(); index - range.start() < RealCompiler.MAXIMUM_TERMS; index++) {
                values[slot] = Complex.of(index);
                Complex term = body.evaluate(values);
                Complex next = sum ? result.add(term) : result.multiply(term);
                if (!next.isFinite()) {
                    return NOT_A_NUMBER;
                }
                change = next.subtract(result).modulus();
                settled = change <= 1e-16 * next.modulus() ? settled + 1 : 0;
                result = next;
                if (settled >= SETTLED_TERMS) {
                    return result;
                }
            }
            return change <= SLOW_CONVERGENCE * result.modulus() ? result : NOT_A_NUMBER;
        };
    }

    /** A real variable of integration, so the real and imaginary parts are integrated separately. */
    private static ComplexFunction integral(Formula.Integral integral, Scope scope, RealCompiler.Frame frame,
                                            String source) {
        ComplexFunction lower = build(integral.lower(), scope, frame, source);
        ComplexFunction upper = build(integral.upper(), scope, frame, source);
        int slot = frame.allocate();
        ComplexFunction body = build(integral.body(), scope.bind(integral.variable(), slot), frame, source);
        return values -> {
            double from = lower.evaluate(values).real();
            double to = upper.evaluate(values).real();
            double real = Quadrature.integrate(point -> {
                values[slot] = Complex.of(point);
                return body.evaluate(values).real();
            }, from, to);
            double imaginary = Quadrature.integrate(point -> {
                values[slot] = Complex.of(point);
                return body.evaluate(values).imaginary();
            }, from, to);
            return new Complex(real, imaginary);
        };
    }

    private static ComplexFunction application(Formula.Application application, Scope scope,
                                               RealCompiler.Frame frame, String source) {
        List<String> parameters = application.definition().parameters();
        int count = parameters.size();
        ComplexFunction[] arguments = new ComplexFunction[count];
        int[] slots = new int[count];
        Scope definitionScope = scope.topLevel();
        for (int index = 0; index < count; index++) {
            arguments[index] = build(application.arguments().get(index), scope, frame, source);
            slots[index] = frame.allocate();
            definitionScope = definitionScope.bind(parameters.get(index), slots[index]);
        }
        ComplexFunction body = build(application.definition().body(), definitionScope, frame, source);
        if (count == 0) {
            return body;
        }
        return values -> {
            Complex[] evaluated = new Complex[count];
            for (int index = 0; index < count; index++) {
                evaluated[index] = arguments[index].evaluate(values);
            }
            for (int index = 0; index < count; index++) {
                values[slots[index]] = evaluated[index];
            }
            return body.evaluate(values);
        };
    }
}
