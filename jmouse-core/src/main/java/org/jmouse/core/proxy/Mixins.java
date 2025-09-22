package org.jmouse.core.proxy;

import java.util.HashMap;
import java.util.Map;

/**
 * 🧩 Container for <b>mixins</b> — bindings of interface → implementation.
 *
 * <p>Mixins are used during proxy creation to attach additional behaviors:
 * each interface can be backed by a concrete delegate object.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>📦 Immutable map of interface-to-instance bindings.</li>
 *   <li>✨ Quick factories for empty or single-object mixins.</li>
 *   <li>🔎 Lookup by interface via {@link #implementationFor(Class)}.</li>
 * </ul>
 *
 * <pre>{@code
 * Mixins mixins = Mixins.of(new AuditableImpl());
 * Auditable audit = (Auditable) mixins.implementationFor(Auditable.class);
 * }</pre>
 */
public record Mixins(Map<Class<?>, Object> implementations) {

    /**
     * 🕳️ Create an empty set of mixins (no interfaces).
     *
     * @return empty mixin container
     */
    public static Mixins empty() {
        return new Mixins(Map.of());
    }

    /**
     * 🧬 Create mixins from a single instance object.
     *
     * <p>Registers all interfaces implemented by the given object,
     * mapping each to the same delegate instance.</p>
     *
     * @param instance delegate object
     * @return mixins covering all interfaces of {@code instance}
     */
    public static Mixins of(Object instance) {
        Map<Class<?>, Object> implementations = new HashMap<>();

        for (Class<?> iface : instance.getClass().getInterfaces()) {
            implementations.put(iface, instance);
        }

        return new Mixins(implementations);
    }

    /**
     * 🔎 Retrieve the delegate for a given interface.
     *
     * @param iface interface type
     * @return registered implementation or {@code null} if none
     */
    public Object implementationFor(Class<?> iface) {
        return implementations.get(iface);
    }
}
