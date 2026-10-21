package org.jmouse.spice.expression;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.ComparisonOperator;
import org.jmouse.el.extension.operator.LogicalOperator;
import org.jmouse.el.extension.operator.MathematicOperator;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ArgumentsNode;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.node.expression.LiteralNode;
import org.jmouse.el.node.expression.PropertyNode;
import org.jmouse.el.node.expression.TernaryNode;
import org.jmouse.el.node.expression.unary.NegativeUnaryOperation;
import org.jmouse.formula.Formula;
import org.jmouse.formula.MathematicalConstant;
import org.jmouse.formula.MathematicalFunction;
import org.jmouse.formula.el.FormulaOperator;
import org.jmouse.formula.el.NotNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads the jME tree of a SPICE expression into jMF's {@link Formula}, with SPICE's meaning: its function names, its
 * names compared without regard to case, a truth as a number.
 *
 * <h2>What is read, decided once</h2>
 * <ul>
 *     <li>A name is a parameter declared before the expression, matched without regard to case and written as it was
 *     declared; {@code pi} is π unless a parameter is called so. Any other name is refused.</li>
 *     <li>SPICE's functions are jMF's of the same meaning ({@link #FUNCTIONS}); a few are forms said through jMF's:
 *     {@code if(c, a, b)} and {@code c ? a : b} are a piecewise value, {@code limit(x, low, high)} is
 *     {@code min(max(x, low), high)}, {@code pwr(x, y)} is ngspice's {@code sgn(x)·|x|^y} — not {@code pow}.</li>
 *     <li>Refused rather than guessed: {@code log}, the natural logarithm to some SPICEs and the decimal one to
 *     others; {@code V(…)} and {@code I(…)}, which read the running circuit where a value is wanted.</li>
 * </ul>
 */
final class SpiceFormulaTranslator {

    /** SPICE's functions and the jMF function each one is, by the name SPICE writes it in lower case. */
    private static final Map<String, MathematicalFunction> FUNCTIONS = Map.ofEntries(
            Map.entry("sqrt", MathematicalFunction.SQRT), Map.entry("exp", MathematicalFunction.EXP),
            Map.entry("ln", MathematicalFunction.LN), Map.entry("log10", MathematicalFunction.LOG10),
            Map.entry("sin", MathematicalFunction.SIN), Map.entry("cos", MathematicalFunction.COS),
            Map.entry("tan", MathematicalFunction.TAN), Map.entry("asin", MathematicalFunction.ASIN),
            Map.entry("acos", MathematicalFunction.ACOS), Map.entry("atan", MathematicalFunction.ATAN),
            Map.entry("sinh", MathematicalFunction.SINH), Map.entry("cosh", MathematicalFunction.COSH),
            Map.entry("tanh", MathematicalFunction.TANH), Map.entry("abs", MathematicalFunction.ABS),
            Map.entry("floor", MathematicalFunction.FLOOR), Map.entry("ceil", MathematicalFunction.CEIL),
            Map.entry("min", MathematicalFunction.MIN), Map.entry("max", MathematicalFunction.MAX),
            Map.entry("pow", MathematicalFunction.POW), Map.entry("sgn", MathematicalFunction.SIGN));

    private static final String CONDITIONAL = "if";
    private static final String LIMIT = "limit";
    private static final String SIGNED_POWER = "pwr";
    private static final String AMBIGUOUS_LOGARITHM = "log";
    private static final List<String> CIRCUIT_READINGS = List.of("v", "i");

    /** The parameters a name may be: lower-case name → the spelling it was declared with. */
    private final Map<String, String> parameters;

    SpiceFormulaTranslator(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    Formula translate(Expression expression) {
        return switch (expression) {
            case NegativeUnaryOperation negation -> new Formula.Negation(translate(negation.getOperand()));
            case NotNode not -> new Formula.Not(translate(not.getOperand()));
            case BinaryOperation binary -> binary(binary);
            case TernaryNode ternary -> conditional(translate(ternary.getCondition()), translate(ternary.getThenBranch()),
                    translate(ternary.getElseBranch()));
            case LiteralNode<?> literal when literal.getValue() instanceof Number number -> new Formula.Constant(
                    number.doubleValue());
            case PropertyNode name -> name(name);
            case FunctionNode function -> function(function);
            default -> throw new SpiceExpressionRefusal(offsetOf(expression),
                    "'" + expression.toSource() + "' is not part of an expression");
        };
    }

    private Formula binary(BinaryOperation binary) {
        Formula left = translate(binary.getLeft());
        Formula right = translate(binary.getRight());
        Operator operator = binary.getOperator();
        if (operator == FormulaOperator.POWER || operator == MathematicOperator.EXPONENTIAL) {
            return power(left, right);
        }
        if (operator instanceof MathematicOperator mathematic) {
            return switch (mathematic) {
                case PLUS -> new Formula.Binary(Formula.Operator.ADD, left, right);
                case SUBTRACT -> new Formula.Binary(Formula.Operator.SUBTRACT, left, right);
                case MULTIPLY -> new Formula.Binary(Formula.Operator.MULTIPLY, left, right);
                case DIVIDE -> new Formula.Binary(Formula.Operator.DIVIDE, left, right);
                case MODULUS, EXPONENTIAL -> throw new SpiceExpressionRefusal(offsetOf(binary),
                        "'" + operator.getSpelling() + "' is not an operator of SPICE expressions");
            };
        }
        if (operator instanceof ComparisonOperator comparison) {
            Formula.Relation relation = switch (comparison) {
                case LT -> Formula.Relation.LESS;
                case LTE -> Formula.Relation.LESS_OR_EQUAL;
                case GT -> Formula.Relation.GREATER;
                case GTE -> Formula.Relation.GREATER_OR_EQUAL;
                case EQUAL -> Formula.Relation.EQUAL;
                case NOT_EQUAL -> Formula.Relation.NOT_EQUAL;
            };
            return new Formula.Comparison(relation, left, right);
        }
        if (operator == LogicalOperator.AND) {
            return new Formula.Logical(Formula.Connective.AND, left, right);
        }
        if (operator == LogicalOperator.OR) {
            return new Formula.Logical(Formula.Connective.OR, left, right);
        }
        throw new SpiceExpressionRefusal(offsetOf(binary),
                "'" + operator.getSpelling() + "' is not an operator of SPICE expressions");
    }

    private Formula name(PropertyNode name) {
        String written = name.getPath();
        String lower = written.toLowerCase(Locale.ROOT);
        if (lower.equals(MathematicalConstant.PI.symbol())) {
            return new Formula.NamedConstant(MathematicalConstant.PI);
        }
        String declared = parameters.get(lower);
        if (declared != null) {
            return new Formula.Variable(declared);
        }
        throw new SpiceExpressionRefusal(offsetOf(name), "'" + written + "' is not a parameter declared before it");
    }

    private Formula function(FunctionNode function) {
        String written = function.getName();
        String lower = written.toLowerCase(Locale.ROOT);
        int offset = offsetOf(function);
        if (CIRCUIT_READINGS.contains(lower)) {
            throw new SpiceExpressionRefusal(offset, "'" + written + "(…)' reads the running circuit; a value here is a "
                    + "number or an expression of the parameters");
        }
        if (lower.equals(AMBIGUOUS_LOGARITHM)) {
            throw new SpiceExpressionRefusal(offset, "'log' is the natural logarithm to some SPICEs and the decimal one to "
                    + "others; write ln(…) or log10(…)");
        }
        List<Formula> arguments = arguments(function);
        return switch (lower) {
            case CONDITIONAL -> {
                requireArguments(written, arguments, 3, offset);
                yield conditional(arguments.get(0), arguments.get(1), arguments.get(2));
            }
            case LIMIT -> {
                requireArguments(written, arguments, 3, offset);
                yield new Formula.Call(MathematicalFunction.MIN, List.of(
                        new Formula.Call(MathematicalFunction.MAX, List.of(arguments.get(0), arguments.get(1))),
                        arguments.get(2)));
            }
            case SIGNED_POWER -> {
                requireArguments(written, arguments, 2, offset);
                Formula base = arguments.get(0);
                yield new Formula.Binary(Formula.Operator.MULTIPLY,
                        new Formula.Call(MathematicalFunction.SIGN, List.of(base)),
                        new Formula.Call(MathematicalFunction.POW, List.of(
                                new Formula.Call(MathematicalFunction.ABS, List.of(base)), arguments.get(1))));
            }
            default -> {
                MathematicalFunction known = FUNCTIONS.get(lower);
                if (known == null) {
                    throw new SpiceExpressionRefusal(offset, "the function '" + written + "' has no jMF counterpart");
                }
                requireArguments(written, arguments, known.arity(), offset);
                yield new Formula.Call(known, arguments);
            }
        };
    }

    private List<Formula> arguments(FunctionNode function) {
        List<Formula> arguments = new ArrayList<>();
        Expression written = function.getArguments();
        if (written instanceof ArgumentsNode list) {
            for (Node child : list.getChildren()) {
                if (child instanceof Expression argument) {
                    arguments.add(translate(argument));
                }
            }
        } else if (written != null) {
            arguments.add(translate(written));
        }
        return arguments;
    }

    private static void requireArguments(String function, List<Formula> arguments, int count, int offset) {
        if (arguments.size() != count) {
            throw new SpiceExpressionRefusal(offset, function + " takes " + count + " argument(s), not "
                    + arguments.size());
        }
    }

    /** {@code c ? a : b}: a truth is a number in SPICE, as in jMF, so the condition is read as written. */
    private static Formula conditional(Formula condition, Formula then, Formula otherwise) {
        return new Formula.Piecewise(List.of(new Formula.Piecewise.Branch(condition, then)), otherwise);
    }

    private static Formula power(Formula base, Formula exponent) {
        return new Formula.Binary(Formula.Operator.POWER, base, exponent);
    }

    /** Where a node stands in the expression's text, when its parser marked it; the whole expression otherwise. */
    private static int offsetOf(Expression expression) {
        if (expression instanceof AbstractExpression marked && marked.getSpan() != null) {
            return marked.getSpan().position();
        }
        if (expression instanceof BinaryOperation binary) {
            return offsetOf(binary.getLeft());
        }
        return -1;
    }
}
