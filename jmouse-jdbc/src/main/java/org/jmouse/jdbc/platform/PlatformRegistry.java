package org.jmouse.jdbc.platform;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlatformRegistry {

    private final Map<String, JdbcPlatformProvider> providers = new ConcurrentHashMap<>();

    public PlatformRegistry register(String name, JdbcPlatformProvider provider) {
        providers.put(name, provider);
        return this;
    }

    public JdbcPlatform get(String name) {
        JdbcPlatformProvider provider = providers.get(name);

        if (provider == null) {
            throw new IllegalArgumentException("No JDBC platform registered: " + name);
        }

        return provider.get();
    }

    public boolean contains(String name) {
        return providers.containsKey(name);
    }
}
