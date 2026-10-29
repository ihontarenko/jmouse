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
import org.jmouse.el.node.expression.unary.NegativeUnaryOperation;
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
    /** {@code dt} → the host call {@code d(f)/dt} is read as; see {@link FormulaLanguage#withRate}. */
    private final Map<String, String> rates;
    private final String source;

    public FormulaTranslator(Map<String, Formula.Definition> definitions, String source) {
        this(definitions, Set.of(), Map.of(), source);
    }

    /**
     * @param hostCalls names read as {@link Formula.HostCall}s, which the formula's host binds
     * @param rates     each {@code d<variable>} a rate is written over, with the host call {@code d(f)/d<variable>}
     *                  is read as
     */
    public FormulaTranslator(Map<String, Formula.Definition> definitions, Set<String> hostCalls,
                             Map<String, String> rates, String source) {
        this.definitions = definitions;
        this.hostCalls = Set.copyOf(hostCalls);
        this.rates = Map.copyOf(rates);
        this.source = source;
    }

    /** A formula with no definitions around it. */
    public static Formula translate(Expression expression, String source) {
        return new FormulaTranslator(Map.of(), source).translate(expression);
    }

    public Formula translate(Expression expression) {
        return switch (expression) {
            case NegativeUnaryOperation negation -> new Formula.Negation(translate(negation.getOperand()));
            case NotNode not -> new Formula.Not(translate(not.getOperand()));
            case FactorialNode factorial -> Formula.call(MathematicalFunction.FACTORIAL, translate(factorial.getOperand()));
            case BinaryOperation binary -> binary(binary);
            case TernaryNode ternary -> new Formula.Piecewise(
                    List.of(new Formula.Piecewise.Branch(translate(ternary.getCondition()), translate(ternary.getThenBranch()))),
                    translate(ternary.getElseBranch()));
            case LiteralNode<?> literal -> literal(literal);
            case PropertyNode property -> name(property.getPath());
            case FunctionNode function -> function(function);
            case TupleNode tuple -> throw refusal("'" + tuple.toSource() + "' is a tuple — a point, a curve's components —, not a "
                    + "number: write one of its components where a number is wanted.", null);
            default -> throw refusal("'" + expression.toSource() + "' is not part of formula notation.", null);
        };
    }

    private Formula binary(BinaryOperation binary) {
        Formula rate = rate(binary);
        if (rate != null) {
            return rate;
        }
        Formula left = translate(binary.getLeft());
        Formula right = translate(binary.getRight());
        Operator operator = binary.getOperator();
        if (operator == FormulaOperator.PAIR) {
            throw refusal("'" + binary.toSource() + "' is a pair: it stands among a call's arguments, 'curve(x, 0 -> 0, 1 -> 1)'.", null);
        }
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

    /**
     * {@code anode.current}: a word and a member its host declared as a call — the same thing as {@code current(anode)},
     * written the way a reader thinks of it, as something the word has. Any other path is refused: a formula names
     * symbols, not properties.
     */
    private Formula member(String path) {
        int dot = path.indexOf('.');
        String owner = path.substring(0, dot);
        String member = path.substring(dot + 1);
        if (member.contains(".") || owner.isEmpty() || !hostCalls.contains(member)) {
            throw refusal("'" + path + "' is a property path; a formula names symbols only"
                    + (hostCalls.isEmpty() ? "." : ", and a word's members here are " + String.join(", ",
                    hostCalls.stream().sorted().toList()) + "."), path);
        }
        return new Formula.HostCall(member, List.of(new Formula.Variable(owner)));
    }

    private Formula literal(LiteralNode<?> literal) {
        if (literal.getValue() instanceof Number number) {
            return new Formula.Constant(number.doubleValue());
        }
        throw refusal("'" + literal.toSource() + "' is not a number.", null);
    }

    /**
     * {@code d(f)/dt}, when the language declared a rate over {@code t}: the division whose left is the call
     * {@code d(f)} and whose right is the name {@code dt}, read as one node — the host call the rate is bound by. Null
     * for any other division.
     */
    private Formula rate(BinaryOperation binary) {
        if (rates.isEmpty() || !isDivision(binary.getOperator())
                || !(binary.getRight() instanceof PropertyNode over) || !rates.containsKey(over.getPath())) {
            return null;
        }
        return over(binary.getLeft(), over.getPath());
    }

    /**
     * What stands over {@code dt}, with its rate read: {@code d(q)} itself, or a product or a negation ending in it.
     * Division binds left to right, so {@code polarity * d(q)/dt} reaches here as {@code (polarity * d(q)) / dt} —
     * the same quantity as {@code polarity · dq/dt}, and read as that. Null when no {@code d(…)} ends it.
     */
    private Formula over(Expression above, String variable) {
        if (above instanceof FunctionNode written && FormulaLanguage.RATE_OPERATOR.equals(written.getName())
                && !definitions.containsKey(written.getName())) {
            List<Formula> arguments = arguments(written);
            if (arguments.size() != 1) {
                throw refusal("A rate is the change of one expression: d(q)/" + variable + ".", "d");
            }
            return new Formula.HostCall(rates.get(variable), arguments);
        }
        if (above instanceof NegativeUnaryOperation negation) {
            Formula operand = over(negation.getOperand(), variable);
            return operand == null ? null : new Formula.Negation(operand);
        }
        if (above instanceof BinaryOperation product && product.getOperator() == MathematicOperator.MULTIPLY) {
            Formula last = over(product.getRight(), variable);
            return last == null ? null : Formula.multiply(translate(product.getLeft()), last);
        }
        return null;
    }

    private static boolean isDivision(Operator operator) {
        return operator == FormulaOperator.DIVIDE || operator == MathematicOperator.DIVIDE;
    }

    private Formula name(String path) {
        if (path.contains(".")) {
            return member(path);
        }
        if (rates.containsKey(path)) {
            throw refusal("'" + path + "' stands only under a rate: d(q)/" + path + ".", path);
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
        if (FormulaLanguage.RATE_OPERATOR.equals(name) && !rates.isEmpty() && !definitions.containsKey(name)) {
            throw refusal("A rate is written over its variable: d(q)/"
                    + rates.keySet().iterator().next() + ", and d(…) stands nowhere else.", name);
        }
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

    /** An argument, or — a pair {@code a -> b} — the two values it pairs, in order. */
    private void addArgument(List<Formula> arguments, Expression argument) {
        if (argument instanceof BinaryOperation pair && pair.getOperator() == FormulaOperator.PAIR) {
            addArgument(arguments, pair.getLeft());
            addArgument(arguments, pair.getRight());
            return;
        }
        arguments.add(translate(argument));
    }

    private List<Formula> arguments(FunctionNode function) {
        List<Formula> arguments = new ArrayList<>();
        Expression argumentNode = function.getArguments();
        if (argumentNode instanceof ArgumentsNode list) {
            for (Node child : list.getChildren()) {
                if (child instanceof Expression argument) {
                    addArgument(arguments, argument);
                }
            }
        } else if (argumentNode != null) {
            addArgument(arguments, argumentNode);
        }
        return arguments;
    }

    private FormulaException refusal(String message, String near) {
        return new FormulaException(message, source,
                near == null || source == null ? -1 : Math.max(-1, source.indexOf(near)));
    }
}
