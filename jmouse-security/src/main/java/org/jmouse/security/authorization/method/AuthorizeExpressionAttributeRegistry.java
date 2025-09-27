package org.jmouse.security.authorization.method;

import org.jmouse.el.node.Expression;
import org.jmouse.security.core.access.annotation.Authorize;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.function.Function;

public class AuthorizeExpressionAttributeRegistry extends AbstractExpressionAttributeRegistry<ExpressionAttribute> {

    public AuthorizeExpressionAttributeRegistry(MethodExpressionHandler<Expression> expressionHandler) {
        super(expressionHandler);
    }

    @Override
    protected ExpressionAttribute resolveAttribute(Method method, Class<?> targetClass) {
        ExpressionAttribute expressionAttribute = () -> null;
        Authorize           authorize           = findAuthorize(method, targetClass);

        if (authorize != null) {
            MethodExpressionHandler<Expression> expressionHandler = getExpressionHandler();
            Expression                          expression        = expressionHandler.getExpressionLanguage()
                    .compile(authorize.value());

            expressionAttribute = () -> expression;
        }

        return expressionAttribute;
    }

    private Authorize findAuthorize(Method method, Class<?> targetClass) {
        Function<AnnotatedElement, Authorize> lookup     = findUniqueAnnotation(Authorize.class);
        Authorize                             annotation = lookup.apply(method);
        return annotation == null ? lookup.apply(getClass(method, targetClass)) : annotation;
    }

}
