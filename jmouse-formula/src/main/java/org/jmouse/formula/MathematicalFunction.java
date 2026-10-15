package org.jmouse.formula;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * The functions an expression may call. Each carries its real implementation and, where the function has one,
 * its complex one; a function without a complex form is refused by the complex compiler rather than guessed.
 */
public enum MathematicalFunction {

    SIN("sin", 1, arguments -> Math.sin(arguments[0]), arguments -> arguments[0].sin()),
    COS("cos", 1, arguments -> Math.cos(arguments[0]), arguments -> arguments[0].cos()),
    TAN("tan", 1, arguments -> Math.tan(arguments[0]), arguments -> arguments[0].tan()),
    COT("cot", 1, arguments -> 1 / Math.tan(arguments[0]), arguments -> Complex.ONE.divide(arguments[0].tan())),
    SEC("sec", 1, arguments -> 1 / Math.cos(arguments[0]), arguments -> Complex.ONE.divide(arguments[0].cos())),
    CSC("csc", 1, arguments -> 1 / Math.sin(arguments[0]), arguments -> Complex.ONE.divide(arguments[0].sin())),
    ASIN("asin", 1, arguments -> Math.asin(arguments[0]), null),
    ACOS("acos", 1, arguments -> Math.acos(arguments[0]), null),
    ATAN("atan", 1, arguments -> Math.atan(arguments[0]), null),
    ATAN2("atan2", 2, arguments -> Math.atan2(arguments[0], arguments[1]), null),
    SINH("sinh", 1, arguments -> Math.sinh(arguments[0]), arguments -> arguments[0].sinh()),
    COSH("cosh", 1, arguments -> Math.cosh(arguments[0]), arguments -> arguments[0].cosh()),
    TANH("tanh", 1, arguments -> hyperbolicTangent(arguments[0]), arguments -> arguments[0].tanh()),
    ASINH("asinh", 1, arguments -> Math.log(arguments[0] + Math.sqrt(arguments[0] * arguments[0] + 1)), null),
    ACOSH("acosh", 1, arguments -> Math.log(arguments[0] + Math.sqrt(arguments[0] * arguments[0] - 1)), null),
    ATANH("atanh", 1, arguments -> 0.5 * Math.log((1 + arguments[0]) / (1 - arguments[0])), null),
    EXP("exp", 1, arguments -> Math.exp(arguments[0]), arguments -> arguments[0].exp()),
    LN("ln", 1, arguments -> Math.log(arguments[0]), arguments -> arguments[0].log()),
    LOG10("log10", 1, arguments -> Math.log10(arguments[0]), null),
    LOG2("log2", 1, arguments -> Math.log(arguments[0]) / Math.log(2), null),
    SQRT("sqrt", 1, arguments -> Math.sqrt(arguments[0]), arguments -> arguments[0].sqrt()),
    CBRT("cbrt", 1, arguments -> Math.cbrt(arguments[0]), null),
    ABS("abs", 1, arguments -> Math.abs(arguments[0]), arguments -> Complex.of(arguments[0].modulus())),
    SIGN("sign", 1, arguments -> Math.signum(arguments[0]), null),
    FLOOR("floor", 1, arguments -> Math.floor(arguments[0]), null),
    CEIL("ceil", 1, arguments -> Math.ceil(arguments[0]), null),
    ROUND("round", 1, arguments -> Math.rint(arguments[0]), null),
    STEP("step", 1, arguments -> arguments[0] >= 0 ? 1 : 0, null),
    MIN("min", 2, arguments -> Math.min(arguments[0], arguments[1]), null),
    MAX("max", 2, arguments -> Math.max(arguments[0], arguments[1]), null),
    MOD("mod", 2, arguments -> arguments[0] - arguments[1] * Math.floor(arguments[0] / arguments[1]), null),
    HYPOT("hypot", 2, arguments -> Math.hypot(arguments[0], arguments[1]), null),
    POW("pow", 2, arguments -> Math.pow(arguments[0], arguments[1]), arguments -> arguments[0].power(arguments[1])),
    RE("re", 1, arguments -> arguments[0], arguments -> Complex.of(arguments[0].real())),
    IM("im", 1, arguments -> 0, arguments -> Complex.of(arguments[0].imaginary())),
    ARG("arg", 1, arguments -> arguments[0] >= 0 ? 0 : Math.PI, arguments -> Complex.of(arguments[0].argument())),
    CONJ("conj", 1, arguments -> arguments[0], arguments -> arguments[0].conjugate()),
    GAMMA("gamma", 1, arguments -> SpecialFunctions.gamma(arguments[0]), arguments -> SpecialFunctions.gamma(arguments[0])),
    FACTORIAL("factorial", 1, arguments -> SpecialFunctions.factorial(arguments[0]),
            arguments -> SpecialFunctions.gamma(arguments[0].add(Complex.ONE))),
    BINOMIAL("binomial", 2, arguments -> SpecialFunctions.binomial(arguments[0], arguments[1]),
            arguments -> SpecialFunctions.gamma(arguments[0].add(Complex.ONE)).divide(SpecialFunctions.gamma(arguments[1]
                    .add(Complex.ONE)).multiply(SpecialFunctions.gamma(arguments[0].subtract(arguments[1]).add(Complex.ONE))))),
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
