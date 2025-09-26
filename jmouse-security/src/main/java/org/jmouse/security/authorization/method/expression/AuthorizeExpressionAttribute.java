package org.jmouse.security.authorization.method.expression;

import org.jmouse.el.node.Expression;
import org.jmouse.security.authorization.method.deny.MethodAuthorizationDeniedHandler;

public final class AuthorizeExpressionAttribute implements ExpressionAttribute {

    private final Expression                       expression;
    private final MethodAuthorizationDeniedHandler handler;

    public AuthorizeExpressionAttribute(Expression expression, MethodAuthorizationDeniedHandler handler) {
        this.expression = expression;
        this.handler = handler;
    }

    @Override
    public Expression getExpression() {
        return expression;
    }

    public MethodAuthorizationDeniedHandler denyHandler() {
        return handler;
    }

}
