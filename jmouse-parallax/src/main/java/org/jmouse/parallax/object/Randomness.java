package org.jmouse.parallax.object;

import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.Substitution;
import org.jmouse.formula.el.FormulaLanguage;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.Function;

/**
 * The random functions the stochastic engines supply: {@code random_sign()}, {@code random_integer(a, b)},
 * {@code random_uniform()}, {@code random_normal()}, {@code random_poisson(mean)} and, in a derivative,
 * {@code white_noise()}. They are host calls — declared by the engine, found on the expression's tree and bound to
 * a draw from the computation's seeded source, never matched by text.
 *
 * <h2>One written call, one draw per evaluation</h2>
 *
 * <p>Every call written in an expression is its own draw each time the expression is evaluated, so
 * {@code x + random_sign()} and {@code y + random_sign()} move independently. Two updates that must see the
 * <em>same</em> draw read a step's value: {@code local axis = random_integer(0, 1)}, evaluated once per step.</p>
 *
 * <h2>White noise is not a normal draw</h2>
 *
 * <p>In {@code d(x)/dt = sigma * white_noise()} the noise is ξ(t), whose integral over a step of length h is normal
 * with variance h. The engine integrates by Euler–Maruyama, so it draws {@code N(0, 1) / sqrt(h)} for ξ and the step
 * adds {@code sigma * sqrt(h) * N(0, 1)}. A {@code random_normal()} there would be one N(0, 1) per step, a process that
 * vanishes as the step shrinks — which is why the two are different words.</p>
 *
 * <p>A random function's arguments are numbers or parameters, evaluated once when the expression is bound.</p>
 */
public final class Randomness {

    /** What one call draws. */
    public enum Draw {

        /** {@code random_sign()}: -1 or 1, each with probability one half. */
        SIGN("random_sign", 0, 0),
        /** {@code random_integer(a, b)}: an integer from a to b, each equally likely. */
        INTEGER("random_integer", 2, 2),
        /** {@code random_uniform()} on [0, 1), or {@code random_uniform(a, b)} on [a, b). */
        UNIFORM("random_uniform", 0, 2),
        /** {@code random_normal()}, standard; {@code random_normal(mean, deviation)}. */
        NORMAL("random_normal", 0, 2),
        /** {@code random_poisson(mean)}: a count of arrivals with that mean. */
        POISSON("random_poisson", 1, 1),
        /** {@code white_noise()}: ξ(t) of a stochastic differential equation, drawn for a step of the integrator. */
        WHITE_NOISE("white_noise", 0, 0);

        private final String name;
        private final int    fewestArguments;
        private final int    mostArguments;

        Draw(String name, int fewestArguments, int mostArguments) {
            this.name = name;
            this.fewestArguments = fewestArguments;
            this.mostArguments = mostArguments;
        }

        public String functionName() {
            return name;
        }

        static Optional<Draw> named(String name) {
            return Arrays.stream(values()).filter(draw -> draw.name.equals(name)).findFirst();
        }

        /**
         * One draw.
         *
         * @param arguments the call's arguments, already numbers
         * @param step      the integrator's step, which white noise is drawn for; ignored by every other draw
         */
        double draw(SplittableRandom random, double[] arguments, double step) {
            return switch (this) {
                case SIGN -> random.nextBoolean() ? 1 : -1;
                case INTEGER -> arguments[0] + random.nextInt((int) (arguments[1] - arguments[0]) + 1);
                case UNIFORM -> arguments.length == 0 ? random.nextDouble()
                        : arguments[0] + (arguments[1] - arguments[0]) * random.nextDouble();
                case NORMAL -> arguments.length == 0 ? gaussian(random) : arguments[0] + arguments[1] * gaussian(random);
                case POISSON -> poisson(random, arguments[0]);
                case WHITE_NOISE -> gaussian(random) / Math.sqrt(step);
            };
        }

        /** One draw for a caller outside an expression — a program — its arguments checked as a written call's are. */
        public double draw(SplittableRandom random, double[] arguments) {
            if (!takes(arguments.length)) {
                throw new IllegalArgumentException(arityRefusal(arguments.length));
            }
            check(arguments);
            return draw(random, arguments, 1);
        }

        /** Whether a call with this many arguments is one this draw can make. */
        boolean takes(int count) {
            return count == fewestArguments || count == mostArguments;
        }

        /** Why a call with this many arguments is refused. */
        String arityRefusal(int count) {
            return name + " takes " + (fewestArguments == mostArguments
                    ? String.valueOf(fewestArguments) : fewestArguments + " or " + mostArguments)
                    + " argument(s), not " + count + ".";
        }

