package org.jmouse.security.authorization.method;

import org.jmouse.el.node.Expression;

@FunctionalInterface
public interface ExpressionAttribute {

    Expression expression();

}