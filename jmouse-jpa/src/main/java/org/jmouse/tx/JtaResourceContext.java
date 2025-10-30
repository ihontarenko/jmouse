package org.jmouse.tx;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 🧵 Thread-local resource registry.
 * Can hold connections, sessions, unit-of-work, etc.
 */
public interface JtaResourceContext {

    static Map<Object, Object> all() {
        return Holder.RESOURCES.get();
    }

    static boolean has(Object key) {
        return Holder.RESOURCES.get().containsKey(key);
    }

    @SuppressWarnings("unchecked")
    static <T> T get(Object key) {
        return (T) Holder.RESOURCES.get().get(key);
    }

    static void bind(Object key, Object value) {
        Object old = Holder.RESOURCES.get().putIfAbsent(key, value);
        if (old != null) {
            throw new IllegalStateException("Resource already bound for key: " + key);
        }
    }

    static void put(Object key, Object value) {
        Holder.RESOURCES.get().put(key, value);
    }

    static Object unbind(Object key) {
        Object value = Holder.RESOURCES.get().remove(key);

        if (Holder.RESOURCES.get().isEmpty()) {
            Holder.RESOURCES.remove();
        }

        return value;
    }

    static Map<Object, Object> view() {
        return Collections.unmodifiableMap(Holder.RESOURCES.get());
    }

    final class Holder {
        private static final ThreadLocal<Map<Object, Object>> RESOURCES =
                ThreadLocal.withInitial(HashMap::new);
    }
}
