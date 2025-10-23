package org.jmouse.security.authorization.method;

import org.jmouse.el.node.Expression;

@FunctionalInterface
public interface ExpressionAttribute<A> {

    Expression expression();

    default A attribute() {
        return null;
    }

    default boolean isDummy() {
        return expression() == null;
    }

}