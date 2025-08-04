package org.jmouse.core.reflection.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

/**
 * 🌀 Dynamic proxy for synthesized annotations.
 *
 * <p>This class creates runtime proxies for annotations whose attributes are merged
 * from multiple sources. It allows for dynamic resolution of annotation attributes
 * using a {@link MergedAnnotation} and supports core annotation contract methods like
 * {@code equals}, {@code hashCode}, {@code toString}, and {@code annotationType}.</p>
 *
 * <pre>{@code
 * MergedAnnotation merged = ...;
 * MyAnnotation proxy = SynthesizedAnnotationProxy.create(merged, MyAnnotation.class);
 * String value = proxy.value();
 * }</pre>
 *
 * @param <A> annotation type
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record SynthesizedAnnotationProxy<A extends Annotation>(
        MergedAnnotation annotation, Class<A> type) implements InvocationHandler {

    /**
     * Constructs a new synthesized annotation proxy handler.
     *
     * @param annotation merged annotation metadata
     * @param type       annotation interface type
     */
    public SynthesizedAnnotationProxy {
    }

    /**
     * 🪄 Creates a proxy instance for the given annotation type using the merged metadata.
     *
     * @param annotation merged annotation to use for resolving attribute values
     * @param type       annotation interface to implement
     * @param <A>        annotation type
     * @return proxy instance implementing the annotation interface
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A create(MergedAnnotation annotation, Class<A> type) {
        ClassLoader classLoader = type.getClassLoader();
        Class<?>[]  interfaces  = new Class<?>[]{type};
        return (A) Proxy.newProxyInstance(
                classLoader, interfaces, new SynthesizedAnnotationProxy<>(annotation, type));
    }

    /**
     * Returns the underlying merged annotation used by the proxy.
     *
     * @return merged annotation metadata
     */
    @Override
    public MergedAnnotation annotation() {
        return annotation;
    }

    /**
     * Returns the type of the annotation interface.
     *
     * @return annotation interface type
     */
    @Override
    public Class<A> type() {
        return type;
    }

    /**
     * Handles method invocations on the proxy.
     *
     * @param proxy  the proxy instance
     * @param method the invoked method
     * @param arguments   method arguments
     * @return resolved value
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
        return switch (method.getName()) {
            case "annotationType" -> type;
            case "equals" -> handleEquals(proxy, arguments[0]);
            case "hashCode" -> handleHashCode();
            case "toString" -> annotation.toString();
            default -> getAttributeValue(method);
        };
    }

    /**
     * Handles equality check for synthesized annotations.
     */
    private boolean handleEquals(Object proxy, Object other) {
        if (proxy == other)
            return true;

        if (!type.isInstance(other))
            return false;

        for (Method method : type.getDeclaredMethods()) {
            Object valueA = getAttributeValue(method);
            Object valueB;

            try {
                valueB = method.invoke(other);
            } catch (Exception e) {
                return false;
            }

            if (!valueA.equals(valueB))
                return false;
        }

        return true;
    }

    /**
     * Computes the hash code for the synthesized annotation.
     */
    private int handleHashCode() {
        return annotation.getAnnotationType().hashCode();
    }

    /**
     * Resolves the value for an annotation attribute.
     *
     * @param method annotation method
     * @return resolved value or default
     */
    private Object getAttributeValue(Method method) {
        AnnotationAttributeMapping mapping = annotation.getAnnotationMapping();
        Object                     value   = mapping.getAttributeValue(method);

        if (value == null) {
            return method.getDefaultValue();
        }

        return value;
    }

}
