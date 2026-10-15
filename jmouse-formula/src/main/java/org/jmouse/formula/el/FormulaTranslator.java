package org.jmouse.formula.el;

import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.ComparisonOperator;
import org.jmouse.el.extension.operator.LogicalOperator;
import org.jmouse.el.extension.operator.MathematicOperator;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.ArgumentsNode;
import org.jmouse.el.node.expression.BinaryOperation;
import org.jmouse.el.node.expression.FunctionNode;
import org.jmouse.el.node.expression.LiteralNode;
import org.jmouse.el.node.expression.PropertyNode;
import org.jmouse.el.node.expression.TernaryNode;
import org.jmouse.formula.Differentiation;
import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.FormulaPrinter;
import org.jmouse.formula.MathematicalConstant;
import org.jmouse.formula.MathematicalFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the jME syntax tree of a formula into jMF's own immutable {@link Formula} tree, which is what is compiled,
 * differentiated and printed. Whatever jME can parse but a formula cannot mean — a string, a filter, a property
 * path — is refused here with its name.
 *
 * <p>Some calls are not functions but forms with a name of their own in them, read here rather than looked up:
 * {@code if}, {@code piecewise}, {@code sum}, {@code product}, {@code integral} and {@code diff}. A name the
 * author defined earlier in the document — {@code f(x) = x^2} — is called through {@link Formula.Application}.
 */
public final class FormulaTranslator {

    /** Names a formula reads as forms; they cannot be defined, and cannot be bound as an index. */
    public static final Set<String> FORMS = Set.of("if", "piecewise", "sum", "product", "integral", "integrate", "diff",
            "derivative");

    private final Map<String, Formula.Definition> definitions;
    private final Set<String> hostCalls;
    private final String source;

    public FormulaTranslator(Map<String, Formula.Definition> definitions, String source) {
        this(definitions, Set.of(), source);
    }

    /** @param hostCalls names read as {@link Formula.HostCall}s, which the formula's host binds */
    public FormulaTranslator(Map<String, Formula.Definition> definitions, Set<String> hostCalls, String source) {
        this.definitions = definitions;
        this.hostCalls = Set.copyOf(hostCalls);
        this.source = source;
    }

    /** A formula with no definitions around it. */
    public static Formula translate(Expression expression, String source) {
        return new FormulaTranslator(Map.of(), source).translate(expression);
    }

    public Formula translate(Expression expression) {
        return switch (expression) {
            case NegationNode negation -> new Formula.Negation(translate(negation.getOperand()));
            case NotNode not -> new Formula.Not(translate(not.getOperand()));
            case FactorialNode factorial -> Formula.call(MathematicalFunction.FACTORIAL, translate(factorial.getOperand()));
            case BinaryOperation binary -> binary(binary);
            case TernaryNode ternary -> new Formula.Piecewise(
                    List.of(new Formula.Piecewise.Branch(translate(ternary.getCondition()), translate(ternary.getThenBranch()))),
                    translate(ternary.getElseBranch()));
            case LiteralNode<?> literal -> literal(literal);
            case PropertyNode property -> name(property.getPath());
            case FunctionNode function -> function(function);
            default -> throw refusal("'" + expression.toSource() + "' is not part of formula notation.", null);
        };
    }

