package org.jmouse.el.node.expression;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.ComparisonOperator;
import org.jmouse.el.node.AbstractExpressionNode;
import org.jmouse.el.node.ExpressionNode;

/**
 * Represents a binary operation node in the Abstract Syntax Tree (AST).
 *
 * <p>A binary operation consists of two expressions (left and right) and an operator,
 * such as addition, subtraction, multiplication, or division.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class BinaryOperation extends AbstractExpressionNode {

    /**
     * The left-hand side tag of the binary operation.
     */
    private final ExpressionNode left;

    /**
     * The operator used in the binary operation.
     */
    private final Operator operator;

    /**
     * The right-hand side tag of the binary operation.
     */
    private final ExpressionNode right;

    /**
     * Constructs a {@code BinaryOperation} with the specified left-hand side tag,
     * operator, and right-hand side tag.
     *
     * @param left     the left-hand side tag of the binary operation
     * @param operator the operator used in the binary operation
     * @param right    the right-hand side tag of the binary operation
     */
    public BinaryOperation(ExpressionNode left, Operator operator, ExpressionNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    /**
     * Returns the left-hand side tag of the binary operation.
     *
     * @return the left-hand side tag
     */
    public ExpressionNode getLeft() {
        return left;
    }

    /**
     * Returns the right-hand side tag of the binary operation.
     *
     * @return the right-hand side tag
     */
    public ExpressionNode getRight() {
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

    @Override
    public Object evaluate(EvaluationContext context) {
        Object result = null;
        Object left  = getLeft().evaluate(context);
        Object right = getRight().evaluate(context);

        if (operator instanceof ComparisonOperator) {
            Conversion conversion = context.getConversion();
            // aligning data types to a single one for comparisons
            right = conversion.convert(right, left.getClass());
        }

        result = operator.getCalculator().calculate(left, right);

        return result;
    }

    @Override
    public String toString() {
        return "( %s %s %s )".formatted(left.toString(), operator.getName(), right.toString());
    }
}
