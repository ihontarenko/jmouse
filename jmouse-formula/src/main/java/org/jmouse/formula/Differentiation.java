package org.jmouse.formula;

import static org.jmouse.formula.Formula.add;
import static org.jmouse.formula.Formula.call;
import static org.jmouse.formula.Formula.constant;
import static org.jmouse.formula.Formula.divide;
import static org.jmouse.formula.Formula.multiply;
import static org.jmouse.formula.Formula.negate;
import static org.jmouse.formula.Formula.power;
import static org.jmouse.formula.Formula.subtract;

/**
 * Symbolic differentiation, so a gradient, a normal, a tangent plane or a Jacobian is exact rather than a finite
 * difference. A function without a usable derivative (floor, sign, step, mod) is refused, not approximated.
 */
public final class Differentiation {

    private Differentiation() {
    }

    public static Formula derivative(Formula expression, String variable) {
        return Simplification.simplify(differentiate(expression, new Target.Named(variable)));
    }

    /** The derivative by a value a host supplies in a slot — by a circuit's node voltage, say — exactly. */
    public static Formula derivative(Formula expression, Formula.Slot slot) {
        return Simplification.simplify(differentiate(expression, new Target.Positioned(slot.index(), slot.label())));
    }

    /** What a derivative is taken by: a free name, or a host's slot. */
    private sealed interface Target {

        boolean occursIn(Formula expression);

        boolean isLeaf(Formula leaf);

        /** Whether a binder's own name is this target: only a name can be bound. */
        boolean isName(String name);

        /** How a derived definition is named: {@code f_x}. */
        String suffix();

        record Named(String name) implements Target {
            public boolean occursIn(Formula expression) {
                return expression.dependsOn(name);
            }

            public boolean isLeaf(Formula leaf) {
                return leaf instanceof Formula.Variable variable && variable.name().equals(name);
            }

            public boolean isName(String candidate) {
                return name.equals(candidate);
            }

            public String suffix() {
                return name;
            }
        }

        record Positioned(int index, String label) implements Target {
            public boolean occursIn(Formula expression) {
                return expression.dependsOnSlot(index);
            }

            public boolean isLeaf(Formula leaf) {
                return leaf instanceof Formula.Slot slot && slot.index() == index;
            }

            public boolean isName(String candidate) {
                return false;
            }

            public String suffix() {
                return label;
            }
        }
    }

    private static Formula differentiate(Formula expression, Target target) {
        if (!target.occursIn(expression)) {
            return constant(0);
        }
        return switch (expression) {
            case Formula.Constant constant -> constant(0);
            case Formula.NamedConstant named -> constant(0);
            case Formula.ImaginaryUnit unit -> constant(0);
            case Formula.Variable symbol -> constant(target.isLeaf(symbol) ? 1 : 0);
            case Formula.Slot slot -> constant(target.isLeaf(slot) ? 1 : 0);
            case Formula.HostCall hostCall -> throw new FormulaException("'" + hostCall.name()
                    + "(…)' is its host's to bind before the expression is differentiated.", null, -1);
            case Formula.Negation negation -> negate(differentiate(negation.operand(), target));
            case Formula.Binary binary -> binaryDerivative(binary, target);
            case Formula.Call functionCall -> functionDerivative(functionCall, target);
            // A truth value is constant wherever it is differentiable at all.
            case Formula.Comparison comparison -> constant(0);
            case Formula.Logical logical -> constant(0);
            case Formula.Not not -> constant(0);
            case Formula.Equation equation -> subtract(differentiate(equation.left(), target),
                    differentiate(equation.right(), target));
            case Formula.Piecewise piecewise -> new Formula.Piecewise(piecewise.branches().stream()
                    .map(branch -> new Formula.Piecewise.Branch(branch.condition(),
                            differentiate(branch.value(), target))).toList(),
                    piecewise.otherwise() == null ? null : differentiate(piecewise.otherwise(), target));
            case Formula.Accumulation accumulation -> accumulationDerivative(accumulation, target);
            case Formula.Integral integral -> integralDerivative(integral, target);
            case Formula.Application application -> applicationDerivative(application, target);
        };
    }