    private Formula binary(BinaryOperation binary) {
        Formula left = translate(binary.getLeft());
        Formula right = translate(binary.getRight());
        Operator operator = binary.getOperator();
        if (operator == FormulaOperator.POWER || operator == MathematicOperator.EXPONENTIAL) {
            return Formula.power(left, right);
        }
        if (operator == FormulaOperator.DIVIDE) {
            return Formula.divide(left, right);
        }
        if (operator instanceof MathematicOperator mathematic) {
            return switch (mathematic) {
                case PLUS -> Formula.add(left, right);
                case SUBTRACT -> Formula.subtract(left, right);
                case MULTIPLY -> Formula.multiply(left, right);
                case DIVIDE -> Formula.divide(left, right);
                case MODULUS -> Formula.call(MathematicalFunction.MOD, left, right);
                case EXPONENTIAL -> Formula.power(left, right);
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
        throw refusal("'" + operator.getSpelling() + "' is not an operator of formulas.", null);
    }

    private Formula literal(LiteralNode<?> literal) {
        if (literal.getValue() instanceof Number number) {
            return new Formula.Constant(number.doubleValue());
        }
        throw refusal("'" + literal.toSource() + "' is not a number.", null);
    }

    private Formula name(String path) {
        if (path.contains(".")) {
            throw refusal("'" + path + "' is a property path; a formula names symbols only.", path);
        }
        if (path.equals("i")) {
            return new Formula.ImaginaryUnit();
        }
        Formula.Definition defined = definitions.get(path);
        if (defined != null) {
            if (!defined.parameters().isEmpty()) {
                throw refusal(path + " is a function of " + String.join(", ", defined.parameters())
                        + "; call it with arguments.", path);
            }
            return new Formula.Application(defined, List.of());
        }
        return MathematicalConstant.bySymbol(path).<Formula>map(Formula.NamedConstant::new)
                .orElseGet(() -> new Formula.Variable(path));
    }

    private Formula function(FunctionNode function) {
        String name = function.getName();
        List<Formula> arguments = arguments(function);
        return switch (name) {
            case "if" -> conditional(arguments);
            case "piecewise" -> piecewise(arguments);
            case "sum" -> accumulation(Formula.Accumulator.SUM, arguments);
            case "product" -> accumulation(Formula.Accumulator.PRODUCT, arguments);
            case "integral", "integrate" -> integral(arguments);
            case "diff", "derivative" -> derivative(arguments);
            default -> call(name, arguments);
        };
    }

    private Formula call(String name, List<Formula> arguments) {
        if (hostCalls.contains(name)) {
            return new Formula.HostCall(name, arguments);
        }
        Formula.Definition defined = definitions.get(name);
        if (defined != null) {
            if (arguments.size() != defined.parameters().size()) {
                throw refusal(name + " takes " + defined.parameters().size() + " argument(s), not " + arguments.size()
                        + ".", name);
            }
            return new Formula.Application(defined, arguments);
        }
        Optional<MathematicalFunction> known = MathematicalFunction.bySymbol(name);
        if (known.isEmpty()) {
            // jME reads a name followed by "(" as a call. In a formula `rho (x - 1)` is a product, and a typo such
            // as `sni(x)` is still refused — by the symbol table, which knows no `sni`.
            if (arguments.size() != 1) {
                throw refusal("'" + name + "' is not a function.", name);
            }
            return Formula.multiply(name(name), arguments.getFirst());
        }
        MathematicalFunction mathematical = known.get();
        if (arguments.size() != mathematical.arity()) {
            throw refusal(mathematical.symbol() + " takes " + mathematical.arity() + " argument(s), not "
                    + arguments.size() + ".", name);
        }
        return new Formula.Call(mathematical, arguments);
    }

    /** {@code if(condition, value)} or {@code if(condition, value, otherwise)}. */
    private Formula conditional(List<Formula> arguments) {
        if (arguments.size() != 2 && arguments.size() != 3) {
            throw refusal("if takes a condition, a value and optionally what it is otherwise.", "if");
        }
        return new Formula.Piecewise(List.of(new Formula.Piecewise.Branch(arguments.get(0), arguments.get(1))),
                arguments.size() == 3 ? arguments.get(2) : null);
    }

    /** {@code piecewise(c₁, v₁, c₂, v₂, …)}, with an odd last argument as what it is otherwise. */
    private Formula piecewise(List<Formula> arguments) {
        if (arguments.size() < 2) {
            throw refusal("piecewise takes pairs of a condition and a value, then optionally what it is otherwise.",
                    "piecewise");
        }
        List<Formula.Piecewise.Branch> branches = new ArrayList<>();
        for (int index = 0; index + 1 < arguments.size(); index += 2) {
            branches.add(new Formula.Piecewise.Branch(arguments.get(index), arguments.get(index + 1)));
        }
        Formula otherwise = arguments.size() % 2 == 1 ? arguments.getLast() : null;
        return new Formula.Piecewise(branches, otherwise);
    }

    /** {@code sum(k, from, to, body)}; {@code to} may be {@code inf}. */
    private Formula accumulation(Formula.Accumulator accumulator, List<Formula> arguments) {
        if (arguments.size() != 4) {
            throw refusal(accumulator.symbol() + " takes an index, a first and a last value, and a term: "
                    + accumulator.symbol() + "(k, 1, n, 1/k^2).", accumulator.symbol());
        }
        String index = boundName(arguments.getFirst(), accumulator.symbol());
        return new Formula.Accumulation(accumulator, index, arguments.get(1), arguments.get(2), arguments.get(3));
    }

    /** {@code integral(t, lower, upper, body)}; either bound may be {@code inf} or {@code -inf}. */
    private Formula integral(List<Formula> arguments) {
        if (arguments.size() != 4) {
            throw refusal("integral takes a variable, two bounds and an integrand: integral(t, 0, x, sin(t)/t).",
                    "integral");
        }
        String variable = boundName(arguments.getFirst(), "integral");
        return new Formula.Integral(variable, arguments.get(1), arguments.get(2), arguments.get(3));
    }

    /** {@code diff(f, x)} or {@code diff(f, x, n)}: the exact derivative, taken here, once. */
    private Formula derivative(List<Formula> arguments) {
        if (arguments.size() != 2 && arguments.size() != 3) {
            throw refusal("diff takes an expression, a variable and optionally an order: diff(sin(x) x, x, 2).", "diff");
        }
        String variable = boundName(arguments.get(1), "diff");
        int order = 1;
        if (arguments.size() == 3) {
            if (!(arguments.get(2) instanceof Formula.Constant constant) || constant.value() != Math.rint(constant.value())
                    || constant.value() < 1 || constant.value() > 8) {
                throw refusal("The order of a derivative is a whole number from 1 to 8.", "diff");
            }
            order = (int) constant.value();
        }
        Formula result = arguments.getFirst();
        for (int step = 0; step < order; step++) {
            result = Differentiation.derivative(result, variable);
        }
        return result;
    }

    private String boundName(Formula argument, String form) {
        if (argument instanceof Formula.Variable variable) {
            return variable.name();
        }
        throw refusal("The first argument of " + form + " names the variable it runs over, such as k or t; "
                + "'" + FormulaPrinter.text(argument) + "'" + " is not a free name.", form);
    }

    private List<Formula> arguments(FunctionNode function) {
        List<Formula> arguments = new ArrayList<>();
        Expression argumentNode = function.getArguments();
        if (argumentNode instanceof ArgumentsNode list) {
            for (Node child : list.getChildren()) {
                if (child instanceof Expression argument) {
                    arguments.add(translate(argument));
                }
            }
        } else if (argumentNode != null) {
            arguments.add(translate(argumentNode));
        }
        return arguments;
    }

    private FormulaException refusal(String message, String near) {
        return new FormulaException(message, source,
                near == null || source == null ? -1 : Math.max(-1, source.indexOf(near)));
    }
}
