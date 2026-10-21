package org.jmouse.spice.expression;

import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaPrinter;
import org.jmouse.formula.MathematicalFunction;
import org.jmouse.formula.Substitution;
import org.jmouse.spice.SpiceNumbers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Writes a jMF {@link Formula} as ngspice reads it in a value ({@code {…}}) or a B source, node by node — the tree is
 * walked, never its text rewritten.
 *
 * <p>Fully parenthesised; functions by ngspice's names; a comparison or a condition as {@code (c ? 1 : 0)}, since a
 * truth is a number in jMF; {@code mod} through {@code floor}, {@code step} and {@code hypot} by what they are. A call
 * of a function the document uses is written as the function's body with its arguments put in — a B source cannot
 * define a function, but it can say one. What SPICE has no way to say — a derivative, a host call such as
 * {@code current(pin)}, an infinite value — is {@link UntranslatableFormula}.</p>
 */
final class SpiceFormulaWriter {

    /** jMF's functions and how ngspice spells each; one missing here, and not written otherwise, has no SPICE spelling. */
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

    /** How deep calls of the document's functions may nest before one is taken for a function that calls itself. */
    private static final int DEEPEST_APPLICATION = 32;

    private final Function<String, String> variables;

    /** @param variables how each name the formula reads is written in SPICE; throws for a name SPICE cannot read */
    SpiceFormulaWriter(Function<String, String> variables) {
        this.variables = variables;
    }

    /** @param depth how many calls of the document's functions this formula was put in place of */
    String written(Formula formula, int depth) {
        return switch (formula) {
            case Formula.Constant constant -> number(constant.value());
            case Formula.NamedConstant named -> number(named.constant().value());
            case Formula.Variable variable -> variables.apply(variable.name());
            case Formula.Negation negation -> "(-" + written(negation.operand(), depth) + ")";
            case Formula.Binary binary -> "(" + written(binary.left(), depth) + " " + binary.operator().symbol() + " "
                    + written(binary.right(), depth) + ")";
            case Formula.Call call -> call(call, depth);
            case Formula.Comparison comparison -> "((" + written(comparison.left(), depth) + " "
                    + comparison.relation().symbol() + " " + written(comparison.right(), depth) + ") ? 1 : 0)";
            case Formula.Logical logical -> "((" + truth(logical.left(), depth)
                    + (logical.connective() == Formula.Connective.AND ? " && " : " || ")
                    + truth(logical.right(), depth) + ") ? 1 : 0)";
            case Formula.Not not -> "((" + written(not.operand(), depth) + " == 0) ? 1 : 0)";
            case Formula.Piecewise piecewise -> piecewise(piecewise, depth);
            case Formula.HostCall call -> throw new UntranslatableFormula("'" + call.name()
                    + "(…)' has no SPICE counterpart");
            case Formula.Application application -> applied(application, depth);
            default -> throw new UntranslatableFormula("'" + FormulaPrinter.text(formula) + "' has no SPICE counterpart");
        };
    }

    /**
     * A call of a function the document uses — {@code soft_minimum(a, b, width)} from {@code smooth-transitions} —
     * written as its body with the arguments put in: exactly the function. A function that calls itself has no finite
     * body to put in and stays untranslatable.
     */
    private String applied(Formula.Application application, int depth) {
        Formula.Definition definition = application.definition();
        if (depth >= DEEPEST_APPLICATION) {
            throw new UntranslatableFormula("'" + definition.name() + "(…)' calls itself, which SPICE cannot say");
        }
        Map<String, Formula> arguments = new LinkedHashMap<>();
        for (int index = 0; index < definition.parameters().size(); index++) {
            arguments.put(definition.parameters().get(index), application.arguments().get(index));
        }
        return written(Substitution.substitute(definition.body(), arguments), depth + 1);
    }

    private String truth(Formula formula, int depth) {
        return "(" + written(formula, depth) + " != 0)";
    }

    private String call(Formula.Call call, int depth) {
        List<String> arguments = call.arguments().stream().map(argument -> written(argument, depth)).toList();
        return switch (call.function()) {
            case MOD -> "(" + arguments.get(0) + " - " + arguments.get(1) + " * floor(" + arguments.get(0) + " / "
                    + arguments.get(1) + "))";
            case STEP -> "((" + arguments.get(0) + " >= 0) ? 1 : 0)";
            case HYPOT -> "sqrt(" + arguments.get(0) + " ^ 2 + " + arguments.get(1) + " ^ 2)";
            default -> {
                String name = SPICE_FUNCTIONS.get(call.function());
                if (name == null) {
                    throw new UntranslatableFormula("the function '" + call.function().name().toLowerCase(Locale.ROOT)
                            + "' has no SPICE counterpart");
                }
                yield name + "(" + String.join(", ", arguments) + ")";
            }
        };
    }

    private String piecewise(Formula.Piecewise piecewise, int depth) {
        if (piecewise.otherwise() == null) {
            throw new UntranslatableFormula("a piecewise value undefined where no branch holds has no SPICE counterpart");
        }
        String nested = written(piecewise.otherwise(), depth);
        for (int index = piecewise.branches().size() - 1; index >= 0; index--) {
            Formula.Piecewise.Branch branch = piecewise.branches().get(index);
            nested = "(" + truth(branch.condition(), depth) + " ? " + written(branch.value(), depth) + " : " + nested + ")";
        }
        return nested;
    }

    private static String number(double value) {
        if (!Double.isFinite(value)) {
            throw new UntranslatableFormula("an infinite value has no SPICE counterpart");
        }
        String written = SpiceNumbers.write(value);
        return value < 0 ? "(" + written + ")" : written;
    }
}
