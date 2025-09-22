package org.jmouse.context.proxy.api;

import java.util.*;

public final class ProxyFactory {

    private final List<ProxyEngine> engines = new ArrayList<>();

    public ProxyFactory register(ProxyEngine engine) {
        engines.add(Objects.requireNonNull(engine));
        return this;
    }

    public <T> T create(ProxyDefinition<T> definition) {
        for (ProxyEngine engine : engines) {
            if (engine.supports(definition)) {
                return engine.createProxy(definition);
            }
        }

        throw new IllegalStateException("NO PROXY ENGINE SUPPORTS: " + definition);
    }
}