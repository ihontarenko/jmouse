package org.jmouse.formula;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * The functions an expression may call. Each carries its real implementation and, where the function has one,
 * its complex one; a function without a complex form is refused by the complex compiler rather than guessed.
 */
public enum MathematicalFunction {

    /** The sine of an angle in radians. */
    SIN("sin", 1, arguments -> Math.sin(arguments[0]), arguments -> arguments[0].sin()),
    /** The cosine of an angle in radians. */
    COS("cos", 1, arguments -> Math.cos(arguments[0]), arguments -> arguments[0].cos()),
    /** The tangent of an angle in radians. */
    TAN("tan", 1, arguments -> Math.tan(arguments[0]), arguments -> arguments[0].tan()),
    /** The cotangent: one over the tangent. */
    COT("cot", 1, arguments -> 1 / Math.tan(arguments[0]), arguments -> Complex.ONE.divide(arguments[0].tan())),
    /** The secant: one over the cosine. */
    SEC("sec", 1, arguments -> 1 / Math.cos(arguments[0]), arguments -> Complex.ONE.divide(arguments[0].cos())),
    /** The cosecant: one over the sine. */
    CSC("csc", 1, arguments -> 1 / Math.sin(arguments[0]), arguments -> Complex.ONE.divide(arguments[0].sin())),
    /** The angle whose sine is given, in radians, from −π/2 to π/2. */
    ASIN("asin", 1, arguments -> Math.asin(arguments[0]), null),
    /** The angle whose cosine is given, in radians, from 0 to π. */
    ACOS("acos", 1, arguments -> Math.acos(arguments[0]), null),
    /** The angle whose tangent is given, in radians, from −π/2 to π/2. */
    ATAN("atan", 1, arguments -> Math.atan(arguments[0]), null),
    /** The angle of the point (x, y) from the x axis, in radians, from −π to π: atan2(y, x). */
    ATAN2("atan2", 2, arguments -> Math.atan2(arguments[0], arguments[1]), null),
    /** The hyperbolic sine. */
    SINH("sinh", 1, arguments -> Math.sinh(arguments[0]), arguments -> arguments[0].sinh()),
    /** The hyperbolic cosine. */
    COSH("cosh", 1, arguments -> Math.cosh(arguments[0]), arguments -> arguments[0].cosh()),
    /** The hyperbolic tangent. */
    TANH("tanh", 1, arguments -> hyperbolicTangent(arguments[0]), arguments -> arguments[0].tanh()),
    /** The inverse hyperbolic sine. */
    ASINH("asinh", 1, arguments -> Math.log(arguments[0] + Math.sqrt(arguments[0] * arguments[0] + 1)), null),
    /** The inverse hyperbolic cosine, for values of 1 and more. */
    ACOSH("acosh", 1, arguments -> Math.log(arguments[0] + Math.sqrt(arguments[0] * arguments[0] - 1)), null),
    /** The inverse hyperbolic tangent, for values between −1 and 1. */
    ATANH("atanh", 1, arguments -> 0.5 * Math.log((1 + arguments[0]) / (1 - arguments[0])), null),
    /** e raised to the power given. */
    EXP("exp", 1, arguments -> Math.exp(arguments[0]), arguments -> arguments[0].exp()),
    /** The natural logarithm: to the base e. */
    LN("ln", 1, arguments -> Math.log(arguments[0]), arguments -> arguments[0].log()),
    /** The logarithm to the base 10. */
    LOG10("log10", 1, arguments -> Math.log10(arguments[0]), null),
    /** The logarithm to the base 2. */
    LOG2("log2", 1, arguments -> Math.log(arguments[0]) / Math.log(2), null),
    /** The square root. */
    SQRT("sqrt", 1, arguments -> Math.sqrt(arguments[0]), arguments -> arguments[0].sqrt()),
    /** The cube root, of a negative value too. */
    CBRT("cbrt", 1, arguments -> Math.cbrt(arguments[0]), null),
    /** The absolute value; of a complex number, its modulus. */
    ABS("abs", 1, arguments -> Math.abs(arguments[0]), arguments -> Complex.of(arguments[0].modulus())),
    /** −1, 0 or 1, as the value is below, at or above zero. */
    SIGN("sign", 1, arguments -> Math.signum(arguments[0]), null),
    /** The largest whole number not above the value. */
    FLOOR("floor", 1, arguments -> Math.floor(arguments[0]), null),
    /** The smallest whole number not below the value. */
    CEIL("ceil", 1, arguments -> Math.ceil(arguments[0]), null),
    /** The nearest whole number; a half goes to the even one. */
    ROUND("round", 1, arguments -> Math.rint(arguments[0]), null),
    /** The unit step: 1 from zero up, 0 below. */
    STEP("step", 1, arguments -> arguments[0] >= 0 ? 1 : 0, null),
    /** The smaller of two values. */
    MIN("min", 2, arguments -> Math.min(arguments[0], arguments[1]), null),
    /** The larger of two values. */
    MAX("max", 2, arguments -> Math.max(arguments[0], arguments[1]), null),
    /** The remainder of a division, of the divisor's sign: mod(a, b) = a − b·floor(a / b). */
    MOD("mod", 2, arguments -> arguments[0] - arguments[1] * Math.floor(arguments[0] / arguments[1]), null),
    /** The length of the hypotenuse: √(a² + b²), without overflow on the way. */
    HYPOT("hypot", 2, arguments -> Math.hypot(arguments[0], arguments[1]), null),
    /** A value raised to a power: pow(a, b) = a^b. */
    POW("pow", 2, arguments -> Math.pow(arguments[0], arguments[1]), arguments -> arguments[0].power(arguments[1])),
    /** The real part of a complex number. */
    RE("re", 1, arguments -> arguments[0], arguments -> Complex.of(arguments[0].real())),
    /** The imaginary part of a complex number. */
    IM("im", 1, arguments -> 0, arguments -> Complex.of(arguments[0].imaginary())),
    /** The argument of a complex number: its angle from the real axis, in radians. */
    ARG("arg", 1, arguments -> arguments[0] >= 0 ? 0 : Math.PI, arguments -> Complex.of(arguments[0].argument())),
    /** The complex conjugate. */
    CONJ("conj", 1, arguments -> arguments[0], arguments -> arguments[0].conjugate()),
    /** The gamma function: (n − 1)! for a whole n, defined between them too. */
    GAMMA("gamma", 1, arguments -> SpecialFunctions.gamma(arguments[0]), arguments -> SpecialFunctions.gamma(arguments[0])),
    /** n!: the product of the whole numbers from 1 to n. */
    FACTORIAL("factorial", 1, arguments -> SpecialFunctions.factorial(arguments[0]),
            arguments -> SpecialFunctions.gamma(arguments[0].add(Complex.ONE))),
    /** The binomial coefficient: in how many ways k things are chosen of n. */
    BINOMIAL("binomial", 2, arguments -> SpecialFunctions.binomial(arguments[0], arguments[1]),
            arguments -> SpecialFunctions.gamma(arguments[0].add(Complex.ONE)).divide(SpecialFunctions.gamma(arguments[1]
                    .add(Complex.ONE)).multiply(SpecialFunctions.gamma(arguments[0].subtract(arguments[1]).add(Complex.ONE))))),
    /** The error function, of the normal distribution. */
    ERF("erf", 1, arguments -> SpecialFunctions.erf(arguments[0]), null);

