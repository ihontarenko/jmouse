package org.jmouse.security.authorization.method;

import org.jmouse.core.reflection.annotation.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public abstract class AbstractExpressionAttributeRegistry<T extends ExpressionAttribute> {

    private final Map<Key, T> cache = new ConcurrentHashMap<>();

    public final T getAttribute(Method method, Class<?> targetClass) {
        return cache.computeIfAbsent(new Key(method, targetClass), k -> resolveAttribute(method, targetClass));
    }

    protected final <A extends Annotation> Function<AnnotatedElement, A> findUniqueAnnotation(Class<A> type) {
        return Annotations.findUniqueAnnotation(type);
    }

    protected abstract T resolveAttribute(Method method, Class<?> targetClass);

    protected static Class<?> targetClass(Method method, Class<?> targetClass) {
        return (targetClass != null) ? targetClass : method.getDeclaringClass();
    }

    private record Key(Method method, Class<?> target) {}
}
