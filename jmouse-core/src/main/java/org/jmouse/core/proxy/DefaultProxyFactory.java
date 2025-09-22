package org.jmouse.core.proxy;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.proxy.annotation.InterceptFor;
import org.jmouse.core.reflection.ClassMatchers;

import java.util.ArrayList;
import java.util.List;

import static org.jmouse.core.reflection.Reflections.getAnnotationValue;

/**
 * 🏭 Default implementation of {@link ProxyFactory}.
 *
 * <p>Delegates proxy creation to a chain of {@link ProxyEngine}s
 * (ByteBuddy first, then JDK dynamic proxies).
 * Interceptors are matched against target classes via
 * {@link InterceptFor} metadata.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>⚡ Supports both class-based proxies (ByteBuddy) and interface-based proxies (JDK).</li>
 *   <li>🧩 Filters interceptors using declared preferred types.</li>
 *   <li>📋 Keeps an internal registry of added {@link MethodInterceptor}s.</li>
 * </ul>
 */
public class DefaultProxyFactory implements ProxyFactory {

    private final List<MethodInterceptor> interceptors = new ArrayList<>();

    /**
     * 🚀 Engines used for proxy creation (order matters).
     * <ul>
     *   <li>{@link ByteBuddyProxyEngine} – class-based proxies</li>
     *   <li>{@link JdkProxyEngine} – interface-based proxies</li>
     * </ul>
     */
    private final List<ProxyEngine> engines = List.of(
            new ByteBuddyProxyEngine(),
            new JdkProxyEngine()
    );

    /**
     * 🏗️ Construct a factory with predefined interceptors.
     *
     * @param interceptors initial interceptors to register
     */
    public DefaultProxyFactory(MethodInterceptor... interceptors) {
        this.interceptors.addAll(List.of(interceptors));
    }

    /**
     * ✨ Create a proxy for the given target.
     *
     * <p>Builds a {@link ProxyContext}, finds the first engine that
     * {@link ProxyEngine#supports(ProxyContext) supports} it, and
     * delegates to that engine.</p>
     *
     * @param object target instance
     * @return proxy wrapping the target
     * @throws UnsupportedProxyException if no engine can handle the target
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T createProxy(Object object) {
        ProxyContext proxyContext = createProxyContext(object, object.getClass().getClassLoader());

        for (ProxyEngine engine : engines) {
            if (engine.supports(proxyContext)) {
                T proxy = (T) engine.createProxy(proxyContext);
                proxyContext.setProxy(proxy);
                return proxy;
            }
        }

        throw new UnsupportedProxyException(
                "No proxy engine can handle target: " + proxyContext.getTargetClass().getName() +
                        ". Provide interfaces (JDK) or use non-final class (ByteBuddy).");
    }

    /**
     * 🏗️ Build a {@link ProxyContext} and attach matching interceptors.
     *
     * <p>Interceptors are matched if their {@link InterceptFor}
     * annotation declares a type assignable from the target class.</p>
     */
    @Override
    public ProxyContext createProxyContext(Object object, ClassLoader classLoader) {
        ProxyContext      proxyContext = new ProxyContext(object, classLoader);
        Matcher<Class<?>> classMatcher = ClassMatchers.isSubtype(proxyContext.getTargetClass());

        for (MethodInterceptor interceptor : interceptors) {
            Class<?>[] preferredClasses = getAnnotationValue(
                    interceptor.getClass(), InterceptFor.class, InterceptFor::value);

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
     * ➕ Add a new interceptor to this factory.
     *
     * @param interceptor interceptor to register
     */
    @Override
    public void addInterceptor(MethodInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    /**
     * 📋 Get all registered interceptors.
     *
     * <p>Returns an immutable copy of the list, so external modifications
     * do not affect the factory’s state.</p>
     */
    @Override
    public List<MethodInterceptor> getInterceptors() {
        return List.copyOf(interceptors);
    }
}
