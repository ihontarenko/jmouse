package org.jmouse.core.proxy2.api;

import java.util.*;

public final class ProxyFactory {
    private final List<ProxyEngine> engines = new ArrayList<>();

    public ProxyFactory register(ProxyEngine engine) {
        engines.add(Objects.requireNonNull(engine));
        return this;
    }

    public <T> T create(ProxyDefinition<T> def) {
        for (ProxyEngine e : engines) {
            if (e.supports(def)) {
                return e.createProxy(def);
            }
        }
        throw new IllegalStateException("No ProxyEngine supports: " + def);
    }
}