    /**
     * The index runs over integers, so bounds that depend on the variable move in steps and contribute nothing
     * where the sum is differentiable. d∑f = ∑f'; d∏f = ∏f · ∑(f'/f).
     */
    private static Formula accumulationDerivative(Formula.Accumulation accumulation, Target target) {
        if (target.isName(accumulation.index())) {
            return constant(0);
        }
        Formula termDerivative = differentiate(accumulation.body(), target);
        if (accumulation.accumulator() == Formula.Accumulator.SUM) {
            return new Formula.Accumulation(Formula.Accumulator.SUM, accumulation.index(), accumulation.from(),
                    accumulation.to(), termDerivative);
        }
        return multiply(accumulation, new Formula.Accumulation(Formula.Accumulator.SUM, accumulation.index(),
                accumulation.from(), accumulation.to(), divide(termDerivative, accumulation.body())));
    }

    /** Leibniz: d/dx ∫ₐ⁽ˣ⁾ᵇ⁽ˣ⁾ f(t, x) dt = f(b, x) b' − f(a, x) a' + ∫ₐᵇ ∂f/∂x dt. */
    private static Formula integralDerivative(Formula.Integral integral, Target target) {
        Formula upperTerm = multiply(Substitution.substitute(integral.body(), integral.variable(), integral.upper()),
                differentiate(integral.upper(), target));
        Formula lowerTerm = multiply(Substitution.substitute(integral.body(), integral.variable(), integral.lower()),
                differentiate(integral.lower(), target));
        Formula boundaries = subtract(upperTerm, lowerTerm);
        if (target.isName(integral.variable()) || !target.occursIn(integral.body())) {
            return boundaries;
        }
        return add(boundaries, new Formula.Integral(integral.variable(), integral.lower(), integral.upper(),
                differentiate(integral.body(), target)));
    }

    /**
     * The chain rule, kept in terms of the author's own function: d f(g₁, …, gₙ)/dx = ∑ fₚ(g) gₚ' + fₓ(g), where
     * fₚ is f differentiated by its parameter p and fₓ by a top-level name its body mentions. Each is a new
     * definition, so the result still reads f'(…) rather than f's body written out.
     */
    private static Formula applicationDerivative(Formula.Application application, Target target) {
        Formula.Definition definition = application.definition();
        Formula result = constant(0);
        for (int index = 0; index < definition.parameters().size(); index++) {
            Formula argumentDerivative = differentiate(application.arguments().get(index), target);
            if (isZero(argumentDerivative)) {
                continue;
            }
            String parameter = definition.parameters().get(index);
            result = add(result, multiply(new Formula.Application(derived(definition, new Target.Named(parameter)), application.arguments()),
                    argumentDerivative));
        }
        if (definition.parameters().stream().noneMatch(target::isName) && target.occursIn(definition.body())) {
            result = add(result, new Formula.Application(derived(definition, target), application.arguments()));
        }
        return result;
    }

    private static Formula.Definition derived(Formula.Definition definition, Target by) {
        return new Formula.Definition(definition.name() + "_" + by.suffix(), definition.parameters(),
                Simplification.simplify(differentiate(definition.body(), by)));
    }

    private static boolean isZero(Formula expression) {
        return expression instanceof Formula.Constant constant && constant.value() == 0;
    }

    private static Formula binaryDerivative(Formula.Binary binary, Target target) {
        Formula left = binary.left();
        Formula right = binary.right();
        Formula leftDerivative = differentiate(left, target);
        Formula rightDerivative = differentiate(right, target);
        return switch (binary.operator()) {
            case ADD -> add(leftDerivative, rightDerivative);
            case SUBTRACT -> subtract(leftDerivative, rightDerivative);
            case MULTIPLY -> add(multiply(leftDerivative, right), multiply(left, rightDerivative));
            case DIVIDE -> divide(subtract(multiply(leftDerivative, right), multiply(left, rightDerivative)),
                    power(right, constant(2)));
            case POWER -> powerDerivative(binary, target, leftDerivative, rightDerivative);
        };
    }

    private static Formula powerDerivative(Formula.Binary binary, Target target, Formula baseDerivative,
                                    Formula exponentDerivative) {
        Formula base = binary.left();
        Formula exponent = binary.right();
        boolean exponentConstant = !target.occursIn(exponent);
        boolean baseConstant = !target.occursIn(base);
        if (exponentConstant) {
            // d(u^n) = n u^(n-1) u'
            return multiply(multiply(exponent, power(base, subtract(exponent, constant(1)))), baseDerivative);
        }
        if (baseConstant) {
            // d(a^v) = a^v ln(a) v'
            return multiply(multiply(binary, call(MathematicalFunction.LN, base)), exponentDerivative);
        }
        // d(u^v) = u^v (v' ln u + v u'/u)
        return multiply(binary, add(multiply(exponentDerivative, call(MathematicalFunction.LN, base)),
                divide(multiply(exponent, baseDerivative), base)));
    }

