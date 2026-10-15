package org.jmouse.formula;

import java.util.ArrayList;
import java.util.List;

/**
 * Local algebraic tidying: folds constants, drops additive zeros and multiplicative ones, collapses products with
 * zero. Enough to make a printed derivative readable; it does not pretend to be a computer algebra system.
 */
public final class Simplification {

    private Simplification() {
    }

    public static Formula simplify(Formula expression) {
        Formula previous;
        Formula current = expression;
        int passes = 0;
        do {
            previous = current;
            current = pass(current);
            passes++;
        } while (!current.equals(previous) && passes < 12);
        return current;
    }

    private static Formula pass(Formula expression) {
        return switch (expression) {
            case Formula.Constant constant -> constant;
            case Formula.NamedConstant named -> named;
            case Formula.ImaginaryUnit unit -> unit;
            case Formula.Variable variable -> variable;
            case Formula.Slot slot -> slot;
            case Formula.HostCall hostCall -> new Formula.HostCall(hostCall.name(), hostCall.arguments().stream().map(Simplification::pass).toList());
            case Formula.Negation negation -> negation(pass(negation.operand()));
            case Formula.Binary binary -> binary(binary.operator(), pass(binary.left()), pass(binary.right()));
            case Formula.Call functionCall -> new Formula.Call(functionCall.function(),
                    functionCall.arguments().stream().map(Simplification::pass).toList());
            case Formula.Comparison comparison -> folded(new Formula.Comparison(comparison.relation(),
                    pass(comparison.left()), pass(comparison.right())));
            case Formula.Logical logical -> folded(new Formula.Logical(logical.connective(), pass(logical.left()),
                    pass(logical.right())));
            case Formula.Not not -> folded(new Formula.Not(pass(not.operand())));
            case Formula.Equation equation -> new Formula.Equation(pass(equation.left()), pass(equation.right()));
            case Formula.Piecewise piecewise -> piecewise(piecewise);
            case Formula.Accumulation accumulation -> new Formula.Accumulation(accumulation.accumulator(),
                    accumulation.index(), pass(accumulation.from()), pass(accumulation.to()), pass(accumulation.body()));
            case Formula.Integral integral -> new Formula.Integral(integral.variable(), pass(integral.lower()),
                    pass(integral.upper()), pass(integral.body()));
            case Formula.Application application -> new Formula.Application(application.definition(),
                    application.arguments().stream().map(Simplification::pass).toList());
        };
    }

    /** A condition over constants is its truth value. */
    private static Formula folded(Formula condition) {
        if (RealCompiler.isConstant(condition)) {
            double value = RealCompiler.fold(condition);
            if (!Double.isNaN(value)) {
                return new Formula.Constant(value);
            }
        }
        return condition;
    }

    /** Branches whose condition is constantly false go; a constantly true one ends the list. */
    private static Formula piecewise(Formula.Piecewise piecewise) {
        List<Formula.Piecewise.Branch> kept = new ArrayList<>();
        for (Formula.Piecewise.Branch branch : piecewise.branches()) {
            Formula condition = pass(branch.condition());
            Formula value = pass(branch.value());
            if (isValue(condition, 0)) {
                continue;
            }
            if (condition instanceof Formula.Constant) {
                if (kept.isEmpty()) {
                    return value;
                }
                return new Formula.Piecewise(kept, value);
            }
            kept.add(new Formula.Piecewise.Branch(condition, value));
        }
        Formula otherwise = piecewise.otherwise() == null ? null : pass(piecewise.otherwise());
        if (kept.isEmpty()) {
            return otherwise == null ? new Formula.Constant(Double.NaN) : otherwise;
        }
        return new Formula.Piecewise(kept, otherwise);
    }

    private static Formula negation(Formula operand) {
        if (operand instanceof Formula.Constant constant) {
            return new Formula.Constant(-constant.value());
        }
        if (operand instanceof Formula.Negation inner) {
            return inner.operand();
        }
        return new Formula.Negation(operand);
    }

    private static Formula binary(Formula.Operator operator, Formula left, Formula right) {
        if (left instanceof Formula.Constant first && right instanceof Formula.Constant second) {
            double folded = RealCompiler.fold(new Formula.Binary(operator, first, second));
            if (Double.isFinite(folded)) {
                return new Formula.Constant(folded);
            }
        }
        return switch (operator) {
            case ADD -> {
                if (isValue(left, 0)) {
                    yield right;
                }
                if (isValue(right, 0)) {
                    yield left;
                }
                if (right instanceof Formula.Negation negated) {
                    yield new Formula.Binary(Formula.Operator.SUBTRACT, left, negated.operand());
                }
                yield new Formula.Binary(operator, left, right);
            }
            case SUBTRACT -> {
                if (isValue(right, 0)) {
                    yield left;
                }
                if (isValue(left, 0)) {
                    yield negation(right);
                }
                if (left.equals(right)) {
                    yield new Formula.Constant(0);
                }
                yield new Formula.Binary(operator, left, right);
            }
            case MULTIPLY -> {
                if (isValue(left, 0) || isValue(right, 0)) {
                    yield new Formula.Constant(0);
                }
                if (isValue(left, 1)) {
                    yield right;
                }
                if (isValue(right, 1)) {
                    yield left;
                }
                if (isValue(left, -1)) {
                    yield negation(right);
                }
                if (isValue(right, -1)) {
                    yield negation(left);
                }
                // Constants lead a product: x*2 → 2*x, and 2*(3*x) → 6*x.
                if (right instanceof Formula.Constant && !(left instanceof Formula.Constant)) {
                    yield binary(operator, right, left);
                }
                if (left instanceof Formula.Constant outer && right instanceof Formula.Binary inner
                        && inner.operator() == Formula.Operator.MULTIPLY && inner.left() instanceof Formula.Constant nested) {
                    yield new Formula.Binary(operator, new Formula.Constant(outer.value() * nested.value()), inner.right());
                }
                yield new Formula.Binary(operator, left, right);
            }
            case DIVIDE -> {
                if (isValue(left, 0) && !isValue(right, 0)) {
                    yield new Formula.Constant(0);
                }
                if (isValue(right, 1)) {
                    yield left;
                }
                if (left.equals(right) && !isValue(right, 0)) {
                    yield new Formula.Constant(1);
                }
                yield new Formula.Binary(operator, left, right);
            }
            case POWER -> {
                if (isValue(right, 0)) {
                    yield new Formula.Constant(1);
                }
                if (isValue(right, 1)) {
                    yield left;
                }
                if (isValue(left, 1)) {
                    yield new Formula.Constant(1);
                }
                yield new Formula.Binary(operator, left, right);
            }
        };
    }

    private static boolean isValue(Formula expression, double value) {
        return expression instanceof Formula.Constant constant && constant.value() == value;
    }
}
