package org.jmouse.el.node.expression;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.OperationException;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.ComparisonOperator;
import org.jmouse.el.extension.operator.LogicalOperator;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.Visitor;

/**
 * Represents a binary operation node in the Abstract Syntax Tree (AST).
 *
 * <p>A binary operation consists of two expressions (left and right) and an operator,
 * such as addition, subtraction, multiplication, or division.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class BinaryOperation extends AbstractExpression {

    /**
     * The left-hand side tag of the binary operation.
     */
    private final Expression left;

    /**
     * The operator used in the binary operation.
     */
    private final Operator operator;

    /**
     * The right-hand side tag of the binary operation.
     */
    private final Expression right;

    /**
     * Constructs a {@code BinaryOperation} with the specified left-hand side tag,
     * operator, and right-hand side tag.
     *
     * @param left     the left-hand side tag of the binary operation
     * @param operator the operator used in the binary operation
     * @param right    the right-hand side tag of the binary operation
     */
    public BinaryOperation(Expression left, Operator operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    /**
     * Returns the left-hand side tag of the binary operation.
     *
     * @return the left-hand side tag
     */
    public Expression getLeft() {
        return left;
    }

    /**
     * Returns the right-hand side tag of the binary operation.
     *
     * @return the right-hand side tag
     */
    public Expression getRight() {
        return right;
    }

    /**
     * Returns the operator used in the binary operation.
     *
     * @return the operator
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Evaluates the operation.
     *
     * <h3>⚠️ {@code and} and {@code or} stop as soon as the answer is decided</h3>
     *
     * <p>This used to evaluate <strong>both</strong> sides before handing them to the calculator, which
     * made short-circuiting structurally impossible: a {@link org.jmouse.el.extension.Calculator}
     * receives values, never the expressions that produced them, so by the time it could decide that
     * the right operand did not matter, the right operand had already run.
     *
     * <p>Three things were wrong with that, in rising order of seriousness. It costs work nobody
     * needed — a lazily-resolved value on the right of a {@code false and …} is fetched anyway. It
     * surprises: every language a reader knows stops at the first {@code false}. And it makes the
     * commonest defensive shape in the language <em>not work</em> — you cannot guard a fragile operand
     * with a cheap test in front of it, which is exactly how anybody would write
     * {@code action == 'x' and something.about(x)}.
     *
     * <p>That last one is not hypothetical. An authorization rule reading
     * {@code action == 'entry.listByPurpose' and purpose != 'ASSET'} evaluated the right side on every
     * call, including the ones with no purpose at all; the operator threw, the condition became
     * unanswerable, and an unanswerable deny is applied fail-closed. A whole subject area stopped
     * working (Innoventa INVT-0126). The guard was there, written the way everyone writes it, and it
     * guarded nothing.
     *
     * <p>⚠️ <strong>The left side is still evaluated exactly once.</strong> Deciding after it, rather
     * than peeking before it, is what keeps that true — a version that tested the left twice would
     * trade this bug for a subtler one.
     *
     * <p>Only {@code AND} and {@code OR} short-circuit, and only on a {@link Boolean} left operand.
     * Anything else falls through to the calculator unchanged, so {@code XOR}, {@code NOT} and every
     * non-boolean left operand behave exactly as they did.
     *
     * <h3>⚠️ The short circuit is also what keeps a refused operand out of the way</h3>
     *
     * <p>{@link org.jmouse.el.extension.calculator.LogicalCalculator} no longer answers {@code false}
     * for an operand that is not a {@link Boolean} — it treats {@code null} as false and <em>refuses</em>
     * anything else, because a plausible wrong answer was the whole defect there. Deciding here first
     * means {@code true or <anything at all>} is {@code true} and the right operand is never looked at,
     * so a rule guarded by a cheap test in front of a doubtful value keeps working exactly as its
     * author intended.
     *
     * <p>⚠️ {@code false and <anything>} is the same story on the other side, and both were already
     * true of this method before the calculator changed. What changed is that the operands the short
     * circuit does <em>not</em> skip are now answered honestly rather than as a silent {@code false}.
     *
     * @param context the evaluation context
     * @return the result of the operation
     */
    @Override
    public Object evaluate(EvaluationContext context) {
        Object left = getLeft().evaluate(context);

        if (operator instanceof LogicalOperator logical) {
            Boolean decided = decidedByLeftAlone(logical, left);

            if (decided != null) {
                return decided;
            }
        }

        Object right = getRight().evaluate(context);

        if (operator instanceof ComparisonOperator) {
            // aligning data types to a single one for comparisons
            Conversion conversion = context.getConversion();
            if (left != null) {
                right = conversion.convert(right, left.getClass());
            }
        }

        try {
            return operator.getCalculator().calculate(left, right);
        } catch (Exception exception) {
            String expression = "%s %s %s".formatted(getLeft(), operator, getRight());
            String evaluated  = "%s %s %s".formatted(left, operator, right);
            throw new OperationException("Binary operation (%s) execution failed. Expression: [%s]".formatted(
                    evaluated, expression
            ), exception);
        }
    }

    /**
     * The answer where the left operand alone settles it, or {@code null} where the right one is still
     * needed.
     *
     * <p>{@code false and …} is false whatever follows; {@code true or …} is true. Everything else —
     * including a left operand that is not a {@link Boolean} at all — returns {@code null} here and is
     * decided by the calculator exactly as before, so this can only ever <em>remove</em> evaluation and
     * never change an answer.
     */
    private Boolean decidedByLeftAlone(LogicalOperator logical, Object left) {
        if (!(left instanceof Boolean decided)) {
            return null;
        }

        if (logical == LogicalOperator.AND && !decided) {
            return false;
        }

        if (logical == LogicalOperator.OR && decided) {
            return true;
        }

        return null;
    }

    /**
     * Recursively executes the given consumer on this node and all its children.
     *
     * @param visitor the consumer to execute on each node
     */
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
        left.accept(visitor);
        right.accept(visitor);
    }

    /**
     * Writes the operation back in the syntax it was read from.
     *
     * <p>⚠️ <strong>{@code getSpelling()}, never {@code getName()}.</strong> A name is a label for a
     * reader — {@code GREATER_THAN} — and printing it produced {@code ( a GREATER_THAN b )}, which the
     * lexer reads as three identifiers rather than a comparison. The un-parse has to emit what can be
     * read back.</p>
     *
     * <p>The parentheses are kept rather than reconstructed from precedence. Redundant ones are
     * harmless and re-parse identically; working out which are needed is a second implementation of
     * precedence, and two of those disagree eventually.</p>
     */
    @Override
    public String toSource() {
        return "( %s %s %s )".formatted(left.toSource(), operator.getSpelling(), right.toSource());
    }

    @Override
    public String toString() {
        return "( %s %s %s )".formatted(left.toString(), operator.getName(), right.toString());
    }
}
