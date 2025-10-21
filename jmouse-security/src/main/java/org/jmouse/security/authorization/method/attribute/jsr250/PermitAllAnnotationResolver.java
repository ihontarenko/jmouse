package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.PermitAll;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.authorization.method.MethodExpressionHandler;
import org.jmouse.security.authorization.method.attribute.AttributeResolver;
import org.jmouse.security.authorization.method.attribute.ExpressionAttributeFactory;

import java.lang.reflect.Method;

public class PermitAllAnnotationResolver implements AttributeResolver<PermitAll> {

    public static final String PERMIT_ALL_EL = "permitAll()";

    @Override
    public Class<PermitAll> annotationType() {
        return PermitAll.class;
    }

    @Override
    public ExpressionAttribute resolve(
            PermitAll annotation,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler,
            ExpressionAttributeFactory factory
    ) {
        return factory.create(handler.getExpressionLanguage().compile(PERMIT_ALL_EL));
    }

    @Override
    public int order() {
        return 5;
    }

}