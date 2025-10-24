package org.jmouse.security.authorization.method.attribute;


import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.security.authorization.method.ExpressionAttribute;
import org.jmouse.security.access.MethodExpressionHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 🧩 Strategy that knows how to convert a specific annotation into an ExpressionAttribute.
 *
 * @param <A> supported annotation type
 */
public interface AttributeResolver<A extends Annotation> {

    /**
     * @return the annotation type this resolver recognizes
     */
    Class<A> annotationType();

    /**
     * Build an {@link ExpressionAttribute} from the discovered annotation.
     *
     * @param annotation  the annotation instance (method or class level)
     * @param method      the invoked method
     * @param targetClass the concrete target class
     * @param handler     expression/EL infrastructure
     */
    ExpressionAttribute<A> resolve(
            A annotation, Method method, Class<?> targetClass, MethodExpressionHandler<MethodInvocation> handler);

    /**
     * Priority: smaller first. Allows preferring method-level resolvers, etc.
     */
    default int order() {
        return 0;
    }
}