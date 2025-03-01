package org.jmouse.template.node;

/**
 * AbstractExpression is the base class for expression nodes in the AST.
 * <p>
 * It extends {@link AbstractNode} and implements the {@link Expression} interface,
 * providing a common structure for expressions that optionally contain a sub-expression (operand).
 * This base class can be used for both unary and composite expressions.
 * </p>
 *
 * @author Ivan Hontarenko
 * @version 1.0
 */
public class AbstractExpression extends AbstractNode implements Expression {

    /**
     * The operand sub-expression.
     * <p>
     * This field represents the child expression of this node.
     * It is used by subclasses that require a single operand, such as unary operations.
     * For composite expressions (e.g. binary operations), the subclass should manage its own child nodes.
     * </p>
     */
    private final Expression operand;

    /**
     * Constructs an AbstractExpression with the specified operand.
     *
     * @param operand the sub-expression that serves as the operand; may be null if not applicable
     */
    public AbstractExpression(Expression operand) {
        this.operand = operand;
    }

    /**
     * Returns the operand of this expression.
     *
     * @return the operand expression, or null if no operand is associated with this expression
     */
    public Expression getOperand() {
        return operand;
    }
}
