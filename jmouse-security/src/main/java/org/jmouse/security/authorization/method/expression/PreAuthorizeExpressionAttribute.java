package org.jmouse.security.authorization.method.expression;

import org.jmouse.security.authorization.method.deny.MethodAuthorizationDeniedHandler;

public final class PreAuthorizeExpressionAttribute implements ExpressionAttribute {

    private final String                           expression;
    private final MethodAuthorizationDeniedHandler handler;

    public PreAuthorizeExpressionAttribute(String expression, MethodAuthorizationDeniedHandler handler) {
        this.expression = expression;
        this.handler = handler;
    }

    @Override
    public String expression() {
        return expression;
    }

    public MethodAuthorizationDeniedHandler denyHandler() {
        return handler;
    }

}
