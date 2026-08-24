package org.jmouse.query.el.function;

import org.jmouse.el.node.Expression;
import org.jmouse.el.node.ExpressionVisitor;
import org.jmouse.el.node.Expressions;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.*;
import org.jmouse.el.node.expression.unary.NegateUnaryOperation;

/**
 * Rebuilds an expression, letting a subclass replace parts of it.
 *
 * <p>⚠️ <strong>Rebuilds rather than mutates.</strong> One parsed function body is inlined at every call
 * site, and mutating it in place would mean the second call site substituted arguments into a body the
 * first had already rewritten. Producing a fresh tree each time is what makes calling a function twice
 * with different arguments mean two different things.</p>
 *
 * <p>The rebuild is an {@link ExpressionVisitor}{@code <Expression>} — which is what that contract was
 * added for. Anything it has not been taught to rebuild reaches {@code visitUnsupported} and
 * <strong>refuses</strong>, so a node kind added later cannot be silently dropped out of a condition.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class Rewriter implements ExpressionVisitor<Expression> {

    /** Rewrites an expression, in full. */
    public Expression rewrite(Expression expression) {
        return expression == null ? null : Expressions.walk(expression, this);
    }

    @Override
    public Expression visitLiteral(LiteralNode<?> literal) {
        return literal;
    }

    @Override
    public Expression visitProperty(PropertyNode property) {
        return property;
    }

    @Override
    public Expression visitBinary(BinaryOperation operation) {
        return new BinaryOperation(
                rewrite(operation.getLeft()), operation.getOperator(), rewrite(operation.getRight()));
    }

    @Override
    public Expression visitFilter(FilterNode filter) {
        FilterNode rebuilt = new FilterNode(filter.getName());

        rebuilt.setLeft(rewrite(filter.getLeft()));
        rebuilt.setArguments(rewrite(filter.getArguments()));

        return rebuilt;
    }

    @Override
    public Expression visitTest(TestNode test) {
        TestNode rebuilt = new TestNode(test.getName());

        rebuilt.setLeft(rewrite(test.getLeft()));
        rebuilt.setArguments(rewrite(test.getArguments()));
        rebuilt.setNegated(test.isNegated());

        return rebuilt;
    }

    @Override
    public Expression visitMembership(InOperationNode membership) {
        InOperationNode rebuilt = new InOperationNode();

        rebuilt.setLeft(rewrite(membership.getLeft()));
        rebuilt.setRight(rewrite(membership.getRight()));

        return rebuilt;
    }

    @Override
    public Expression visitFallback(NullSafeFallbackNode fallback) {
        NullSafeFallbackNode rebuilt = new NullSafeFallbackNode();

        rebuilt.setNullable(rewrite(fallback.getNullable()));
        rebuilt.setOtherwise(rewrite(fallback.getOtherwise()));

        return rebuilt;
    }

    @Override
    public Expression visitTernary(TernaryNode ternary) {
        TernaryNode rebuilt = new TernaryNode();

        rebuilt.setCondition(rewrite(ternary.getCondition()));
        rebuilt.setThenBranch(rewrite(ternary.getThenBranch()));
        rebuilt.setElseBranch(rewrite(ternary.getElseBranch()));

        return rebuilt;
    }

    @Override
    public Expression visitNegation(NegateUnaryOperation negation) {
        return new NegateUnaryOperation(rewrite(negation.getOperand()));
    }

    @Override
    public Expression visitCall(FunctionNode call) {
        FunctionNode rebuilt = new FunctionNode(call.getName());

        rebuilt.setArguments(rewrite(call.getArguments()));

        return rebuilt;
    }

    @Override
    public Expression visitArray(ArrayNode array) {
        return children(new ArrayNode(), array);
    }

    @Override
    public Expression visitArguments(ArgumentsNode arguments) {
        return children(new ArgumentsNode(), arguments);
    }

    private Expression children(ArgumentsNode rebuilt, ArgumentsNode original) {
        for (Node child : original.getChildren()) {
            if (child instanceof Expression item) {
                rebuilt.add(rewrite(item));
            }
        }

        return rebuilt;
    }
}
