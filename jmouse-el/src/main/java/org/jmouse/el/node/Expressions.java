package org.jmouse.el.node;

import org.jmouse.el.node.expression.*;
import org.jmouse.el.node.expression.unary.NegateUnaryOperation;

/**
 * Dispatch for {@link ExpressionVisitor} — the type switch, written once.
 *
 * <p>⚠️ <strong>Here rather than as an {@code accept} on every node.</strong> Double dispatch would mean
 * touching thirty node classes to gain a contract none of them needs to know about, and every future
 * node kind would have to remember to implement it. One switch in one place is a smaller thing to keep
 * right, and it is the same place a reader looks to find out what shapes exist.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Expressions {

    private Expressions() {
    }

    /**
     * Hands an expression to the visitor method that matches its shape.
     *
     * <p>⚠️ <strong>Order within the switch carries meaning where types are related.</strong>
     * {@link ArrayNode} extends {@link ArgumentsNode} and {@link NegateUnaryOperation} extends
     * {@link UnaryOperation}; the more specific of each pair is asked first, so a visitor that
     * distinguishes them can, and one that does not is unaffected.</p>
     *
     * @param expression what to dispatch
     * @param visitor    what to dispatch it to
     * @param <R>        what the visit produces
     * @return whatever the visitor made of it
     */
    public static <R> R walk(Expression expression, ExpressionVisitor<R> visitor) {
        return switch (expression) {
            case null -> visitor.visitUnsupported(null);
            case PropertyNode property -> visitor.visitProperty(property);
            case LiteralNode<?> literal -> visitor.visitLiteral(literal);
            case FilterNode filter -> visitor.visitFilter(filter);
            case TestNode test -> visitor.visitTest(test);
            case InOperationNode membership -> visitor.visitMembership(membership);
            case NullSafeFallbackNode fallback -> visitor.visitFallback(fallback);
            case TernaryNode ternary -> visitor.visitTernary(ternary);
            case NegateUnaryOperation negation -> visitor.visitNegation(negation);
            case UnaryOperation unary -> visitor.visitUnary(unary);
            case BinaryOperation operation -> visitor.visitBinary(operation);
            case FunctionNode call -> visitor.visitCall(call);
            case ArrayNode array -> visitor.visitArray(array);
            case ArgumentsNode arguments -> visitor.visitArguments(arguments);
            default -> visitor.visitUnsupported(expression);
        };
    }
}
