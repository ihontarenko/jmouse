package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.DenyAll;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.node.expression.literal.BooleanLiteralNode;
import org.jmouse.security.authorization.method.AuthorizedExpressionAttribute;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.authorization.method.MethodExpressionHandler;
import org.jmouse.security.core.access.Phase;

import java.lang.reflect.Method;

public class DenyAllAnnotationResolver extends Jsr250AnnotationResolver<DenyAll> {

    @Override
    public Class<DenyAll> annotationType() {
        return DenyAll.class;
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    public ExpressionAttribute resolve(
            DenyAll annotation,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler
    ) {
        return new AuthorizedExpressionAttribute(Phase.BEFORE, new BooleanLiteralNode(false));
    }

    @Override
    protected String getExpressionString(DenyAll annotation) {
        throw new UnsupportedOperationException();
    }

}