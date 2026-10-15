package org.jmouse.parallax.spice;

import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaPrinter;
import org.jmouse.formula.MathematicalFunction;
import org.jmouse.formula.el.FormulaLanguage;
import org.jmouse.spice.SpiceNumbers;
import org.jmouse.spice.SpiceParseException;
import org.jmouse.spice.SpicePosition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expressions in both directions: a SPICE expression — {@code {2*R1}}, {@code '1/(2*pi*R*C)'}, a {@code .param}
 * value — read as a jMF {@link Formula} over the parameters, and a jMF formula written as SPICE writes it in a value
 * ({@code {…}}) or a B source.
 *
 * <h2>SPICE to jMF</h2>
 *
 * <p>The expression is cut into SPICE's own tokens here — numbers keep their scale factors ({@code 1k} is 1000),
 * {@code **} is a power, {@code &&} and {@code ||} are {@code and} and {@code or}, names are matched to the declared
 * parameters without regard to case — and the result is read by jMF's own parser, so what jMF cannot read is refused
 * by jMF, at the expression's place. Refused rather than guessed: {@code log}, which is the natural logarithm to some
 * SPICEs and the decimal one to others; {@code ?:}; and {@code V(…)} or {@code I(…)} where a value is wanted.</p>
 *
 * <h2>jMF to SPICE</h2>
 *
 * <p>Fully parenthesised, functions by ngspice's names, a comparison or a condition written as {@code (c ? 1 : 0)} — a
 * truth is a number in jMF — and {@code mod} through {@code floor}. What has no SPICE spelling — a derivative, a call of
 * the author's own function, a host call such as {@code current(pin)} — is {@link Untranslatable}.</p>
 */
public final class SpiceExpressions {

    private static final Pattern NUMBER = Pattern.compile("(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?[A-Za-zµμ]*");
    private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** SPICE's functions and the jMF function each one is. */
    private static final Map<String, String> FUNCTIONS = Map.ofEntries(
            Map.entry("sqrt", "sqrt"), Map.entry("exp", "exp"), Map.entry("ln", "ln"), Map.entry("log10", "log10"),
            Map.entry("sin", "sin"), Map.entry("cos", "cos"), Map.entry("tan", "tan"), Map.entry("asin", "asin"),
            Map.entry("acos", "acos"), Map.entry("atan", "atan"), Map.entry("sinh", "sinh"), Map.entry("cosh", "cosh"),
            Map.entry("tanh", "tanh"), Map.entry("abs", "abs"), Map.entry("floor", "floor"), Map.entry("ceil", "ceil"),
            Map.entry("min", "min"), Map.entry("max", "max"), Map.entry("pow", "pow"), Map.entry("pwr", "pow"),
            Map.entry("sgn", "sign"));

    /** jMF's functions and how ngspice spells each; one missing here has no SPICE spelling. */
    private static final Map<MathematicalFunction, String> SPICE_FUNCTIONS = Map.ofEntries(
            Map.entry(MathematicalFunction.SQRT, "sqrt"), Map.entry(MathematicalFunction.EXP, "exp"),
            Map.entry(MathematicalFunction.LN, "ln"), Map.entry(MathematicalFunction.LOG10, "log10"),
            Map.entry(MathematicalFunction.SIN, "sin"), Map.entry(MathematicalFunction.COS, "cos"),
            Map.entry(MathematicalFunction.TAN, "tan"), Map.entry(MathematicalFunction.ASIN, "asin"),
            Map.entry(MathematicalFunction.ACOS, "acos"), Map.entry(MathematicalFunction.ATAN, "atan"),
            Map.entry(MathematicalFunction.SINH, "sinh"), Map.entry(MathematicalFunction.COSH, "cosh"),
            Map.entry(MathematicalFunction.TANH, "tanh"), Map.entry(MathematicalFunction.ABS, "abs"),
            Map.entry(MathematicalFunction.FLOOR, "floor"), Map.entry(MathematicalFunction.CEIL, "ceil"),
            Map.entry(MathematicalFunction.MIN, "min"), Map.entry(MathematicalFunction.MAX, "max"),
            Map.entry(MathematicalFunction.POW, "pow"), Map.entry(MathematicalFunction.SIGN, "sgn"));

