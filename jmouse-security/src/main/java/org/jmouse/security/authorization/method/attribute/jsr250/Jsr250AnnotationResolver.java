package org.jmouse.security.authorization.method.attribute.jsr250;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.node.Expression;
import org.jmouse.security.authorization.method.AnnotationExpressionAttribute;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.access.MethodExpressionHandler;
import org.jmouse.security.authorization.method.attribute.AttributeResolver;
import org.jmouse.security.access.Phase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

abstract public class Jsr250AnnotationResolver<A extends Annotation> implements AttributeResolver<A> {

    @Override
    public ExpressionAttribute<A> resolve(
            A annotation,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler
    ) {
        Expression expression = handler.getExpressionLanguage().compile(getExpressionString(annotation));
        return new AnnotationExpressionAttribute<>(Phase.BEFORE, expression, annotation);
    }

    abstract protected String getExpressionString(A annotation);

}
