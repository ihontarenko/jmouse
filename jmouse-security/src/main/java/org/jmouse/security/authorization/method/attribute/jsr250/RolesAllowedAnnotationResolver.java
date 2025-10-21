package org.jmouse.security.authorization.method.attribute.jsr250;

import jakarta.annotation.security.RolesAllowed;
import org.jmouse.core.Streamable;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.el.node.Expression;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.authorization.method.MethodExpressionHandler;
import org.jmouse.security.authorization.method.attribute.AttributeResolver;
import org.jmouse.security.authorization.method.attribute.ExpressionAttributeFactory;
import org.jmouse.util.StringHelper;

import java.lang.reflect.Method;

public class RolesAllowedAnnotationResolver implements AttributeResolver<RolesAllowed> {

    @Override
    public Class<RolesAllowed> annotationType() {
        return RolesAllowed.class;
    }

    @Override
    public ExpressionAttribute resolve(
            RolesAllowed annotation,
            Method method,
            Class<?> targetClass,
            MethodExpressionHandler<MethodInvocation> handler,
            ExpressionAttributeFactory factory
    ) {
        String rolesAllowed = Streamable.of(annotation.value())
                .map(StringHelper::unquote)
                .map(role -> "'" + role + "'")
                .joining(", ");

        String     expressionLanguage = "hasAnyRole(" + rolesAllowed + ")";
        Expression expression         = handler.getExpressionLanguage().compile(expressionLanguage);

        return factory.create(expression);
    }
}