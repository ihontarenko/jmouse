package org.jmouse.core.proxy;

import java.util.ArrayList;
import java.util.List;

/**
 * 📐 Immutable description of a proxy.
 *
 * <p>Encapsulates all metadata required to build and run a proxy instance:</p>
 * <ul>
 *   <li>🎯 Target class to be proxied</li>
 *   <li>📦 {@link InstanceProvider} for supplying target instances</li>
 *   <li>🔗 Ordered chain of {@link MethodInterceptor}s</li>
 *   <li>➕ Extra interfaces to expose on the proxy</li>
 *   <li>🧩 {@link Mixins} for interface → delegate bindings</li>
 *   <li>⚖️ {@link InterceptionPolicy} to control which methods are intercepted</li>
 *   <li>📚 {@link ClassLoader} to define the proxy</li>
 * </ul>
 *
 * <p>Instances are created via the {@link Builder}.</p>
 *
 * @param <T> the target class type
 */
public final class ProxyDefinition<T> {

    private final Class<T>                targetClass;
    private final ClassLoader             classLoader;
    private final InstanceProvider<T>     instanceProvider;
    private final List<MethodInterceptor> interceptors;
    private final List<Class<?>>          extraInterfaces;
    private final Mixins                  mixins;
    private final InterceptionPolicy      policy;

    private ProxyDefinition(Builder<T> builder) {
        this.targetClass = builder.targetClass;
        this.classLoader = builder.classLoader;
        this.instanceProvider = builder.instanceProvider;
        this.interceptors = List.copyOf(builder.interceptors);
        this.extraInterfaces = List.copyOf(builder.extraInterfaces);
        this.mixins = builder.mixins;
        this.policy = builder.policy;
    }

    /**
     * Starts building a proxy definition for the given class.
     *
     * @param targetClass the target class to proxy
     * @param <T>         type of target
     * @return builder instance
     */
    public static <T> Builder<T> builder(Class<T> targetClass) {
        return new Builder<>(targetClass);
    }

    /**
     * @return the target class being proxied
     */
    public Class<T> targetClass() {
        return targetClass;
    }

    /**
     * @return the classloader that will define the proxy
     */
    public ClassLoader classLoader() {
        return classLoader;
    }

    /**
     * @return provider for creating/obtaining target instances
     */
    public InstanceProvider<T> instanceProvider() {
        return instanceProvider;
    }

    /**
     * @return ordered interceptor chain for this proxy
     */
    public List<MethodInterceptor> chain() {
        return interceptors;
    }

    /**
     * @return additional interfaces to expose on the proxy
     */
    public List<Class<?>> extraInterfaces() {
        return extraInterfaces;
    }

    /**
     * @return registered mixins for interface delegation
     */
    public Mixins mixins() {
        return mixins;
    }

    /**
     * @return interception policy controlling which methods are intercepted
     */
    public InterceptionPolicy policy() {
        return policy;
    }

    /**
     * 🏗️ Builder for {@link ProxyDefinition}.
     *
     * <p>Allows fluent configuration of proxy metadata.</p>
     */
    public static final class Builder<T> {

        private final Class<T>                targetClass;
        private final List<MethodInterceptor> interceptors     = new ArrayList<>();
        private final List<Class<?>>          extraInterfaces  = new ArrayList<>();
        private       ClassLoader             classLoader;
        private       InstanceProvider<T>     instanceProvider = InstanceProvider.singleton(null);
        private       Mixins                  mixins           = Mixins.empty();
        private       InterceptionPolicy      policy           = InterceptionPolicy.defaultPolicy();

        public Builder(Class<T> targetClass) {
            this.targetClass = targetClass;
        }

        /**
         * @param instanceProvider provider for target instances
         */
        public Builder<T> instanceProvider(InstanceProvider<T> instanceProvider) {
            this.instanceProvider = instanceProvider;
            return this;
        }

        /**
         * @param classLoader class loader to define proxy in
         */
        public Builder<T> classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        /**
         * Adds a single interceptor to the chain.
         */
        public Builder<T> intercept(MethodInterceptor interceptor) {
            this.interceptors.add(interceptor);
            return this;
        }

        /**
         * Adds multiple interceptors to the chain.
         */
        public Builder<T> interceptAll(List<MethodInterceptor> list) {
            this.interceptors.addAll(list);
            return this;
        }

        /**
         * Adds extra interfaces for the proxy to implement.
         */
        public Builder<T> addInterfaces(Class<?>... ifaces) {
            this.extraInterfaces.addAll(List.of(ifaces));
            return this;
        }

        /**
         * Sets mixins for interface → implementation delegation.
         */
        public Builder<T> mixins(Mixins mixins) {
            this.mixins = mixins;
            return this;
        }

        /**
         * Sets interception policy controlling which methods are intercepted.
         */
        public Builder<T> policy(InterceptionPolicy policy) {
            this.policy = policy;
            return this;
        }

        /**
         * Builds an immutable {@link ProxyDefinition}.
         */
        public ProxyDefinition<T> build() {
            return new ProxyDefinition<>(this);
        }
    }
}
