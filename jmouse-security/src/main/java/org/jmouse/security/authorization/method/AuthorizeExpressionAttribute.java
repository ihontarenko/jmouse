package org.jmouse.security.authorization.method;

import org.jmouse.el.node.Expression;

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