    private SpiceExpressions() {
    }

    /** A jMF formula that SPICE has no way to say, and why. */
    public static final class Untranslatable extends RuntimeException {
        public Untranslatable(String message) {
            super(message);
        }
    }

    // ─── SPICE → jMF ────────────────────────────────────────────────────────────────────────────────

    /**
     * Reads a SPICE expression as a jMF formula.
     *
     * @param written    the expression as written, without its braces or quotes
     * @param position   where it was written, for a refusal
     * @param parameters the parameters it may name, each by its lower-case name → the spelling it was declared with
     * @throws SpiceParseException when it names what is not declared, uses what jMF cannot say, or does not read
     */
    public static Formula read(String written, SpicePosition position, Map<String, String> parameters) {
        String text = jmfText(written, position, parameters);
        try {
            return FormulaLanguage.standard().parse(text);
        } catch (RuntimeException refused) {
            throw new SpiceParseException(position, "'" + written.strip() + "' does not read as an expression: "
                    + refused.getMessage());
        }
    }

    /** The SPICE expression in jMF's notation, token by token. */
    private static String jmfText(String written, SpicePosition position, Map<String, String> parameters) {
        StringBuilder text = new StringBuilder();
        int index = 0;
        while (index < written.length()) {
            char character = written.charAt(index);
            SpicePosition at = new SpicePosition(position.line(), position.column() + 1 + index);
            if (Character.isWhitespace(character)) {
                text.append(' ');
                index++;
                continue;
            }
            Matcher number = NUMBER.matcher(written).region(index, written.length());
            if ((Character.isDigit(character) || character == '.') && number.lookingAt()) {
                String literal = number.group();
                SpiceNumbers.Engineering value = SpiceNumbers.read(literal)
                        .orElseThrow(() -> new SpiceParseException(at, "'" + literal + "' is not a number"));
                text.append(jmfNumber(value.value()));
                index = number.end();
                continue;
            }
            Matcher name = NAME.matcher(written).region(index, written.length());
            if (name.lookingAt()) {
                text.append(jmfName(name.group(), followedByParenthesis(written, name.end()), at, parameters));
                index = name.end();
                continue;
            }
            String two = index + 1 < written.length() ? written.substring(index, index + 2) : "";
            switch (two) {
                case "**" -> text.append('^');
                case "&&" -> text.append(" and ");
                case "||" -> text.append(" or ");
                case "<=", ">=", "==", "!=" -> text.append(two);
                default -> {
                    two = "";
                    switch (character) {
                        case '+', '-', '*', '/', '^', '(', ')', ',', '<', '>' -> text.append(character);
                        case '!' -> text.append(" not ");
                        case '?', ':' -> throw new SpiceParseException(at,
                                "a conditional '?:' has no jMF counterpart; write the value it takes");
                        default -> throw new SpiceParseException(at, "'" + character + "' is not part of an expression");
                    }
                }
            }
            index += two.isEmpty() ? 1 : 2;
        }
        return text.toString();
    }

