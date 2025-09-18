package org.jmouse.core.throttle;

import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 🔑 Default {@link RateLimitKeyResolver} based on method + arguments hash.
 *
 * <p>Builds a string key of the form:</p>
 * <pre>
 *   Class#method(paramTypes)|arguments=&lt;deepHash(arguments)&gt;
 * </pre>
 *
 * <h3>Details</h3>
 * <ul>
 *   <li>📌 If arguments are empty → key is only method signature.</li>
 *   <li>🧮 Arguments are hashed with {@link Arrays#deepHashCode(Object[])}.</li>
 *   <li>🧵 Thread-safe (stateless, no mutable fields).</li>
 *   <li>👍 Suitable as a default resolver for {@link RateLimit.Scope#CUSTOM}.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * MethodArgumentsHashKeyResolver resolver = new MethodArgumentsHashKeyResolver();
 * String key = (String) resolver.resolve(service, MyService.class.getMethod("find", String.class),
 *                                        new Object[]{"abc"});
 * // → "com.example.MyService#find(String)|arguments=96354"
 * }</pre>
 */
public final class MethodArgumentsHashKeyResolver implements RateLimitKeyResolver {

    /**
     * 🧩 Resolve a unique key from target, method, and arguments.
     *
     * @param target    target instance (unused in this implementation)
     * @param method    reflected method
     * @param arguments actual invocation arguments
     * @return string key including method signature + deep hash of arguments
     */
    @Override
    public Object resolve(Object target, Method method, Object[] arguments) {
        StringBuilder builder       = new StringBuilder();
        int           argumentsHash = (arguments == null || arguments.length == 0)
                ? 0 : Arrays.deepHashCode(arguments);

        builder.append(Reflections.getMethodSignature(method));
        builder.append("|arguments=").append(argumentsHash);

        return builder.toString();
    }
}
