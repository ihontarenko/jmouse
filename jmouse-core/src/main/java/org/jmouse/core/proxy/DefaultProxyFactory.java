package org.jmouse.core.proxy;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.proxy.annotation.ProxyMethodInterceptor;
import org.jmouse.core.proxy.jdk.JdkProxy;
import org.jmouse.core.reflection.ClassMatchers;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.core.reflection.Reflections.getAnnotationValue;

/**
 * Default implementation of the {@link ProxyFactory} interface. Uses the JDK dynamic proxy
 * mechanism ({@link JdkProxy}) to create proxies and applies only those {@link MethodInterceptor}s
 * whose associated annotation {@link ProxyMethodInterceptor} matches the target class.
 *
 * <p>Usage example:
 * <pre>{@code
 * // Create a new factory with some interceptors
 * ProxyFactory factory = new DefaultProxyFactory(new LoggingInterceptor(), new SecurityInterceptor());
 * // Create a proxy instance (for example, MyService is your original class)
 * InternalService proxyService = factory.createProxy(serviceInstance);
 * }</pre>
 */
public class DefaultProxyFactory implements ProxyFactory {

    private final List<MethodInterceptor> interceptors = new ArrayList<>();

    /**
     * Constructs a {@code DefaultProxyFactory} with the specified array of
     * {@link MethodInterceptor}s. Each provided interceptor will be evaluated and potentially
     * applied when creating a proxy context.
     *
     * @param interceptors one or more {@link MethodInterceptor}s to be added
     */
    public DefaultProxyFactory(MethodInterceptor... interceptors) {
        this.interceptors.addAll(List.of(interceptors));
    }

    /**
     * Creates a proxy for the specified object using the default JDK proxy mechanism.
     * The proxy is configured with the interceptors that match the target class.
     *
     * @param <T>    the type of the created proxy
     * @param object the original object to be proxied
     * @return a JDK-based proxy instance of the specified object
     * @see #createProxyContext(Object, ClassLoader)
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T createProxy(Object object) {
        // Uses default JDK proxy mechanism
        return (T) new JdkProxy(createProxyContext(object, object.getClass().getClassLoader())).getProxy();
    }

    /**
     * Creates a {@link ProxyContext} for the specified object and class loader. This method
     * determines which {@link MethodInterceptor}s should be applied by checking the
     * {@link ProxyMethodInterceptor#value() value} of each interceptor's annotation against
     * the target class. Only interceptors whose preferred class is a supertype of the target
     * class are added to the proxy context.
     *
     * @param object      the original object to be proxied
     * @param classLoader the class loader to define the proxy class
     * @return a new {@link ProxyContext} instance populated with matching interceptors
     */
    @Override
    public ProxyContext createProxyContext(Object object, ClassLoader classLoader) {
        ProxyContext      proxyContext = new ProxyContext(object, classLoader);
        Matcher<Class<?>> classMatcher = ClassMatchers.isSubtype(proxyContext.getTargetClass());

        for (MethodInterceptor interceptor : interceptors) {
            Class<?>[] preferredClasses = (Class<?>[]) getAnnotationValue(
                    interceptor.getClass(), ProxyMethodInterceptor.class, ProxyMethodInterceptor::value);

            if (preferredClasses == null) {
                continue;
            }

            for (Class<?> preferredClass : preferredClasses) {
                if (classMatcher.matches(preferredClass)) {
                    proxyContext.addInterceptor(interceptor);
                    break;
                }
            }
        }

        return proxyContext;
    }

    /**
     * Adds a {@link MethodInterceptor} to this factory. The interceptor will be considered
     * for future proxy creations, and will be applied if it matches the target class.
     *
     * @param interceptor the interceptor to add
     */
    @Override
    public void addInterceptor(MethodInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    /**
     * Retrieves the list of all currently registered {@link MethodInterceptor}s. The returned
     * list is a copy, so modifying it does not affect the actual interceptor list maintained
     * by this factory.
     *
     * @return an unmodifiable copy of the registered interceptors
     */
    @Override
    public List<MethodInterceptor> getInterceptors() {
        return List.copyOf(interceptors);
    }

}
