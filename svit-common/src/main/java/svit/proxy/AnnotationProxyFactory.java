package svit.proxy;

import svit.proxy.annotation.ProxyMethodInterceptor;
import svit.reflection.ClassFinder;
import svit.reflection.Reflections;

/**
 * A proxy factory that automatically scans for method interceptors annotated with
 * {@link ProxyMethodInterceptor} and applies them to the target class.
 * <p>
 * This class extends {@link ProxyFactory} to provide additional functionality for discovering
 * and registering {@link MethodInterceptor}s based on annotations.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * AnnotationProxyFactory proxyFactory = new AnnotationProxyFactory(userService, baseClassToScan);
 * UserService proxy = proxyFactory.getProxy();
 * }</pre>
 *
 * @see ProxyFactory
 * @see MethodInterceptor
 */
public class AnnotationProxyFactory extends ProxyFactory {

    /**
     * Constructs an {@link AnnotationProxyFactory} with the specified target object
     * and scans the provided base classes for interceptors.
     *
     * @param target      the target object to proxy.
     * @param baseClasses the base classes or packages to scan for annotated interceptors.
     */
    public AnnotationProxyFactory(Object target, Class<?>... baseClasses) {
        super(target);
        scanInterceptors(baseClasses);
    }

    /**
     * Constructs an {@link AnnotationProxyFactory} with the specified target object.
     * Interceptor scanning can be performed later using {@link #scanInterceptors(Class[])}.
     *
     * @param target the target object to proxy.
     */
    public AnnotationProxyFactory(Object target) {
        super(target);
    }

    /**
     * Scans the specified base classes or packages for classes annotated with
     * {@link ProxyMethodInterceptor} and registers their instances as {@link MethodInterceptor}s
     * for the target class.
     *
     * @param baseClasses the base classes or packages to scan for annotated interceptors.
     */
    public void scanInterceptors(Class<?>... baseClasses) {
        for (Class<?> annotatedClass : ClassFinder.findAnnotatedClasses(ProxyMethodInterceptor.class, baseClasses)) {
            ProxyMethodInterceptor annotation = annotatedClass.getAnnotation(ProxyMethodInterceptor.class);
            Class<?>[]             targets    = annotation.value();

            for (Class<?> targetClass : targets) {
                if (targetClass.isAssignableFrom(proxyConfig.getTargetClass())) {
                    MethodInterceptor interceptor = (MethodInterceptor) Reflections.instantiate(
                            Reflections.findFirstConstructor(annotatedClass));
                    addInterceptor(interceptor);
                }
            }
        }
    }
}