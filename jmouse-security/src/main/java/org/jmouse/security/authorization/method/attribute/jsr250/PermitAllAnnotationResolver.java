package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.PermitAll;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.node.expression.literal.BooleanLiteralNode;
import org.jmouse.security.authorization.method.AuthorizedExpressionAttribute;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.authorization.method.MethodExpressionHandler;
import org.jmouse.security.core.access.Phase;

import java.lang.reflect.Method;

public class PermitAllAnnotationResolver extends Jsr250AnnotationResolver<PermitAll> {

    @Override
    public Class<PermitAll> annotationType() {
        return PermitAll.class;
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    public ExpressionAttribute resolve(
            PermitAll annotation,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler
    ) {
        return new AuthorizedExpressionAttribute(Phase.BEFORE, new BooleanLiteralNode(true));
    }

    @Override
    protected String getExpressionString(PermitAll annotation) {
        throw new UnsupportedOperationException();
    }

}