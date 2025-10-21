package org.jmouse.security.authorization.method.attribute;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.core.reflection.annotation.Annotations;
import org.jmouse.el.node.Expression;
import org.jmouse.el.parser.ParseException;
import org.jmouse.security.authorization.method.AuthorizedExpressionAttribute;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.authorization.method.MethodExpressionHandler;
import org.jmouse.security.core.access.annotation.Authorize;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.function.Function;

public class AuthorizeExpressionAttributeRegistry extends AbstractExpressionAttributeRegistry<ExpressionAttribute> {

    public AuthorizeExpressionAttributeRegistry(MethodExpressionHandler<MethodInvocation> expressionHandler) {
        super(expressionHandler);
    }

    @SuppressWarnings("unchecked") // todo
    protected Class<? extends Annotation>[] getAnnotations() {
        return new Class[]{Authorize.class};
    }

    @Override
    public ExpressionAttribute resolveAttribute(Method method, Class<?> targetClass) {
        ExpressionAttribute                   expressionAttribute = () -> null;
        Function<AnnotatedElement, Authorize> lookup              = Annotations.lookup(Authorize.class);
        Authorize                             authorize           = lookup.apply(method);

        if (authorize == null) {
            authorize = lookup.apply(getClass(method, targetClass));
        }

        if (authorize != null) {
            MethodExpressionHandler<MethodInvocation> expressionHandler = getExpressionHandler();

            try {
                Expression expression = expressionHandler.getExpressionLanguage().compile(authorize.value());
                expressionAttribute = new AuthorizedExpressionAttribute(authorize.phase(), expression);
            } catch (ParseException parseException) {
                throw new IllegalArgumentException(
                        "Could not parse expression: '%s'".formatted(authorize.value()), parseException);
            }
        }

        return expressionAttribute;
    }

}
