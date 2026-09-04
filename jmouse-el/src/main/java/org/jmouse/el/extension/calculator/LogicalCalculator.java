package org.jmouse.el.extension.calculator;

import org.jmouse.el.extension.Calculator;
import org.jmouse.el.extension.OperationException;
import org.jmouse.helpers.Arrays;

import java.util.function.BinaryOperator;

/**
 * Enum representing logical operations in expressions.
 * Each operator evaluates boolean conditions.
 *
 * <h2>⚠️ NULL IS FALSE, A BOOLEAN IS ITSELF, AND ANYTHING ELSE IS A FAULT</h2>
 *
 * <p>Those three lines are the whole contract, and the third one is a deliberate refusal to guess.
 * This used to answer {@code false} whenever <em>either</em> operand was not a {@link Boolean}, which
 * is the worst of the available behaviours: it is an answer, it is plausible, and it is wrong. What it
 * actually produced:
 *
 * <pre>
 *   !null        → false     ⚠️ INVERTED — null is falsy, so its negation is true
 *   null or true → false
 *   1 and true   → false
 *   0 or true    → false
 *   '' or true   → false
 * </pre>
 *
 * <p>{@code !null} is the one that matters most. It reads as <em>"null is truthy"</em>, and it is
 * wrong in the exact shape people write guards in — {@code !something} in front of using it. In an
 * authorization {@code deny}, a spurious {@code false} <strong>permits</strong>.
 *
 * <h2>⚠️ Why not the language's own {@code Conversion}, which is the obvious move</h2>
 *
 * <p>{@code IfBranchNode} and {@code TernaryNode} coerce with
 * {@code conversion.convert(value, Boolean.class)}, so following them looks like the consistent
 * choice. Measured, that conversion is not a truthiness rule at all — it is
 * {@code Boolean.parseBoolean(String.valueOf(x))} reached through a transition chain:
 *
 * <pre>
 *   1, 0, -1, 2.5   → false      (Integer → String → Boolean)
 *   'x', '', 'no'   → false
 *   'true', 'TRUE'  → true
 *   Object, List    → ConverterNotFound
 * </pre>
 *
 * <p>Adopting it would leave {@code 1 and true} false — the same surprise with a better excuse. And a
 * {@link Calculator} receives values rather than a context, so it has no {@code Conversion} to reach
 * for in any case. Refusing is both the honest answer and the one that needs no context.
 *
 * <h2>⚠️ The short circuit still decides first, and still wins</h2>
 *
 * <p>{@code BinaryOperation} answers {@code false and …} and {@code true or …} from the left operand
 * alone and never calls this. So {@code true or <anything at all>} stays {@code true} and evaluates
 * nothing on the right — including operands this would refuse. That is intended: an operand nobody
 * needs to look at is an operand nobody has to justify.
 *
 * @author Ivan Hontarenko
 */
public enum LogicalCalculator implements Calculator<Boolean> {

    /** Logical AND ({@code &&}). */
    AND(new AndOperation()),

    /** Logical OR ({@code ||}). */
    OR(new OrOperation()),

    /** Logical NOT ({@code !}). */
    NOT(new NotOperation()),

    /** Logical XOR ({@code ^}). */
    XOR(new XorOperation());

    private final BinaryOperator<Boolean> operation;

    LogicalCalculator(BinaryOperator<Boolean> operation) {
        this.operation = operation;
    }

    @Override
    public Boolean calculate(Object... operands) {
        Boolean valueA = asBoolean(Arrays.get(operands, 0, null), "left");

        if (this == NOT) {
            return operation.apply(valueA, null);
        }

        return operation.apply(valueA, asBoolean(Arrays.get(operands, 1, null), "right"));
    }

    /**
     * One operand as a boolean, or a refusal naming what it actually was.
     *
     * <p>⚠️ <strong>The message carries the value and its type, not just "not a boolean".</strong> The
     * whole failure this replaces was invisible; a replacement that says only <em>something was
     * wrong</em> would send somebody reading a stack trace back to guessing which side of which
     * operator it came from.
     */
    private Boolean asBoolean(Object operand, String side) {
        if (operand == null) {
            return false;
        }

        if (operand instanceof Boolean already) {
            return already;
        }

        throw new OperationException(
                "The %s operand of '%s' is %s (%s), which is not true or false. Logical operators do "
                        .formatted(side, name(), operand, operand.getClass().getSimpleName())
                + "not guess: compare it to something ('x == 1'), test it ('x is empty'), or convert it "
                + "explicitly. Null is the one exception and counts as false.");
    }

    /** AND operation */
    public static class AndOperation implements BinaryOperator<Boolean> {
        @Override
        public Boolean apply(Boolean left, Boolean right) {
            return left && right;
        }
    }

    /** OR operation */
    public static class OrOperation implements BinaryOperator<Boolean> {
        @Override
        public Boolean apply(Boolean left, Boolean right) {
            return left || right;
        }
    }

    /** NOT operation */
    public static class NotOperation implements BinaryOperator<Boolean> {
        @Override
        public Boolean apply(Boolean left, Boolean ignored) {
            return !left;
        }
    }

    /** XOR operation */
    public static class XorOperation implements BinaryOperator<Boolean> {
        @Override
        public Boolean apply(Boolean left, Boolean right) {
            return left ^ right;
        }
    }

}
