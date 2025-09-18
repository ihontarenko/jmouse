package org.jmouse.core.proxy;

/**
 * ⚙️ Strategy interface for building proxies.
 *
 * <p>Each engine encapsulates a specific proxying mechanism
 * (e.g. JDK dynamic proxies, bytecode generation, custom wrappers).</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>🏗️ {@link #createProxy(ProxyContext)} → build a proxy from context.</li>
 *   <li>✅ {@link #supports(ProxyContext)} → check whether this engine can handle the context.</li>
 *   <li>🏷️ {@link #name()} → human-readable identifier (for logging/config).</li>
 * </ul>
 */
public interface ProxyEngine {

    /**
     * 🏗️ Create a proxy instance for the given context.
     *
     * @param context proxy configuration (target, interceptors, class loader, etc.)
     * @return generated proxy object
     */
    Object createProxy(ProxyContext context);

    /**
     * ✅ Determine if this engine supports the given proxy context.
     *
     * @param context proxy configuration
     * @return {@code true} if this engine can handle it
     */
    boolean supports(ProxyContext context);

    /**
     * 🏷️ Name of this proxy engine (e.g. "JDK", "CGLIB").
     *
     * @return unique engine name
     */
    String name();
}
