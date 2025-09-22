package org.jmouse.core.proxy.old;

import org.jmouse.core.proxy.ProxyContext;
import org.jmouse.core.proxy.ProxyEngine;

/**
 * 🪞 {@link ProxyEngine} based on JDK dynamic proxies.
 *
 * <p>Generates proxies for targets that expose at least one interface.
 * Delegates proxy creation to {@link JdkProxy}.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>✅ Supports only interface-based proxying.</li>
 *   <li>⚡ Lightweight and built into the JDK (no external deps).</li>
 *   <li>🚫 Cannot proxy concrete classes (use ByteBuddy instead).</li>
 * </ul>
 */
public class JdkProxyEngine implements ProxyEngine {

    public static final String ENGINE_NAME = "JDK";

    /**
     * 🏗️ Create a JDK dynamic proxy for the given context.
     */
    @Override
    public Object createProxy(ProxyContext context) {
        return new JdkProxy(context).getProxy();
    }

    /**
     * ✅ Supports targets with at least one interface.
     */
    @Override
    public boolean supports(ProxyContext context) {
        return !context.getInterfaces().isEmpty();
    }

    /**
     * 🏷️ Engine name.
     */
    @Override
    public String name() {
        return ENGINE_NAME;
    }
}
