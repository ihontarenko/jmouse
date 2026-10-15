package org.jmouse.formula;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates a formula exactly, over fractions of integers of any size: {@code +}, {@code -}, {@code *} and {@code /}
 * never round, a power takes an integer exponent (a negative one is a reciprocal), and {@code floor}, {@code ceil},
 * {@code round}, {@code mod}, {@code abs}, {@code sign}, {@code min} and {@code max} keep the result exact. A comparison
 * is 1 or 0, so {@code 1/2 < floor(…)} is decided without a rounding error however large its numbers are — which is
 * what Tupper's inequality needs, its {@code k} being 543 digits long.
 *
 * <p>A function with no exact form ({@code sin}, {@code sqrt}, {@code π}) is refused rather than approximated: an exact
 * answer that is secretly a double is the one result nobody can check.</p>
 *
 * <p>Work is bounded: at most {@value #MAXIMUM_OPERATIONS} operations per evaluation and numbers of at most
 * {@value #MAXIMUM_BITS} bits, so an expression a reader writes cannot exhaust the server.</p>
 */
public final class ExactRationalEvaluator {

    private static final int MAXIMUM_BITS       = 16_384;
    private static final int MAXIMUM_OPERATIONS = 10_000;

    private int operations;

    private ExactRationalEvaluator() {
    }

    /**
     * @param values every free name of the expression and its exact value
     * @throws IllegalArgumentException when the expression has no exact form or exceeds the budget, saying which
     */
    public static Rational evaluate(Formula expression, Map<String, Rational> values) {
        return new ExactRationalEvaluator().read(expression, values);
    }

    /** Whether a condition holds: a comparison's 1, any value that is not zero, an equation whose sides are equal. */
    public static boolean holds(Formula condition, Map<String, Rational> values) {
        return evaluate(condition, values).signum() != 0;
    }

    private Rational read(Formula expression, Map<String, Rational> values) {
        if (++operations > MAXIMUM_OPERATIONS) {
            throw new IllegalArgumentException("The exact formula exceeded its operation budget.");
        }
        Rational result = switch (expression) {
            case Formula.Constant constant -> {
                try {
                    yield Rational.ofDecimal(constant.value());
                } catch (ArithmeticException undefined) {
                    throw new IllegalArgumentException(undefined.getMessage());
                }
            }
            case Formula.Variable variable -> {
                Rational value = values.get(variable.name());
                if (value == null) {
                    throw new IllegalArgumentException("No exact value for '" + variable.name() + "'.");
                }
                yield value;
            }
            case Formula.Negation negation -> read(negation.operand(), values).negate();
            case Formula.Binary binary -> binary(binary, values);
            case Formula.Call call -> call(call, values);
            case Formula.Comparison comparison -> truth(comparison.relation().holds(
                    read(comparison.left(), values).compareTo(read(comparison.right(), values)), 0));
            case Formula.Equation equation -> truth(read(equation.left(), values).equals(read(equation.right(), values)));
            case Formula.Not not -> truth(read(not.operand(), values).signum() == 0);
            case Formula.Logical logical -> {
                boolean left = read(logical.left(), values).signum() != 0;
                boolean both = logical.connective() == Formula.Connective.AND
                        ? left && read(logical.right(), values).signum() != 0
                        : left || read(logical.right(), values).signum() != 0;
                yield truth(both);
            }
            case Formula.Piecewise piecewise -> piecewise(piecewise, values);
            case Formula.Application application -> application(application, values);
            case Formula.NamedConstant named -> throw new IllegalArgumentException("'" + named.constant().symbol()
                    + "' is not a fraction, so an exact formula cannot use it.");
            case Formula.ImaginaryUnit unit -> throw new IllegalArgumentException("An exact formula is real; it has no i.");
            case Formula.Slot slot -> throw new IllegalArgumentException("An exact formula reads names, not a host's slots.");
            case Formula.HostCall hostCall -> throw new IllegalArgumentException("'" + hostCall.name()
                    + "(…)' is its host's to bind; an exact formula has no host.");
            case Formula.Accumulation accumulation -> throw new IllegalArgumentException(
                    "A " + accumulation.accumulator().symbol() + " has no exact form here; write its terms.");
            case Formula.Integral integral -> throw new IllegalArgumentException("An integral has no exact form.");
        };
        if (result.bitLength() > MAXIMUM_BITS) {
            throw new IllegalArgumentException("The exact formula exceeded its number size budget.");
        }
        return result;
    }

    private Rational binary(Formula.Binary binary, Map<String, Rational> values) {
        Rational left = read(binary.left(), values);
        Rational right = read(binary.right(), values);
        return switch (binary.operator()) {
            case ADD -> left.add(right);
            case SUBTRACT -> left.subtract(right);
            case MULTIPLY -> left.multiply(right);
            case DIVIDE -> divide(left, right);
            case POWER -> power(left, right);
        };
    }

    private Rational call(Formula.Call call, Map<String, Rational> values) {
        List<Rational> arguments = call.arguments().stream().map(argument -> read(argument, values)).toList();
        return switch (call.function()) {
            case FLOOR -> Rational.of(arguments.getFirst().floor());
            case CEIL -> Rational.of(arguments.getFirst().ceiling());
            case ROUND -> Rational.of(arguments.getFirst().rounded());
            case ABS -> arguments.getFirst().abs();
            case SIGN -> Rational.of(arguments.getFirst().signum());
            case MIN -> arguments.get(0).compareTo(arguments.get(1)) <= 0 ? arguments.get(0) : arguments.get(1);
            case MAX -> arguments.get(0).compareTo(arguments.get(1)) >= 0 ? arguments.get(0) : arguments.get(1);
            case MOD -> {
                Rational dividend = arguments.get(0);
                Rational divisor = arguments.get(1);
                yield dividend.subtract(divisor.multiply(Rational.of(divide(dividend, divisor).floor())));
            }
            case POW -> power(arguments.get(0), arguments.get(1));
            default -> throw new IllegalArgumentException("'" + call.function().symbol() + "' has no exact form.");
        };
    }

    private Rational piecewise(Formula.Piecewise piecewise, Map<String, Rational> values) {
        for (Formula.Piecewise.Branch branch : piecewise.branches()) {
            if (read(branch.condition(), values).signum() != 0) {
                return read(branch.value(), values);
            }
        }
        if (piecewise.otherwise() == null) {
            throw new IllegalArgumentException("No branch of the exact formula applies.");
        }
        return read(piecewise.otherwise(), values);
    }

    /** A call of the author's own function: its body read with its parameters bound to the arguments' values. */
    private Rational application(Formula.Application application, Map<String, Rational> values) {
        Formula.Definition definition = application.definition();
        Map<String, Rational> arguments = new HashMap<>(values);
        for (int index = 0; index < definition.parameters().size(); index++) {
            arguments.put(definition.parameters().get(index), read(application.arguments().get(index), values));
        }
        return read(definition.body(), arguments);
    }

    private static Rational divide(Rational dividend, Rational divisor) {
        if (divisor.signum() == 0) {
            throw new IllegalArgumentException("Division by zero in an exact formula.");
        }
        return dividend.divide(divisor);
    }

    private static Rational power(Rational base, Rational exponent) {
        if (!exponent.isInteger()) {
            throw new IllegalArgumentException("An exact power has an integer exponent, not " + exponent + ".");
        }
        BigInteger written = exponent.numerator();
        if (written.abs().compareTo(BigInteger.valueOf(MAXIMUM_BITS)) > 0
                || (long) base.bitLength() * written.abs().longValue() > MAXIMUM_BITS + 1L) {
            throw new IllegalArgumentException("The exact formula's power exceeds its number size budget.");
        }
        if (written.signum() < 0 && base.signum() == 0) {
            throw new IllegalArgumentException("Zero has no negative power.");
        }
        return base.power(written.intValue());
    }

    private static Rational truth(boolean holds) {
        return holds ? Rational.ONE : Rational.ZERO;
    }
}
