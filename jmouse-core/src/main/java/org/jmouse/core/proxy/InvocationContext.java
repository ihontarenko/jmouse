package org.jmouse.core.proxy;

import org.jmouse.core.IdGenerator;
import org.jmouse.core.SecureRandomStringGenerator;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 📦 Immutable call-scoped context shared across all interceptors in a pipeline.
 *
 * <p>Captures essential details of a single proxy method invocation:</p>
 * <ul>
 *   <li>🆔 Stable call identifier ({@link #callId})</li>
 *   <li>⏱️ Start timestamps ({@link #nanoStart}, {@link #wallStart})</li>
 *   <li>🔗 References to proxy, target, method, and arguments</li>
 *   <li>📊 Interceptor ordinal (via pipeline cursor)</li>
 *   <li>🗂️ Mutable attribute bag for cross-interceptor data exchange</li>
 * </ul>
 *
 * <p>Instances are <b>immutable</b> except for the {@link #attributes} map,
 * which is thread-safe and intended for interceptor cooperation.</p>
 */
public record InvocationContext(
        MethodInvocation invocation,
        Object proxy,
        Object target,
        Method method,
        Object[] arguments,
        long nanoStart,
        Instant wallStart,
        String callId,
        Map<String, Object> attributes) {

    /**
     * Default generator for call IDs (16-char secure random).
     */
    public static final IdGenerator<String, String> ID_GENERATOR = new SecureRandomStringGenerator(16);

    /**
     * @return a fresh {@link Builder} for constructing contexts
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Factory shortcut to build a context from a {@link MethodInvocation}.
     *
     * @param invocation base invocation (proxy/target/method/args)
     * @return a new context initialized with invocation data
     */
    public static InvocationContext forInvocation(MethodInvocation invocation) {
        return builder()
                .invocation(invocation)
                .proxy(invocation.getProxy())
                .target(invocation.getTarget())
                .method(invocation.getMethod())
                .arguments(invocation.getArguments())
                .build();
    }

    /**
     * Returns the arguments array (never {@code null}).
     * <p><b>Note:</b> arguments are cloned at construction time for immutability.
     * To replace arguments dynamically, use a {@link MethodInvocation} wrapper
     * before calling {@code proceed()}.</p>
     */
    @Override
    public Object[] arguments() {
        return arguments;
    }

    /**
     * Retrieves an attribute from the context.
     *
     * @param key attribute key
     * @return value or {@code null} if missing
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Retrieves an attribute with type safety.
     *
     * @param key  attribute key
     * @param type expected type
     * @param <T>  type parameter
     * @return casted value, or {@code null} if missing or type mismatch
     */
    public <T> T getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        return (type.isInstance(value)) ? type.cast(value) : null;
    }

    /**
     * Adds or replaces an attribute in the context.
     *
     * @param key   attribute key
     * @param value attribute value (may be {@code null})
     * @return this same context (for chaining)
     */
    public InvocationContext setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    /**
     * Removes an attribute from the context.
     *
     * @param key attribute key
     * @return previous value or {@code null}
     */
    public Object removeAttribute(String key) {
        return attributes.remove(key);
    }

    /**
     * Builder for {@link InvocationContext}.
     *
     * <p>Ensures defaults:
     * <ul>
     *   <li>Arguments → empty array if null</li>
     *   <li>Call ID → generated if not set</li>
     *   <li>Timestamps → captured at build time if not set</li>
     *   <li>Attributes → {@link ConcurrentHashMap} if not provided</li>
     * </ul>
     */
    public static final class Builder {

        private MethodInvocation    invocation;
        private Object              proxy;
        private Object              target;
        private Method              method;
        private Object[]            arguments;
        private String              callId;
        private Long                nanoStart;
        private Instant             wallStart;
        private Map<String, Object> attributes;

        public Builder invocation(MethodInvocation invocation) {
            this.invocation = invocation;
            return this;
        }

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
            this.nanoStart = nanos;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.wallStart = startedAt;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Builds a new immutable {@link InvocationContext}.
         *
         * @return fully initialized context
         */
        public InvocationContext build() {
            long    nowNanos = (nanoStart != null) ? nanoStart : System.nanoTime();
            Instant now      = (wallStart != null) ? wallStart : Instant.now();
            String  id       = (callId != null) ? callId : ID_GENERATOR.generate();

            Map<String, Object> bag       = (attributes != null) ? attributes : new ConcurrentHashMap<>();
            Object[]            arguments = (this.arguments == null) ? new Object[0] : this.arguments.clone();

            return new InvocationContext(invocation, proxy, target, method, arguments, nowNanos, now, id, bag);
        }
    }
}
