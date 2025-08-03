package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class SynthesizedAnnotationProxy<A extends Annotation> implements InvocationHandler {

    private final MergedAnnotation annotation;
    private final Class<A> type;

    public SynthesizedAnnotationProxy(MergedAnnotation annotation, Class<A> type) {
        this.annotation = annotation;
        this.type = type;
    }

    public MergedAnnotation getAnnotation() {
        return annotation;
    }

    public Class<A> getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A create(MergedAnnotation annotation, Class<A> type) {
        ClassLoader       classLoader = type.getClassLoader();
        Class<?>[]        interfaces  = new Class<?>[] {type};
        return (A) Proxy.newProxyInstance(
                classLoader, interfaces, new SynthesizedAnnotationProxy<>(annotation, type));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }
}
