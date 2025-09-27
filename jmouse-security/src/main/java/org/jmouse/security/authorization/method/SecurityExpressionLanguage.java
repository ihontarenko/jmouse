package org.jmouse.security.authorization.method;

import org.jmouse.core.SingletonSupplier;
import org.jmouse.el.ExpressionLanguage;

import java.util.function.Supplier;

public class SecurityExpressionLanguage extends ExpressionLanguage {

    private static final Supplier<SecurityExpressionLanguage> EXPRESSION_LANGUAGE_SUPPLIER
            = SingletonSupplier.of(SecurityExpressionLanguage::new);

    public static ExpressionLanguage getSharedInstance() {
        return EXPRESSION_LANGUAGE_SUPPLIER.get();
    }

}
