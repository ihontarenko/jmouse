package org.jmouse.core.proxy;

import java.util.ArrayList;
import java.util.List;

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

    @SuppressWarnings("unchecked")
    public static <T> ProxyDefinition<T> createDefault(T instance) {
        Class<T> type = (Class<T>) instance.getClass();
        return builder(type)
                .classLoader(type.getClassLoader())
                .instanceProvider(InstanceProvider.singleton(instance))
                .build();
    }

    public static <T> Builder<T> builder(Class<T> targetClass) {
        return new Builder<>(targetClass);
    }

    public Class<T> targetClass() {
        return targetClass;
    }

    public ClassLoader classLoader() {
        return classLoader;
    }

    public InstanceProvider<T> instanceProvider() {
        return instanceProvider;
    }

    public List<MethodInterceptor> chain() {
        return interceptors;
    }

    public List<Class<?>> extraInterfaces() {
        return extraInterfaces;
    }

    public Mixins mixins() {
        return mixins;
    }

    public InterceptionPolicy policy() {
        return policy;
    }

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

        public Builder<T> instanceProvider(InstanceProvider<T> instanceProvider) {
            this.instanceProvider = instanceProvider;
            return this;
        }

        public Builder<T> classLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Builder<T> intercept(MethodInterceptor interceptor) {
            this.interceptors.add(interceptor);
            return this;
        }

        public Builder<T> interceptAll(List<MethodInterceptor> list) {
            this.interceptors.addAll(list);
            return this;
        }

        public Builder<T> addInterface(Class<?> iface) {
            this.extraInterfaces.add(iface);
            return this;
        }

        public Builder<T> mixins(Mixins mixins) {
            this.mixins = mixins;
            return this;
        }

        public Builder<T> policy(InterceptionPolicy policy) {
            this.policy = policy;
            return this;
        }

        public ProxyDefinition<T> build() {
            return new ProxyDefinition<>(this);
        }
    }
}
