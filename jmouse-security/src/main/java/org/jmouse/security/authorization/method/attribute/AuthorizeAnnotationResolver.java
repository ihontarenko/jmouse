package org.jmouse.security.authorization.method.attribute;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParseException;
import org.jmouse.security.authorization.method.AuthorizedExpressionAttribute;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.core.access.MethodExpressionHandler;
import org.jmouse.security.core.access.annotation.Authorize;

import java.lang.reflect.Method;

public class AuthorizeAnnotationResolver implements AttributeResolver<Authorize> {

    @Override
    public Class<Authorize> annotationType() {
        return Authorize.class;
    }

    @Override
    public ExpressionAttribute resolve(
            Authorize authorize,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler
    ) {
        try {
            Expression expression = handler.getExpressionLanguage().compile(authorize.value());
            return new AuthorizedExpressionAttribute(authorize.phase(), expression);
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Could not parse @Authorize expression: '%s' at %s#%s".formatted(
                            authorize.value(), targetClass.getSimpleName(), method.getName()), e);
        }
    }

    @Override
    public int order() {
        return -10;
    }
}