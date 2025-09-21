package org.jmouse.core.proxy2.api;

import java.util.HashMap;
import java.util.Map;

public record Mixins(Map<Class<?>, Object> implementations) {

    public static Mixins empty() {
        return new Mixins(Map.of());
    }

    public static Mixins of(Object implementation) {
        Map<Class<?>, Object> implementations = new HashMap<>();

        for (Class<?> iface : implementation.getClass().getInterfaces()) {
            implementations.put(iface, implementation);
        }

        return new Mixins(implementations);
    }

    public Object implementationFor(Class<?> iface) {
        return implementations.get(iface);
    }

}