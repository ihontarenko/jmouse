package org.jmouse.core.proxy;

import org.jmouse.core.reflection.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration class for proxy instances.
 * <p>
 * The {@code ProxyConfig} class holds the target object, its interfaces, and
 * a list of {@link MethodInterceptor}s to customize the behavior of proxy instances.
 * It also provides utility methods to determine if the target class implements
 * specific methods like {@code equals} or {@code hashCode}.
 */
public class ProxyContext {

    private final Object                  target;
    private final Class<?>                targetClass;
    private final List<Class<?>>          interfaces;
    private final List<MethodInterceptor> interceptors = new ArrayList<>();
    private final boolean                 hasHashCode;
    private final boolean                 hasEquals;
    private final ClassLoader             classLoader;

    /**
     * Constructs a {@code ProxyConfig} for the given target object.
     *
     * @param target the target object to be proxied.
     * @param classLoader the {@link ClassLoader}.
     * @throws NullPointerException if the target object is {@code null}.
     */
    public ProxyContext(Object target, ClassLoader classLoader) {
        this.target = Objects.requireNonNull(target, "Target object must not be null");
        this.targetClass = target.getClass();
        this.classLoader = classLoader;
        this.interfaces = List.of(Reflections.getClassInterfaces(targetClass));
        this.hasEquals = Reflections.hasMethod(targetClass, "equals");
        this.hasHashCode = Reflections.hasMethod(targetClass, "hashCode");
    }

    /**
     * Returns the target object being proxied.
     *
     * @return the target object.
     */
    public Object getTarget() {
        return target;
    }

    /**
     * Returns the class of the target object.
     *
     * @return the target class.
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }

    /**
     * Returns the list of interfaces implemented by the target class.
     *
     * @return a list of {@link Class} objects representing the interfaces.
     */
    public List<Class<?>> getInterfaces() {
        return interfaces;
    }

    /**
     * Adds a {@link MethodInterceptor} to the list of interceptors.
     * <p>
     * Interceptors are used to customize the behavior of method invocations on the proxy.
     *
     * @param interceptor the interceptor to add.
     */
    public void addInterceptor(MethodInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    /**
     * Returns an unmodifiable list of registered {@link MethodInterceptor}s.
     *
     * @return the list of interceptors.
     */
    public List<MethodInterceptor> getInterceptors() {
        return List.copyOf(interceptors);
    }

    /**
     * Returns an applicable {@link ClassLoader}
     *
     * @return the class loader object
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Determines whether the target class has an overridden {@code hashCode} method.
     *
     * @return {@code true} if the target class has a {@code hashCode} method, otherwise {@code false}.
     */
    public boolean hasHashCode() {
        return hasHashCode;
    }

    /**
     * Determines whether the target class has an overridden {@code equals} method.
     *
     * @return {@code true} if the target class has an {@code equals} method, otherwise {@code false}.
     */
    public boolean hasEquals() {
        return hasEquals;
    }
}
