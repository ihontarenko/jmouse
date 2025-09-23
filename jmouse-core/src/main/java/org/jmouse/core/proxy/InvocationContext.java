package org.jmouse.core.proxy;

import org.jmouse.core.IdGenerator;
import org.jmouse.core.SecureRandomStringGenerator;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable call-scoped context shared across interceptors in the pipeline.
 * <p>
 * Provides:
 * <ul>
 *   <li>Stable call id & timestamps</li>
 *   <li>References to proxy/target/method/args</li>
 *   <li>Ordinal (position) of current interceptor</li>
 *   <li>Mutable attribute bag for cross-interceptor data exchange</li>
 * </ul>
 */
public record InvocationContext(
        /** Proxy instance (JDK/ByteBuddy). May be same as target for direct calls. */
        Object proxy,
        /** Target (real) instance to invoke. */
        Object target,
        /** Method being invoked. */
        Method method,
        /** Arguments snapshot (defensive, non-null). */
        Object[] arguments,
        /** Monotonic start time (nanoTime) captured at the moment of creating the context. */
        long startedAtNanos,
        /** Wall-clock timestamp when the context was created. */
        Instant startedAt,
        /** Stable correlation id for this call (useful for logs/tracing). */
        String callId,
        /** Current interceptor ordinal (0..n-1). Updated per step via {@link #withOrdinal(int)}. */
        int ordinal,
        /** Mutable attribute bag to share data between interceptors. */
        Map<String, Object> attributes) {
    // --- Builders / factories -------------------------------------------------

    public final static IdGenerator<String, String> ID_GENERATOR = new SecureRandomStringGenerator(16);

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Quick factory from a MethodInvocation (proxy/target/method/args).
     */
    public static InvocationContext forInvocation(MethodInvocation invocation) {
        return builder()
                .proxy(invocation.getProxy())
                .target(invocation.getTarget())
                .method(invocation.getMethod())
                .arguments(invocation.getArguments())
                .build();
    }

    // --- "with-*" helpers to keep record immutable ----------------------------

    public InvocationContext withOrdinal(int newOrdinal) {
        return new InvocationContext(
                proxy, target, method, arguments, startedAtNanos, startedAt, callId, newOrdinal, attributes);
    }

    /**
     * Returns current argument array (non-null). If you need to replace arguments,
     * do it at {@link MethodInvocation} level (via a wrapper) before proceed().
     */
    public Object[] arguments() {
        return arguments;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        return (type.isInstance(value)) ? type.cast(value) : null;
    }

    public InvocationContext setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }

    // --- Builder --------------------------------------------------------------

    public static final class Builder {

        private Object              proxy;
        private Object              target;
        private Method              method;
        private Object[]            arguments;
        private String              callId;
        private Long                startedAtNanos;
        private Instant             startedAt;
        private Integer             ordinal;
        private Map<String, Object> attributes;

        public Builder proxy(Object proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder target(Object target) {
            this.target = target;
            return this;
        }

        public Builder method(Method method) {
            this.method = method;
            return this;
        }

        public Builder arguments(Object[] arguments) {
            this.arguments = (arguments == null) ? new Object[0] : arguments.clone();
            return this;
        }

        public Builder callId(String callId) {
            this.callId = callId;
            return this;
        }

        public Builder startedAtNanos(long nanos) {
            this.startedAtNanos = nanos;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder ordinal(int ordinal) {
            this.ordinal = ordinal;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public InvocationContext build() {
            long    nowNanos = (startedAtNanos != null) ? startedAtNanos : System.nanoTime();
            Instant now      = (startedAt != null) ? startedAt : Instant.now();
            String  id       = (callId != null) ? callId : ID_GENERATOR.generate();
            int     ord      = (ordinal != null) ? ordinal : 0;

            Map<String, Object> bag       = (attributes != null) ? attributes : new ConcurrentHashMap<>();
            Object[]            arguments = (this.arguments == null) ? new Object[0] : this.arguments.clone();

            return new InvocationContext(proxy, target, method, arguments, nowNanos, now, id, ord, bag);
        }
    }
}