    private static Formula functionDerivative(Formula.Call functionCall, Target target) {
        Formula argument = functionCall.arguments().getFirst();
        Formula inner = differentiate(argument, target);
        Formula outer = switch (functionCall.function()) {
            case SIN -> call(MathematicalFunction.COS, argument);
            case COS -> negate(call(MathematicalFunction.SIN, argument));
            case TAN -> power(call(MathematicalFunction.SEC, argument), constant(2));
            case COT -> negate(power(call(MathematicalFunction.CSC, argument), constant(2)));
            case SEC -> multiply(call(MathematicalFunction.SEC, argument), call(MathematicalFunction.TAN, argument));
            case CSC -> negate(multiply(call(MathematicalFunction.CSC, argument), call(MathematicalFunction.COT, argument)));
            case ASIN -> divide(constant(1), call(MathematicalFunction.SQRT, subtract(constant(1), power(argument, constant(2)))));
            case ACOS -> negate(divide(constant(1), call(MathematicalFunction.SQRT, subtract(constant(1), power(argument, constant(2))))));
            case ATAN -> divide(constant(1), add(constant(1), power(argument, constant(2))));
            case SINH -> call(MathematicalFunction.COSH, argument);
            case COSH -> call(MathematicalFunction.SINH, argument);
            case TANH -> subtract(constant(1), power(call(MathematicalFunction.TANH, argument), constant(2)));
            case ASINH -> divide(constant(1), call(MathematicalFunction.SQRT, add(power(argument, constant(2)), constant(1))));
            case ACOSH -> divide(constant(1), call(MathematicalFunction.SQRT, subtract(power(argument, constant(2)), constant(1))));
            case ATANH -> divide(constant(1), subtract(constant(1), power(argument, constant(2))));
            case EXP -> call(MathematicalFunction.EXP, argument);
            case LN -> divide(constant(1), argument);
            case LOG10 -> divide(constant(1), multiply(argument, constant(Math.log(10))));
            case LOG2 -> divide(constant(1), multiply(argument, constant(Math.log(2))));
            case SQRT -> divide(constant(1), multiply(constant(2), call(MathematicalFunction.SQRT, argument)));
            case CBRT -> divide(constant(1), multiply(constant(3), power(call(MathematicalFunction.CBRT, argument), constant(2))));
            case ABS -> call(MathematicalFunction.SIGN, argument);
            case RE, CONJ -> constant(1);
            case IM, ARG -> constant(0);
            case ERF -> multiply(divide(constant(2), call(MathematicalFunction.SQRT, new Formula.NamedConstant(MathematicalConstant.PI))),
                    call(MathematicalFunction.EXP, negate(power(argument, constant(2)))));
            case ATAN2, MIN, MAX, HYPOT, POW, MOD, BINOMIAL -> null;
            case SIGN, FLOOR, CEIL, ROUND, STEP, GAMMA, FACTORIAL -> throw new FormulaException(
                    functionCall.function().symbol() + " has no derivative to compute exactly.", null, -1);
        };
        if (outer == null) {
            return twoArgument(functionCall, target);
        }
        return multiply(outer, inner);
    }

    private static Formula twoArgument(Formula.Call functionCall, Target target) {
        Formula first = functionCall.arguments().get(0);
        Formula second = functionCall.arguments().get(1);
        Formula firstDerivative = differentiate(first, target);
        Formula secondDerivative = differentiate(second, target);
        return switch (functionCall.function()) {
            // atan2(y, x)' = (x y' - y x') / (x^2 + y^2)
            case ATAN2 -> divide(subtract(multiply(second, firstDerivative), multiply(first, secondDerivative)),
                    add(power(first, constant(2)), power(second, constant(2))));
            case HYPOT -> divide(add(multiply(first, firstDerivative), multiply(second, secondDerivative)), functionCall);
            case POW -> differentiate(power(first, second), target);
            default -> throw new FormulaException(functionCall.function().symbol()
                    + " has no derivative to compute exactly.", null, -1);
        };
    }
}
