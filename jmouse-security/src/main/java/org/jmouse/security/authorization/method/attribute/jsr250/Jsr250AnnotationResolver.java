package org.jmouse.security.authorization.method.attribute.jsr250;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.authorization.method.AuthorizedExpressionAttribute;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.authorization.method.MethodExpressionHandler;
import org.jmouse.security.authorization.method.attribute.AttributeResolver;
import org.jmouse.security.core.access.Phase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

abstract public class Jsr250AnnotationResolver<A extends Annotation> implements AttributeResolver<A> {

    @Override
    public ExpressionAttribute resolve(
            A annotation,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler
    ) {
        return new AuthorizedExpressionAttribute(
                Phase.BEFORE, handler.getExpressionLanguage().compile(getExpressionString(annotation)));
    }

    abstract protected String getExpressionString(A annotation);

}
