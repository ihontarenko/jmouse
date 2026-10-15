package org.jmouse.formula;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A parsed mathematical expression: the syntax tree every representation of an object is computed from. It is
 * immutable and carries no evaluation strategy; {@link RealCompiler} and {@link ComplexCompiler} turn it into
 * something that evaluates, {@link Differentiation} into another expression.
 *
 * <p>Three kinds of node introduce a name of their own, and inside them that name is <em>bound</em>: the index of
 * an {@link Accumulation}, the variable of an {@link Integral}, and the parameters of a {@link Definition} that an
 * {@link Application} calls. {@link #variables()} answers the free names only — the ones a caller must supply.
 *
 * <p>A truth value is a number: a {@link Comparison} or a {@link Logical} is 1 when it holds and 0 when it does
 * not, and a condition holds when it is not zero.
 */
public sealed interface Formula {

    record Constant(double value) implements Formula {
    }

    /** A named constant (pi, e, tau, phi, inf) kept by name, so a derivative or a printout still says π. */
    record NamedConstant(MathematicalConstant constant) implements Formula {
    }

    /** The imaginary unit; meaningful to the complex compiler only. */
    record ImaginaryUnit() implements Formula {
    }

    record Variable(String name) implements Formula {
    }

    record Negation(Formula operand) implements Formula {
    }

    record Binary(Operator operator, Formula left, Formula right) implements Formula {
    }

    record Call(MathematicalFunction function, List<Formula> arguments) implements Formula {
        public Call {
            arguments = List.copyOf(arguments);
        }
    }

    /** {@code left < right} and its kin: 1 when the relation holds, 0 when it does not. */
    record Comparison(Relation relation, Formula left, Formula right) implements Formula {
    }

    /** {@code left and right}, {@code left or right}: 1 or 0, from the truth of each side. */
    record Logical(Connective connective, Formula left, Formula right) implements Formula {
    }

    /** {@code not operand}: 1 when the operand is 0, otherwise 0. */
    record Not(Formula operand) implements Formula {
    }

    /**
     * The value of the first branch whose condition holds, else {@code otherwise}. Without an otherwise the
     * expression is undefined (NaN) where no branch holds — which is how a formula restricts its own domain.
     */
    record Piecewise(List<Branch> branches, Formula otherwise) implements Formula {
        public Piecewise {
            branches = List.copyOf(branches);
            if (branches.isEmpty()) {
                throw new IllegalArgumentException("A piecewise expression needs at least one branch.");
            }
        }

        public record Branch(Formula condition, Formula value) {
            public Branch {
                Objects.requireNonNull(condition, "condition");
                Objects.requireNonNull(value, "value");
            }
        }
    }

    /**
     * A sum or a product of {@code body} over the integers {@code index} takes from {@code from} to {@code to}
     * inclusive. The bounds may depend on free names; an infinite upper bound is a series, summed to convergence.
     */
    record Accumulation(Accumulator accumulator, String index, Formula from, Formula to, Formula body)
            implements Formula {
        public Accumulation {
            Objects.requireNonNull(accumulator, "accumulator");
            Objects.requireNonNull(index, "index");
        }
    }

    /** The definite integral of {@code body} over {@code variable} from {@code lower} to {@code upper}. */
    record Integral(String variable, Formula lower, Formula upper, Formula body) implements Formula {
        public Integral {
            Objects.requireNonNull(variable, "variable");
        }
    }

    /** A call of a function the formula's author defined: {@code f(x) = x^2 - 1}, then {@code f(a + 1)}. */
    record Application(Definition definition, List<Formula> arguments) implements Formula {
        public Application {
            arguments = List.copyOf(arguments);
            if (arguments.size() != definition.parameters().size()) {
                throw new IllegalArgumentException(definition.name() + " takes " + definition.parameters().size()
                        + " argument(s), not " + arguments.size() + ".");
            }
        }
    }

    /**
     * {@code left = right}: an equation. As a value it is its residual, {@code left - right}, whose zero set is
     * the equation's solution set — which is what an implicit curve or surface draws.
     */
    record Equation(Formula left, Formula right) implements Formula {
    }

    /**
     * A call the formula does not evaluate itself but leaves to the program that reads it: {@code V(out, GND)},
     * {@code I(R1)} or {@code ddt(q)} for a circuit solver. A language reads one only for a name its host declared
     * ({@code FormulaLanguage.withHostCalls}); a bare name among the arguments is a {@link Variable} the host reads as
     * a word — a node, a part — not as a value.
     *
     * <p>Nothing compiles or differentiates a host call. The host replaces every one, through
     * {@link Substitution#replaceHostCalls}, before it does either: a voltage with a {@link Slot}, a rate with
     * another, a charge with the expression the host integrates itself.</p>
     */
    record HostCall(String name, List<Formula> arguments) implements Formula {
        public HostCall {
            Objects.requireNonNull(name, "name");
            arguments = List.copyOf(arguments);
        }
    }

    /**
     * A value the host supplies at position {@code index} of the argument array, with no name in the formula's
     * namespace: what a {@link HostCall} becomes once its host binds it. It cannot collide with anything an author
     * names, because it is not a name; {@code label} is only for printing and for messages.
     */
    record Slot(int index, String label) implements Formula {
        public Slot {
            if (index < 0) {
                throw new IllegalArgumentException("A slot's index is not negative.");
            }
            Objects.requireNonNull(label, "label");
        }
    }

    /**
     * A function the author named. Its body sees its parameters and the names free where it was defined — never
     * the names bound around the place it is called, so {@code f} means the same thing everywhere.
     */
    record Definition(String name, List<String> parameters, Formula body) {
        public Definition {
            parameters = List.copyOf(parameters);
            if (Set.copyOf(parameters).size() != parameters.size()) {
                throw new IllegalArgumentException(name + " names one parameter twice.");
            }
        }
    }

    enum Operator {
        ADD("+", 1),
        SUBTRACT("-", 1),
        MULTIPLY("*", 2),
        DIVIDE("/", 2),
        POWER("^", 4);

        private final String symbol;
        private final int precedence;

        Operator(String symbol, int precedence) {
            this.symbol = symbol;
            this.precedence = precedence;
        }

        public String symbol() {
            return symbol;
        }

        public int precedence() {
            return precedence;
        }
    }

    enum Relation {
        LESS("<", "<"),
        LESS_OR_EQUAL("<=", "\\le"),
        GREATER(">", ">"),
        GREATER_OR_EQUAL(">=", "\\ge"),
        EQUAL("==", "="),
        NOT_EQUAL("!=", "\\ne");

        private final String symbol;
        private final String latex;

        Relation(String symbol, String latex) {
            this.symbol = symbol;
            this.latex = latex;
        }

        public String symbol() {
            return symbol;
        }

        public String latex() {
            return latex;
        }

        public boolean holds(double left, double right) {
            return switch (this) {
                case LESS -> left < right;
                case LESS_OR_EQUAL -> left <= right;
                case GREATER -> left > right;
                case GREATER_OR_EQUAL -> left >= right;
                case EQUAL -> left == right;
                case NOT_EQUAL -> left != right;
            };
        }
    }

    enum Connective {
        AND("and", "\\land"),
        OR("or", "\\lor");

        private final String symbol;
        private final String latex;

        Connective(String symbol, String latex) {
            this.symbol = symbol;
            this.latex = latex;
        }

        public String symbol() {
            return symbol;
        }

        public String latex() {
            return latex;
        }
    }

    enum Accumulator {
        SUM("sum", "\\sum"),
        PRODUCT("product", "\\prod");

        private final String symbol;
        private final String latex;

        Accumulator(String symbol, String latex) {
            this.symbol = symbol;
            this.latex = latex;
        }

        public String symbol() {
            return symbol;
        }

        public String latex() {
            return latex;
        }
    }

    /** Every free name the expression mentions, in the order they first appear. */
    default Set<String> variables() {
        Set<String> found = new LinkedHashSet<>();
        collectFreeVariables(this, Set.of(), found);
        return found;
    }

    /** Whether {@code name} occurs free: whether the value of this expression can depend on it. */
    default boolean dependsOn(String name) {
        return variables().contains(name);
    }

    default boolean mentionsImaginaryUnit() {
        return switch (this) {
            case ImaginaryUnit unit -> true;
            case Constant constant -> false;
            case NamedConstant constant -> false;
            case Variable variable -> false;
            case Slot slot -> false;
            case HostCall hostCall -> hostCall.arguments().stream().anyMatch(Formula::mentionsImaginaryUnit);
            case Negation negation -> negation.operand().mentionsImaginaryUnit();
            case Not not -> not.operand().mentionsImaginaryUnit();
            case Binary binary -> binary.left().mentionsImaginaryUnit() || binary.right().mentionsImaginaryUnit();
            case Comparison comparison -> comparison.left().mentionsImaginaryUnit() || comparison.right().mentionsImaginaryUnit();
            case Logical logical -> logical.left().mentionsImaginaryUnit() || logical.right().mentionsImaginaryUnit();
            case Equation equation -> equation.left().mentionsImaginaryUnit() || equation.right().mentionsImaginaryUnit();
            case Call call -> call.arguments().stream().anyMatch(Formula::mentionsImaginaryUnit);
            case Piecewise piecewise -> piecewise.branches().stream().anyMatch(branch ->
                    branch.condition().mentionsImaginaryUnit() || branch.value().mentionsImaginaryUnit())
                    || (piecewise.otherwise() != null && piecewise.otherwise().mentionsImaginaryUnit());
            case Accumulation accumulation -> accumulation.from().mentionsImaginaryUnit()
                    || accumulation.to().mentionsImaginaryUnit() || accumulation.body().mentionsImaginaryUnit();
            case Integral integral -> integral.lower().mentionsImaginaryUnit() || integral.upper().mentionsImaginaryUnit()
                    || integral.body().mentionsImaginaryUnit();
            case Application application -> application.definition().body().mentionsImaginaryUnit()
                    || application.arguments().stream().anyMatch(Formula::mentionsImaginaryUnit);
        };
    }

    private static void collectFreeVariables(Formula expression, Set<String> bound, Set<String> found) {
        switch (expression) {
            case Variable variable -> {
                if (!bound.contains(variable.name())) {
                    found.add(variable.name());
                }
            }
            case Negation negation -> collectFreeVariables(negation.operand(), bound, found);
            case Not not -> collectFreeVariables(not.operand(), bound, found);
            case Binary binary -> {
                collectFreeVariables(binary.left(), bound, found);
                collectFreeVariables(binary.right(), bound, found);
            }
            case Comparison comparison -> {
                collectFreeVariables(comparison.left(), bound, found);
                collectFreeVariables(comparison.right(), bound, found);
            }
            case Logical logical -> {
                collectFreeVariables(logical.left(), bound, found);
                collectFreeVariables(logical.right(), bound, found);
            }
            case Equation equation -> {
                collectFreeVariables(equation.left(), bound, found);
                collectFreeVariables(equation.right(), bound, found);
            }
            case Call call -> call.arguments().forEach(argument -> collectFreeVariables(argument, bound, found));
            case Piecewise piecewise -> {
                for (Piecewise.Branch branch : piecewise.branches()) {
                    collectFreeVariables(branch.condition(), bound, found);
                    collectFreeVariables(branch.value(), bound, found);
                }
                if (piecewise.otherwise() != null) {
                    collectFreeVariables(piecewise.otherwise(), bound, found);
                }
            }
            case Accumulation accumulation -> {
                collectFreeVariables(accumulation.from(), bound, found);
                collectFreeVariables(accumulation.to(), bound, found);
                collectFreeVariables(accumulation.body(), including(bound, accumulation.index()), found);
            }
            case Integral integral -> {
                collectFreeVariables(integral.lower(), bound, found);
                collectFreeVariables(integral.upper(), bound, found);
                collectFreeVariables(integral.body(), including(bound, integral.variable()), found);
            }
            case Application application -> {
                // The body is lexically scoped: what it leaves free is free where it was defined, the top level.
                Definition definition = application.definition();
                collectFreeVariables(definition.body(), Set.copyOf(definition.parameters()), found);
                application.arguments().forEach(argument -> collectFreeVariables(argument, bound, found));
            }
            case HostCall hostCall -> hostCall.arguments().forEach(argument -> collectFreeVariables(argument, bound, found));
            case Slot slot -> {
            }
            case Constant constant -> {
            }
            case NamedConstant constant -> {
            }
            case ImaginaryUnit unit -> {
            }
        }
    }

    /** Every slot the expression reads, by index, in the order they first appear. */
    default Set<Integer> slots() {
        Set<Integer> found = new LinkedHashSet<>();
        collectSlots(this, found);
        return found;
    }

    /** Whether the value of this expression can depend on the slot at {@code index}. */
    default boolean dependsOnSlot(int index) {
        return slots().contains(index);
    }

    /**
     * Roughly how many elementary evaluations one evaluation of this expression costs: 1 for arithmetic and calls; an
     * integral its body times what a quadrature usually takes; a sum or product its body times its terms — counted when
     * the bounds are numbers, a hundred when they are not; a defined function its body. What an estimate of a
     * computation's cost multiplies by, so an object whose curve is itself an integral is not taken for a cheap one.
     */
    default long evaluationWeight() {
        return weightOf(this);
    }

    private static long weightOf(Formula expression) {
        // An optional part of a node — an integral without bounds, a piecewise without an otherwise — weighs nothing.
        if (expression == null) {
            return 1;
        }
        return switch (expression) {
            case Integral integral -> saturated(Quadrature.TYPICAL_EVALUATIONS, weightOf(integral.body()))
                    + Math.max(weightOf(integral.lower()), weightOf(integral.upper()));
            case Accumulation accumulation -> saturated(termsOf(accumulation), weightOf(accumulation.body()));
            case Application application -> Math.max(weightOf(application.definition().body()),
                    application.arguments().stream().mapToLong(Formula::weightOf).max().orElse(1));
            case Negation negation -> weightOf(negation.operand());
            case Not not -> weightOf(not.operand());
            case Binary binary -> Math.max(weightOf(binary.left()), weightOf(binary.right()));
            case Comparison comparison -> Math.max(weightOf(comparison.left()), weightOf(comparison.right()));
            case Logical logical -> Math.max(weightOf(logical.left()), weightOf(logical.right()));
            case Equation equation -> Math.max(weightOf(equation.left()), weightOf(equation.right()));
            case Call call -> call.arguments().stream().mapToLong(Formula::weightOf).max().orElse(1);
            case HostCall hostCall -> hostCall.arguments().stream().mapToLong(Formula::weightOf).max().orElse(1);
            case Piecewise piecewise -> Math.max(weightOf(piecewise.otherwise()), piecewise.branches().stream()
                    .mapToLong(branch -> Math.max(weightOf(branch.condition()), weightOf(branch.value()))).max().orElse(1));
            case Constant constant -> 1;
            case NamedConstant constant -> 1;
            case ImaginaryUnit unit -> 1;
            case Variable variable -> 1;
            case Slot slot -> 1;
        };
    }

    private static long termsOf(Accumulation accumulation) {
        if (accumulation.from() instanceof Constant from && accumulation.to() instanceof Constant to) {
            return Math.max(1, (long) Math.min(1e9, Math.abs(to.value() - from.value()) + 1));
        }
        return 100;
    }

    private static long saturated(long factor, long weight) {
        return weight > Long.MAX_VALUE / factor ? Long.MAX_VALUE : factor * weight;
    }

    /** Whether a host call is still unbound somewhere in the expression, the bodies it calls included. */
    default boolean mentionsHostCall() {
        return Substitution.findsHostCall(this);
    }

    private static void collectSlots(Formula expression, Set<Integer> found) {
        switch (expression) {
            case Slot slot -> found.add(slot.index());
            case HostCall hostCall -> hostCall.arguments().forEach(argument -> collectSlots(argument, found));
            case Negation negation -> collectSlots(negation.operand(), found);
            case Not not -> collectSlots(not.operand(), found);
            case Binary binary -> {
                collectSlots(binary.left(), found);
                collectSlots(binary.right(), found);
            }
            case Comparison comparison -> {
                collectSlots(comparison.left(), found);
                collectSlots(comparison.right(), found);
            }
            case Logical logical -> {
                collectSlots(logical.left(), found);
                collectSlots(logical.right(), found);
            }
            case Equation equation -> {
                collectSlots(equation.left(), found);
                collectSlots(equation.right(), found);
            }
            case Call call -> call.arguments().forEach(argument -> collectSlots(argument, found));
            case Piecewise piecewise -> {
                for (Piecewise.Branch branch : piecewise.branches()) {
                    collectSlots(branch.condition(), found);
                    collectSlots(branch.value(), found);
                }
                if (piecewise.otherwise() != null) {
                    collectSlots(piecewise.otherwise(), found);
                }
            }
            case Accumulation accumulation -> {
                collectSlots(accumulation.from(), found);
                collectSlots(accumulation.to(), found);
                collectSlots(accumulation.body(), found);
            }
            case Integral integral -> {
                collectSlots(integral.lower(), found);
                collectSlots(integral.upper(), found);
                collectSlots(integral.body(), found);
            }
            case Application application -> {
                collectSlots(application.definition().body(), found);
                application.arguments().forEach(argument -> collectSlots(argument, found));
            }
            case Variable variable -> {
            }
            case Constant constant -> {
            }
            case NamedConstant constant -> {
            }
            case ImaginaryUnit unit -> {
            }
        }
    }

    private static Set<String> including(Set<String> bound, String name) {
        Set<String> extended = new LinkedHashSet<>(bound);
        extended.add(name);
        return extended;
    }

    static Formula constant(double value) {
        return new Constant(value);
    }

    static Formula variable(String name) {
        return new Variable(name);
    }

    static Formula add(Formula left, Formula right) {
        return new Binary(Operator.ADD, left, right);
    }

    static Formula subtract(Formula left, Formula right) {
        return new Binary(Operator.SUBTRACT, left, right);
    }

    static Formula multiply(Formula left, Formula right) {
        return new Binary(Operator.MULTIPLY, left, right);
    }

    static Formula divide(Formula left, Formula right) {
        return new Binary(Operator.DIVIDE, left, right);
    }

    static Formula power(Formula left, Formula right) {
        return new Binary(Operator.POWER, left, right);
    }

    static Formula negate(Formula operand) {
        return new Negation(operand);
    }

    static Formula call(MathematicalFunction function, Formula... arguments) {
        return new Call(function, List.of(arguments));
    }
}
