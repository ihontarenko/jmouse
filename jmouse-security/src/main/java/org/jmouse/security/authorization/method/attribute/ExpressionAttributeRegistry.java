package org.jmouse.security.authorization.method.attribute;

import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.authorization.method.MethodExpressionHandler;

import java.lang.reflect.Method;

public interface ExpressionAttributeRegistry<T extends ExpressionAttribute> {

    MethodExpressionHandler<MethodInvocation> getExpressionHandler();

    T getAttribute(Method method, Class<?> type);

    T getAttribute(Method method);

    T getAttribute(MethodInvocation invocation);

    T resolveAttribute(Method method, Class<?> targetClass);

}
