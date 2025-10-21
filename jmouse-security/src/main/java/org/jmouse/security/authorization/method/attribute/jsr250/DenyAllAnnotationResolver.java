package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.DenyAll;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.authorization.method.MethodExpressionHandler;
import org.jmouse.security.authorization.method.attribute.AttributeResolver;
import org.jmouse.security.authorization.method.attribute.ExpressionAttributeFactory;

import java.lang.reflect.Method;

public class DenyAllAnnotationResolver implements AttributeResolver<DenyAll> {

    public static final String DENY_ALL_EL = "denyAll()";

    @Override
    public Class<DenyAll> annotationType() {
        return DenyAll.class;
    }

    @Override
    public ExpressionAttribute resolve(
            DenyAll annotation,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler,
            ExpressionAttributeFactory factory
    ) {
        return factory.create(handler.getExpressionLanguage().compile(DENY_ALL_EL));
    }

    @Override
    public int order() {
        return 5;
    }
}