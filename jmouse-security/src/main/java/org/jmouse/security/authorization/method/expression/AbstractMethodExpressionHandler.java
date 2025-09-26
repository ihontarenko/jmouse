package org.jmouse.security.authorization.method.expression;

import org.jmouse.el.ExpressionLanguage;

abstract public class AbstractMethodExpressionHandler implements MethodExpressionHandler {

    private final ExpressionLanguage language;

    protected AbstractMethodExpressionHandler(ExpressionLanguage language) {
        this.language = language;
    }

    @Override
    public ExpressionLanguage getExpressionLanguage() {
        return language;
    }

}