    private static String jmfName(String name, boolean called, SpicePosition at, Map<String, String> parameters) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (called) {
            if (lower.equals("v") || lower.equals("i")) {
                throw new SpiceParseException(at, "'" + name + "(…)' reads the running circuit; a value here is a "
                        + "number or an expression of the parameters");
            }
            if (lower.equals("log")) {
                throw new SpiceParseException(at, "'log' is the natural logarithm to some SPICEs and the decimal one to "
                        + "others; write ln(…) or log10(…)");
            }
            String function = FUNCTIONS.get(lower);
            if (function == null) {
                throw new SpiceParseException(at, "the function '" + name + "' has no jMF counterpart");
            }
            return function;
        }
        if (lower.equals("pi")) {
            return "pi";
        }
        String declared = parameters.get(lower);
        if (declared == null) {
            throw new SpiceParseException(at, "'" + name + "' is not a parameter declared before it");
        }
        return declared;
    }

    private static boolean followedByParenthesis(String text, int from) {
        int index = from;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index < text.length() && text.charAt(index) == '(';
    }

    /** A number as jMF reads it back exactly: plain decimals, never an exponent. */
    static String jmfNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    /** A formula in jMF's notation, as a jMX document writes it. */
    public static String jmf(Formula formula) {
        return FormulaPrinter.text(formula);
    }

    // ─── jMF → SPICE ────────────────────────────────────────────────────────────────────────────────

    /**
     * Writes a jMF formula in SPICE's notation.
     *
     * @param variables how each name the formula reads is written in SPICE — a parameter, {@code V(node)}, {@code time};
     *                  throws {@link Untranslatable} for a name SPICE cannot read
     * @throws Untranslatable when the formula holds what SPICE cannot say
     */
    public static String spice(Formula formula, Function<String, String> variables) {
        return switch (formula) {
            case Formula.Constant constant -> number(constant.value());
            case Formula.NamedConstant named -> number(named.constant().value());
            case Formula.Variable variable -> variables.apply(variable.name());
            case Formula.Negation negation -> "(-" + spice(negation.operand(), variables) + ")";
            case Formula.Binary binary -> binary(binary, variables);
            case Formula.Call call -> call(call, variables);
            case Formula.Comparison comparison -> "((" + spice(comparison.left(), variables) + " "
                    + comparison.relation().symbol() + " " + spice(comparison.right(), variables) + ") ? 1 : 0)";
            case Formula.Logical logical -> "((" + truth(logical.left(), variables)
                    + (logical.connective() == Formula.Connective.AND ? " && " : " || ")
                    + truth(logical.right(), variables) + ") ? 1 : 0)";
            case Formula.Not not -> "((" + spice(not.operand(), variables) + " == 0) ? 1 : 0)";
            case Formula.Piecewise piecewise -> piecewise(piecewise, variables);
            case Formula.HostCall call -> throw new Untranslatable("'" + call.name() + "(…)' has no SPICE counterpart");
            case Formula.Application application -> throw new Untranslatable("'" + application.definition().name()
                    + "(…)' is a function of the document's own, which SPICE cannot call");
            default -> throw new Untranslatable("'" + FormulaPrinter.text(formula) + "' has no SPICE counterpart");
        };
    }

    private static String truth(Formula formula, Function<String, String> variables) {
        return "(" + spice(formula, variables) + " != 0)";
    }

    private static String binary(Formula.Binary binary, Function<String, String> variables) {
        String left = spice(binary.left(), variables);
        String right = spice(binary.right(), variables);
        return "(" + left + " " + binary.operator().symbol() + " " + right + ")";
    }

    private static String call(Formula.Call call, Function<String, String> variables) {
        List<String> arguments = call.arguments().stream().map(argument -> spice(argument, variables)).toList();
        return switch (call.function()) {
            case MOD -> "(" + arguments.get(0) + " - " + arguments.get(1) + " * floor(" + arguments.get(0) + " / "
                    + arguments.get(1) + "))";
            case STEP -> "((" + arguments.get(0) + " >= 0) ? 1 : 0)";
            case HYPOT -> "sqrt(" + arguments.get(0) + " ^ 2 + " + arguments.get(1) + " ^ 2)";
            default -> {
                String name = SPICE_FUNCTIONS.get(call.function());
                if (name == null) {
                    throw new Untranslatable("the function '" + call.function().name().toLowerCase(Locale.ROOT)
                            + "' has no SPICE counterpart");
                }
                yield name + "(" + String.join(", ", arguments) + ")";
            }
        };
    }

    private static String piecewise(Formula.Piecewise piecewise, Function<String, String> variables) {
        if (piecewise.otherwise() == null) {
            throw new Untranslatable("a piecewise value undefined where no branch holds has no SPICE counterpart");
        }
        String written = spice(piecewise.otherwise(), variables);
        for (int index = piecewise.branches().size() - 1; index >= 0; index--) {
            Formula.Piecewise.Branch branch = piecewise.branches().get(index);
            written = "(" + truth(branch.condition(), variables) + " ? " + spice(branch.value(), variables) + " : "
                    + written + ")";
        }
        return written;
    }

    private static String number(double value) {
        if (!Double.isFinite(value)) {
            throw new Untranslatable("an infinite value has no SPICE counterpart");
        }
        String written = SpiceNumbers.write(value);
        return value < 0 ? "(" + written + ")" : written;
    }
}
