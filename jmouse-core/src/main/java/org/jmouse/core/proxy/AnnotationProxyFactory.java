package org.jmouse.core.proxy;

import org.jmouse.core.proxy.annotation.InterceptFor;
import org.jmouse.core.reflection.ClassFinder;
import org.jmouse.core.reflection.Reflections;

/**
 * An extension of {@link DefaultProxyFactory} that automatically discovers and registers
 * {@link MethodInterceptor}s based on the {@link InterceptFor} annotation.
 * <p>
 * This class scans the provided base classes (and potentially their packages) using
 * {@link ClassFinder#findAnnotatedClasses(Class, Class[])} to locate classes annotated with
 * {@code @ProxyMethodInterceptor}. Each discovered class is instantiated via reflection and
 * then added to this factory's interceptor list.
 *
 * @see DefaultProxyFactory
 * @see InterceptFor
 */
public class AnnotationProxyFactory extends DefaultProxyFactory {

    /**
     * Constructs an {@code AnnotationProxyFactory} and immediately scans
     * the specified base classes for interceptors annotated with
     * {@link InterceptFor}.
     *
     * @param baseClasses one or more base classes (or packages) used to discover annotated interceptors
     */
    public AnnotationProxyFactory(Class<?>... baseClasses) {
        super();
        scanInterceptors(baseClasses);
    }

    /**
     * Scans the specified base classes (and potentially their packages) for all
     * classes annotated with {@link InterceptFor}. For each discovered
     * class, this method instantiates a {@link MethodInterceptor} via reflection
     * and adds it to the list of interceptors in this factory.
     *
     * @param baseClasses one or more base classes (or packages) used to discover annotated interceptors
     */
    public void scanInterceptors(Class<?>... baseClasses) {
        for (Class<?> annotatedClass : ClassFinder.findAnnotatedClasses(InterceptFor.class, baseClasses)) {
            MethodInterceptor interceptor = (MethodInterceptor) Reflections.instantiate(
                    Reflections.findFirstConstructor(annotatedClass));
            addInterceptor(interceptor);
        }
    }
}
