package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.PermitAll;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.node.Expression;
import org.jmouse.el.node.expression.literal.BooleanLiteralNode;
import org.jmouse.security.authorization.method.AnnotationExpressionAttribute;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.core.access.MethodExpressionHandler;
import org.jmouse.security.core.access.Phase;

import java.lang.reflect.Method;

public class PermitAllAnnotationResolver extends Jsr250AnnotationResolver<PermitAll> {

    private static final Expression PERMIT_ALL_EXPRESSION = new BooleanLiteralNode(true);

    @Override
    public Class<PermitAll> annotationType() {
        return PermitAll.class;
    }

    @Override
    public int order() {
        return 5;
    }

    @Override
    public ExpressionAttribute<PermitAll> resolve(
            PermitAll permitAll,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler
    ) {
        return new AnnotationExpressionAttribute<>(Phase.BEFORE, PERMIT_ALL_EXPRESSION, permitAll);
    }

    @Override
    protected String getExpressionString(PermitAll annotation) {
        throw new UnsupportedOperationException();
    }

}