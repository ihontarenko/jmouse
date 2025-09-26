package org.jmouse.el.node.expression;

import org.jmouse.core.convert.Conversion;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.Operator;
import org.jmouse.el.extension.operator.ComparisonOperator;
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

    @Override
    public Object evaluate(EvaluationContext context) {
        Object left  = getLeft().evaluate(context);
        Object right = getRight().evaluate(context);

        if (operator instanceof ComparisonOperator) {
            // aligning data types to a single one for comparisons
            Conversion conversion = context.getConversion();
            right = conversion.convert(right, left.getClass());
        }

        return operator.getCalculator().calculate(left, right);
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

    @Override
    public String toString() {
        return "( %s %s %s )".formatted(left.toString(), operator.getName(), right.toString());
    }
}
