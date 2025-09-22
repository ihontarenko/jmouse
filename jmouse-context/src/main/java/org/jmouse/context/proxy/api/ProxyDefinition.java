package org.jmouse.context.proxy.api;

import java.util.ArrayList;
import java.util.List;

public final class ProxyDefinition<T> {

    private final Class<T>                targetClass;
    private final TargetProvider<T>       targetProvider;
    private final List<MethodInterceptor> interceptors;
    private final List<Class<?>>          extraInterfaces;
    private final Mixins                  mixins;
    private final ProxyOptions            options;
    private final InterceptionPolicy      policy;

    private ProxyDefinition(Builder<T> builder) {
        this.targetClass = builder.targetClass;
        this.targetProvider = builder.targetProvider;
        this.interceptors = List.copyOf(builder.interceptors);
        this.extraInterfaces = List.copyOf(builder.extraInterfaces);
        this.mixins = builder.mixins;
        this.options = builder.options;
        this.policy = builder.policy;
    }

    public static <T> Builder<T> builder(Class<T> targetClass) {
        return new Builder<>(targetClass);
    }

    public Class<T> targetClass() {
        return targetClass;
    }

    public TargetProvider<T> target() {
        return targetProvider;
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

    public ProxyOptions options() {
        return options;
    }

    public InterceptionPolicy policy() {
        return policy;
    }

    public static final class Builder<T> {

        private final Class<T>                targetClass;
        private final List<MethodInterceptor> interceptors    = new ArrayList<>();
        private final List<Class<?>>          extraInterfaces = new ArrayList<>();
        private       TargetProvider<T>       targetProvider  = TargetProvider.singleton(null);
        private       Mixins                  mixins          = Mixins.empty();
        private       ProxyOptions            options         = ProxyOptions.defaults();
        private       InterceptionPolicy      policy          = InterceptionPolicy.defaultPolicy();

        public Builder(Class<T> targetClass) {
            this.targetClass = targetClass;
        }

        public Builder<T> target(TargetProvider<T> targetProvider) {
            this.targetProvider = targetProvider;
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

        public Builder<T> options(ProxyOptions options) {
            this.options = options;
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
