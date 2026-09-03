package org.jmouse.el.node;

import org.jmouse.el.node.expression.*;
import org.jmouse.el.node.expression.unary.NegateUnaryOperation;

/**
 * A visitor that <strong>returns something</strong> — for turning an expression into another form.
 *
 * <h2>⚠️ Why this exists beside {@link Visitor} rather than replacing it</h2>
 *
 * <p>{@link Visitor} returns {@code void}. It is a traversal for side effects, and the template engine's
 * renderer and initializer are built on it correctly — nothing about it is wrong, and nothing here
 * changes it.</p>
 *
 * <p>But compiling is not a traversal, it is a <em>fold</em>: an expression becomes SQL and its bound
 * values, or a DTO, or whatever a second backend needs. Written through a {@code void} visitor the
 * result has to accumulate into a field, which puts it somewhere the type system cannot see — the same
 * "nowhere to put the value" that makes a {@code String}-returning compile step wrong.</p>
 *
 * <h2>⚠️ The failure mode is the design</h2>
 *
 * <p>Every method defaults to {@link #visitUnsupported}, which <strong>throws</strong> and names the
 * shape it could not handle. Two things follow, and both are the point:</p>
 *
 * <ul>
 *   <li>A backend implements only what it supports; everything else refuses loudly, with one message
 *       written once rather than a {@code default -> throw} invented separately in each backend.</li>
 *   <li>⚠️ <strong>A node kind added to this package later routes to {@code visitUnsupported} in every
 *       existing backend.</strong> It refuses rather than being silently skipped — and a visitor that
 *       quietly ignored an unknown node would drop a condition from a query and return rows satisfying
 *       part of what was asked, with nothing anywhere to say so.</li>
 * </ul>
 *
 * <p>Dispatch lives in {@link Expressions#walk}, so the type switch is written once instead of being
 * copied into each backend. No node class implements an {@code accept} for this — nothing existing has
 * to change to gain it.</p>
 *
 * @param <R> what a visit produces
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ExpressionVisitor<R> {

    /**
     * Anything this visitor has not been taught to handle.
     *
     * <p>⚠️ Throws by default, deliberately. A backend that means to ignore a shape has to say so, in
     * its own code, where a reader can see the decision.</p>
     *
     * @param expression the shape that was met
     * @return nothing — this throws unless a backend overrides it
     */
    default R visitUnsupported(Expression expression) {
        throw new UnsupportedOperationException(
                "%s cannot be handled by %s".formatted(
                        expression == null ? "null" : expression.getClass().getSimpleName(),
                        getClass().getSimpleName()));
    }

    /** A constant — a number, a string, a boolean, {@code null}. */
    default R visitLiteral(LiteralNode<?> literal) {
        return visitUnsupported(literal);
    }

    /** A path into the data — {@code entry[quantity]}, {@code issue.assignee}. */
    default R visitProperty(PropertyNode property) {
        return visitUnsupported(property);
    }

    /** Two operands and an operator — comparison, logic, arithmetic. */
    default R visitBinary(BinaryOperation operation) {
        return visitUnsupported(operation);
    }

    /** A converter or transformation pipe — {@code x | int}. */
    default R visitFilter(FilterNode filter) {
        return visitUnsupported(filter);
    }

    /** A predicate — {@code x is contains(…)}, {@code x is null}. */
    default R visitTest(TestNode test) {
        return visitUnsupported(test);
    }

    /** Membership — {@code x in […]}. */
    default R visitMembership(InOperationNode membership) {
        return visitUnsupported(membership);
    }

    /** A null fallback — {@code a ?? b}. */
    default R visitFallback(NullSafeFallbackNode fallback) {
        return visitUnsupported(fallback);
    }

    /** A conditional — {@code c ? a : b}. */
    default R visitTernary(TernaryNode ternary) {
        return visitUnsupported(ternary);
    }

    /** A logical negation — {@code !x}. */
    default R visitNegation(NegateUnaryOperation negation) {
        return visitUnsupported(negation);
    }

    /** Any other one-operand operation — {@code ++i}, {@code i--}. */
    default R visitUnary(UnaryOperation unary) {
        return visitUnsupported(unary);
    }

    /** A call — {@code count(x)}. */
    default R visitCall(FunctionNode call) {
        return visitUnsupported(call);
    }

    /** A literal list — {@code ['a', 'b']}. */
    default R visitArray(ArrayNode array) {
        return visitUnsupported(array);
    }

    /**
     * A bare argument list.
     *
     * <p>⚠️ {@link ArrayNode} extends {@link ArgumentsNode}, so a visitor that handled only this one
     * would receive arrays here too. {@link Expressions#walk} asks for the array first so the two stay
     * distinguishable — an array is a value and an argument list is not.</p>
     */
    default R visitArguments(ArgumentsNode arguments) {
        return visitUnsupported(arguments);
    }

    /**
     * A scoped bean access — {@code @world.reveal('base')}, {@code @player#MAX}, {@code @player:$id}.
     *
     * <p>⚠️ <strong>A backend that does not implement this refuses it, and that is the safe default.</strong>
     * This is the one shape reaching outside the expression into whatever a host registered, so a
     * visitor auditing what a document may touch has to see it — one that silently skipped it would
     * report a document clean while it called into anything at all.</p>
     */
    default R visitBeanAccess(BeanAccessNode access) {
        return visitUnsupported(access);
    }

    /** A method call on a value already in scope — {@code unit.distanceTo(target)}. */
    default R visitScopedCall(ScopedCallNode call) {
        return visitUnsupported(call);
    }

    /** A range — {@code 1..10}. */
    default R visitRange(RangeNode range) {
        return visitUnsupported(range);
    }

    /** A literal map — <code>{'a': 1}</code>. */
    default R visitMap(MapNode map) {
        return visitUnsupported(map);
    }

    /** One entry of a literal map. */
    default R visitKeyValue(KeyValueNode entry) {
        return visitUnsupported(entry);
    }
}
