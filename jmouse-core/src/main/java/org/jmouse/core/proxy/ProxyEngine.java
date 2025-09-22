package org.jmouse.core.proxy;

/**
 * ⚙️ Strategy interface for building proxies.
 *
 * <p>Each engine encapsulates a specific proxying mechanism
 * (e.g. JDK dynamic proxies, ByteBuddy, CGLIB, custom wrappers).</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>🏗️ {@link #createProxy(ProxyDefinition)} → build a proxy instance from a {@link ProxyDefinition}.</li>
 *   <li>✅ {@link #supports(ProxyDefinition)} → check whether this engine can handle the definition.</li>
 *   <li>🏷️ {@link #name()} → human-readable identifier (for logging/config).</li>
 * </ul>
 */
public interface ProxyEngine {

    /**
     * 🏗️ Create a proxy instance for the given definition.
     *
     * @param definition proxy definition (target object, interceptors, class loader, mixins, etc.)
     * @param <T>        proxy type
     * @return generated proxy object
     * @throws ProxyInvocationException if proxy creation fails
     */
    <T> T createProxy(ProxyDefinition<T> definition);

    /**
     * ✅ Check if this engine supports proxying for the given definition.
     *
     * @param definition proxy definition
     * @return {@code true} if this engine can handle the definition, {@code false} otherwise
     */
    boolean supports(ProxyDefinition<?> definition);

    /**
     * 🏷️ Name of this proxy engine (e.g. {@code "JDK"}, {@code "BYTE_BUDDY"}, {@code "CGLIB"}).
     *
     * @return unique engine identifier
     */
    String name();
}