    @FunctionalInterface
    public interface RealImplementation {
        double apply(double[] arguments);
    }

    private final String symbol;
    private final int arity;
    private final RealImplementation real;
    private final Function<Complex[], Complex> complex;

    MathematicalFunction(String symbol, int arity, RealImplementation real, Function<Complex[], Complex> complex) {
        this.symbol = symbol;
        this.arity = arity;
        this.real = real;
        this.complex = complex;
    }

    public String symbol() {
        return symbol;
    }

    public int arity() {
        return arity;
    }

    public RealImplementation real() {
        return real;
    }

    public Optional<Function<Complex[], Complex>> complex() {
        return Optional.ofNullable(complex);
    }

    /**
     * tanh through {@link Math#exp}, which the JVM compiles to one instruction sequence, where {@link Math#tanh} falls
     * back to the strict software routine — a tenth of a device model's time went there. Near zero, where
     * {@code (1 − e) / (1 + e)} would cancel, the series is used; both agree with the exact value to about 1e-13.
     */
    public static double hyperbolicTangent(double value) {
        double magnitude = Math.abs(value);
        if (magnitude < 1e-3) {
            double square = value * value;
            return value * (1 - square / 3 + 2 * square * square / 15);
        }
        if (magnitude > 20) {
            return Math.copySign(1, value);
        }
        double decay = Math.exp(-2 * magnitude);
        return Math.copySign((1 - decay) / (1 + decay), value);
    }

    public static Optional<MathematicalFunction> bySymbol(String symbol) {
        String normalized = switch (symbol) {
            case "log" -> "ln";
            case "arcsin" -> "asin";
            case "arccos" -> "acos";
            case "arctan" -> "atan";
            case "tg" -> "tan";
            case "ctg" -> "cot";
            case "sgn" -> "sign";
            case "Gamma", "Γ" -> "gamma";
            case "choose" -> "binomial";
            default -> symbol;
        };
        return Arrays.stream(values()).filter(function -> function.symbol.equals(normalized)).findFirst();
    }
}