        /** Refuses arguments this draw cannot use, at binding, with words. */
        void check(double[] arguments) {
            switch (this) {
                case INTEGER -> {
                    if (arguments[0] != Math.rint(arguments[0]) || arguments[1] != Math.rint(arguments[1])
                            || arguments[1] < arguments[0] || arguments[1] - arguments[0] >= Integer.MAX_VALUE) {
                        throw new FormulaException("random_integer(a, b) takes two integers, a not above b.", null, -1);
                    }
                }
                case UNIFORM -> {
                    if (arguments.length == 2 && !(arguments[1] > arguments[0])) {
                        throw new FormulaException("random_uniform(a, b) takes a below b.", null, -1);
                    }
                }
                case NORMAL -> {
                    if (arguments.length == 2 && !(arguments[1] >= 0)) {
                        throw new FormulaException("random_normal(mean, deviation) takes a deviation not below 0.", null, -1);
                    }
                }
                case POISSON -> {
                    if (!(arguments[0] >= 0) || arguments[0] > 500) {
                        throw new FormulaException("random_poisson(mean) takes a mean from 0 to 500.", null, -1);
                    }
                }
                default -> {
                }
            }
        }
    }

    private static final FormulaLanguage LANGUAGE = FormulaLanguage.standard()
            .withHostCalls(Arrays.stream(Draw.values()).map(Draw::functionName).toArray(String[]::new));

    private Randomness() {
    }

    /** The language a stochastic expression is read in: jMF and the random functions. */
    static Formula parse(String source) {
        return LANGUAGE.parse(source);
    }

    /** Whether any expression of the definition calls a random function: whether it is computed as sample paths. */
    public static boolean isStochastic(ObjectDefinition definition) {
        return switch (definition) {
            case ObjectDefinition.DiscreteMap map -> map.updates().stream().anyMatch(Randomness::drawsOn)
                    || map.stepValues().stream().anyMatch(value -> drawsOn(value.expression()));
            case ObjectDefinition.DifferentialSystem system -> system.equations().stream().anyMatch(Randomness::drawsOn);
            default -> false;
        };
    }

    /** Whether an expression calls a random function; one that does not read is not random — compiling refuses it. */
    public static boolean drawsOn(String source) {
        Formula expression;
        try {
            expression = LANGUAGE.parse(source);
        } catch (FormulaException unreadable) {
            return false;
        }
        boolean[] found = {false};
        Substitution.replaceHostCalls(expression, call -> {
            found[0] = true;
            return call;
        });
        return found[0];
    }

    /**
     * Every random call of an expression replaced by a slot at the front of the argument array, in the order the
     * calls are met; what each slot draws is recorded in {@code draws}.
     *
     * @param argument   evaluates a call's argument, which may name parameters only
     * @param noise      whether {@code white_noise()} may be called: in a derivative, and nowhere else
     */
    static Formula bind(Formula expression, List<BoundDraw> draws, Function<Formula, Double> argument, boolean noise) {
        return Substitution.replaceHostCalls(expression, call -> {
            Draw draw = Draw.named(call.name()).orElseThrow();
            int count = call.arguments().size();
            if (!draw.takes(count)) {
                throw new FormulaException(draw.arityRefusal(count), null, -1);
            }
            if (draw == Draw.WHITE_NOISE && !noise) {
                throw new FormulaException("white_noise() is the noise of a derivative, d(x)/dt = … white_noise(); "
                        + "a step draws random_normal().", null, -1);
            }
            double[] arguments = new double[count];
            for (int index = 0; index < count; index++) {
                arguments[index] = argument.apply(call.arguments().get(index));
            }
            draw.check(arguments);
            draws.add(new BoundDraw(draw, arguments));
            return new Formula.Slot(draws.size() - 1, call.name());
        });
    }

    /** One random call with its arguments evaluated: what fills its slot before every evaluation. */
    record BoundDraw(Draw draw, double[] arguments) {
    }

    /** A standard normal number by Box and Muller's method. */
    public static double gaussian(SplittableRandom random) {
        double first = random.nextDouble();
        double second = random.nextDouble();
        return Math.sqrt(-2 * Math.log(Math.max(first, 1e-300))) * Math.cos(2 * Math.PI * second);
    }

    /** Knuth's method: multiply uniform numbers until the product falls below e^-mean. */
    private static double poisson(SplittableRandom random, double mean) {
        double limit = Math.exp(-mean);
        int count = 0;
        double product = random.nextDouble();
        while (product > limit) {
            count++;
            product *= random.nextDouble();
        }
        return count;
    }
}
