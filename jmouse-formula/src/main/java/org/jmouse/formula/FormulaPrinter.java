package org.jmouse.formula;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Writes an expression back as plain notation or as LaTeX for KaTeX.
 *
 * <p>{@link #text} writes the expression alone; {@link #document} writes the author's definitions it uses
 * first, {@code f(x) = …; f(2)}, which is what parses back to the same tree.
 */
public final class FormulaPrinter {

    // Binding strength, loosest first; a child binding more loosely than its place allows is parenthesised.
    private static final int OR = 1;
    private static final int AND = 2;
    private static final int NOT = 3;
    private static final int RELATION = 4;
    private static final int ADDITIVE = 5;
    private static final int MULTIPLICATIVE = 6;
    private static final int UNARY = 7;
    private static final int POWER = 8;
    private static final int POSTFIX = 9;

    private FormulaPrinter() {
    }

    public static String text(Formula expression) {
        return text(expression, 0);
    }

    /** The definitions the expression calls, innermost first, then the expression. */
    public static String document(Formula expression) {
        Set<Formula.Definition> definitions = new LinkedHashSet<>();
        collectDefinitions(expression, definitions);
        StringBuilder document = new StringBuilder();
        for (Formula.Definition definition : definitions) {
            document.append(definition.name());
            if (!definition.parameters().isEmpty()) {
                document.append('(').append(String.join(", ", definition.parameters())).append(')');
            }
            document.append(" = ").append(text(definition.body())).append("; ");
        }
        return document.append(text(expression)).toString();
    }

    public static String latex(Formula expression) {
        return latex(expression, 0);
    }

    private static String text(Formula expression, int parent) {
        return switch (expression) {
            case Formula.Constant constant -> number(constant.value());
            case Formula.NamedConstant named -> named.constant().symbol();
            case Formula.ImaginaryUnit unit -> "i";
            case Formula.Variable variable -> variable.name();
            case Formula.Slot slot -> slot.label();
            case Formula.HostCall hostCall -> hostCall.name() + "(" + String.join(", ",
                    hostCall.arguments().stream().map(argument -> text(argument, 0)).toList()) + ")";
            case Formula.Negation negation -> wrap("-" + text(negation.operand(), UNARY), parent > MULTIPLICATIVE);
            case Formula.Binary binary -> {
                int precedence = precedenceOf(binary.operator());
                boolean power = binary.operator() == Formula.Operator.POWER;
                String left = text(binary.left(), power ? precedence + 1 : precedence);
                String right = text(binary.right(), power ? precedence : precedence + 1);
                String separator = power ? "^" : " " + binary.operator().symbol() + " ";
                yield wrap(left + separator + right, precedence < parent);
            }
            case Formula.Call call when call.function() == MathematicalFunction.FACTORIAL ->
                    text(call.arguments().getFirst(), POSTFIX) + "!";
            case Formula.Call call -> call.function().symbol() + arguments(call.arguments());
            case Formula.Comparison comparison -> wrap(text(comparison.left(), RELATION + 1) + " "
                    + comparison.relation().symbol() + " " + text(comparison.right(), RELATION + 1), RELATION < parent);
            case Formula.Logical logical -> {
                int precedence = logical.connective() == Formula.Connective.AND ? AND : OR;
                yield wrap(text(logical.left(), precedence) + " " + logical.connective().symbol() + " "
                        + text(logical.right(), precedence + 1), precedence < parent);
            }
            case Formula.Not not -> wrap("!" + text(not.operand(), RELATION + 1), NOT < parent);
            case Formula.Equation equation -> wrap(text(equation.left(), 0) + " = " + text(equation.right(), 0), parent > 0);
            case Formula.Piecewise piecewise -> piecewiseText(piecewise);
            case Formula.Accumulation accumulation -> accumulation.accumulator().symbol() + "(" + accumulation.index()
                    + ", " + text(accumulation.from(), 0) + ", " + text(accumulation.to(), 0) + ", "
                    + text(accumulation.body(), 0) + ")";
            case Formula.Integral integral -> "integral(" + integral.variable() + ", " + text(integral.lower(), 0)
                    + ", " + text(integral.upper(), 0) + ", " + text(integral.body(), 0) + ")";
            case Formula.Application application -> application.definition().name()
                    + (application.arguments().isEmpty() ? "" : arguments(application.arguments()));
        };
    }

    private static String piecewiseText(Formula.Piecewise piecewise) {
        List<Formula.Piecewise.Branch> branches = piecewise.branches();
        String otherwise = piecewise.otherwise() == null ? "" : ", " + text(piecewise.otherwise(), 0);
        if (branches.size() == 1) {
            Formula.Piecewise.Branch branch = branches.getFirst();
            return "if(" + text(branch.condition(), 0) + ", " + text(branch.value(), 0) + otherwise + ")";
        }
        return "piecewise(" + branches.stream()
                .map(branch -> text(branch.condition(), 0) + ", " + text(branch.value(), 0))
                .collect(Collectors.joining(", ")) + otherwise + ")";
    }

    private static String arguments(List<Formula> arguments) {
        return "(" + arguments.stream().map(argument -> text(argument, 0)).collect(Collectors.joining(", ")) + ")";
    }

    private static int precedenceOf(Formula.Operator operator) {
        return switch (operator) {
            case ADD, SUBTRACT -> ADDITIVE;
            case MULTIPLY, DIVIDE -> MULTIPLICATIVE;
            case POWER -> POWER;
        };
    }

    private static String latex(Formula expression, int parent) {
        return switch (expression) {
            case Formula.Constant constant -> Double.isInfinite(constant.value())
                    ? (constant.value() > 0 ? "\\infty" : "-\\infty") : number(constant.value());
            case Formula.NamedConstant named -> named.constant().latex();
            case Formula.ImaginaryUnit unit -> "i";
            case Formula.Variable variable -> latexName(variable.name());
            case Formula.Slot slot -> latexName(slot.label());
            case Formula.HostCall hostCall -> "\\operatorname{" + hostCall.name().replace("_", "\\_") + "}\\left(" + String.join(", ",
                    hostCall.arguments().stream().map(argument -> latex(argument, 0)).toList()) + "\\right)";
            case Formula.Negation negation -> wrapLatex("-" + latex(negation.operand(), UNARY), parent > MULTIPLICATIVE);
            case Formula.Binary binary -> switch (binary.operator()) {
                // A fraction raised to a power keeps its parentheses: (a/b)² is not a/b².
                case DIVIDE -> wrapLatex("\\frac{" + latex(binary.left(), 0) + "}{" + latex(binary.right(), 0) + "}",
                        parent >= POSTFIX);
                case POWER -> latex(binary.left(), POSTFIX) + "^{" + latex(binary.right(), 0) + "}";
                case MULTIPLY -> wrapLatex(latex(binary.left(), MULTIPLICATIVE) + " \\cdot "
                        + latex(binary.right(), UNARY), parent > MULTIPLICATIVE);
                case ADD -> wrapLatex(latex(binary.left(), ADDITIVE) + " + " + latex(binary.right(), MULTIPLICATIVE),
                        parent > ADDITIVE);
                case SUBTRACT -> wrapLatex(latex(binary.left(), ADDITIVE) + " - " + latex(binary.right(), MULTIPLICATIVE),
                        parent > ADDITIVE);
            };
            case Formula.Call call -> callLatex(call);
            case Formula.Comparison comparison -> wrapLatex(latex(comparison.left(), RELATION + 1) + " "
                    + comparison.relation().latex() + " " + latex(comparison.right(), RELATION + 1), RELATION < parent);
            case Formula.Logical logical -> {
                int precedence = logical.connective() == Formula.Connective.AND ? AND : OR;
                yield wrapLatex(latex(logical.left(), precedence) + " " + logical.connective().latex() + " "
                        + latex(logical.right(), precedence + 1), precedence < parent);
            }
            case Formula.Not not -> wrapLatex("\\lnot " + latex(not.operand(), RELATION + 1), NOT < parent);
            case Formula.Equation equation -> latex(equation.left(), 0) + " = " + latex(equation.right(), 0);
            case Formula.Piecewise piecewise -> "\\begin{cases} " + piecewise.branches().stream()
                    .map(branch -> latex(branch.value(), 0) + " & " + latex(branch.condition(), 0))
                    .collect(Collectors.joining(" \\\\ "))
                    + (piecewise.otherwise() == null ? ""
                    : " \\\\ " + latex(piecewise.otherwise(), 0) + " & \\text{otherwise}") + " \\end{cases}";
            case Formula.Accumulation accumulation -> wrapLatex(accumulation.accumulator().latex() + "_{"
                    + latexName(accumulation.index()) + "=" + latex(accumulation.from(), 0) + "}^{"
                    + latex(accumulation.to(), 0) + "} " + latex(accumulation.body(), MULTIPLICATIVE), parent > ADDITIVE);
            case Formula.Integral integral -> wrapLatex("\\int_{" + latex(integral.lower(), 0) + "}^{"
                    + latex(integral.upper(), 0) + "} " + latex(integral.body(), ADDITIVE) + " \\, d"
                    + latexName(integral.variable()), parent > ADDITIVE);
            case Formula.Application application -> latexName(application.definition().name())
                    + (application.arguments().isEmpty() ? "" : "\\left(" + application.arguments().stream()
                    .map(argument -> latex(argument, 0)).collect(Collectors.joining(", ")) + "\\right)");
        };
    }

    private static String callLatex(Formula.Call call) {
        Formula first = call.arguments().getFirst();
        return switch (call.function()) {
            case SQRT -> "\\sqrt{" + latex(first, 0) + "}";
            case CBRT -> "\\sqrt[3]{" + latex(first, 0) + "}";
            case ABS -> "\\left|" + latex(first, 0) + "\\right|";
            case FLOOR -> "\\left\\lfloor " + latex(first, 0) + " \\right\\rfloor";
            case CEIL -> "\\left\\lceil " + latex(first, 0) + " \\right\\rceil";
            case FACTORIAL -> latex(first, POSTFIX) + "!";
            case GAMMA -> "\\Gamma\\left(" + latex(first, 0) + "\\right)";
            case BINOMIAL -> "\\binom{" + latex(first, 0) + "}{" + latex(call.arguments().get(1), 0) + "}";
            case SIN, COS, TAN, COT, SEC, CSC, SINH, COSH, TANH, EXP, LN, ARG -> "\\" + call.function().symbol()
                    + "\\left(" + latex(first, 0) + "\\right)";
            default -> "\\operatorname{" + call.function().symbol() + "}\\left("
                    + call.arguments().stream().map(argument -> latex(argument, 0)).collect(Collectors.joining(", "))
                    + "\\right)";
        };
    }

    private static void collectDefinitions(Formula expression, Set<Formula.Definition> found) {
        switch (expression) {
            case Formula.Application application -> {
                collectDefinitions(application.definition().body(), found);
                found.add(application.definition());
                application.arguments().forEach(argument -> collectDefinitions(argument, found));
            }
            case Formula.Negation negation -> collectDefinitions(negation.operand(), found);
            case Formula.Not not -> collectDefinitions(not.operand(), found);
            case Formula.Binary binary -> {
                collectDefinitions(binary.left(), found);
                collectDefinitions(binary.right(), found);
            }
            case Formula.Comparison comparison -> {
                collectDefinitions(comparison.left(), found);
                collectDefinitions(comparison.right(), found);
            }
            case Formula.Logical logical -> {
                collectDefinitions(logical.left(), found);
                collectDefinitions(logical.right(), found);
            }
            case Formula.Equation equation -> {
                collectDefinitions(equation.left(), found);
                collectDefinitions(equation.right(), found);
            }
            case Formula.Call call -> call.arguments().forEach(argument -> collectDefinitions(argument, found));
            case Formula.Piecewise piecewise -> {
                piecewise.branches().forEach(branch -> {
                    collectDefinitions(branch.condition(), found);
                    collectDefinitions(branch.value(), found);
                });
                if (piecewise.otherwise() != null) {
                    collectDefinitions(piecewise.otherwise(), found);
                }
            }
            case Formula.Accumulation accumulation -> {
                collectDefinitions(accumulation.from(), found);
                collectDefinitions(accumulation.to(), found);
                collectDefinitions(accumulation.body(), found);
            }
            case Formula.Integral integral -> {
                collectDefinitions(integral.lower(), found);
                collectDefinitions(integral.upper(), found);
                collectDefinitions(integral.body(), found);
            }
            case Formula.Constant constant -> {
            }
            case Formula.NamedConstant named -> {
            }
            case Formula.ImaginaryUnit unit -> {
            }
            case Formula.Variable variable -> {
            }
            case Formula.Slot slot -> {
            }
            case Formula.HostCall hostCall -> hostCall.arguments().forEach(argument -> collectDefinitions(argument, found));
        }
    }

    private static String latexName(String name) {
        return switch (name) {
            case "alpha", "beta", "gamma", "delta", "epsilon", "theta", "lambda", "mu", "nu", "rho", "sigma", "omega",
                 "kappa", "xi", "zeta", "eta", "tau", "phi", "psi", "chi" -> "\\" + name;
            default -> name.length() > 1 && name.contains("_") ? name.replaceFirst("_(.*)", "_{$1}") : name;
        };
    }

    private static String number(double value) {
        if (Double.isInfinite(value)) {
            return value > 0 ? "inf" : "-inf";
        }
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static String wrap(String text, boolean needed) {
        return needed ? "(" + text + ")" : text;
    }

    private static String wrapLatex(String text, boolean needed) {
        return needed ? "\\left(" + text + "\\right)" : text;
    }
}
