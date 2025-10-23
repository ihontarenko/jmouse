package org.jmouse.security.core.access;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.security.authorization.method.SecurityExpressionLanguage;

abstract public class AbstractExpressionHandler<T> implements ExpressionHandler<T> {

    private final ExpressionLanguage language;

    public AbstractExpressionHandler(ExpressionLanguage language) {
        this.language = language;
    }

    public AbstractExpressionHandler() {
        this(SecurityExpressionLanguage.getSharedInstance());
    }

    public ExpressionLanguage getExpressionLanguage() {
        return language;
    }

}
