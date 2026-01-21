package org.jmouse.crawler.dsl.builder;

import org.jmouse.crawler.api.UtilityRegistry;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class UtilitiesBuilder {

    private final MapUtilityRegistry reg = new MapUtilityRegistry();

    public <T> UtilitiesBuilder register(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        reg.put(type, instance);
        return this;
    }

    UtilityRegistry build() {
        return reg;
    }
}

final class MapUtilityRegistry implements UtilityRegistry {
    private final Map<Class<?>, Object> map = new ConcurrentHashMap<>();

    <T> void put(Class<T> type, T instance) {
        map.put(type, instance);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Class<T> type) {
        return (T) map.get(type);
    }
}

