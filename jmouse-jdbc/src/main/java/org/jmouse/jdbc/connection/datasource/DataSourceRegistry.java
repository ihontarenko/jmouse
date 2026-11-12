package org.jmouse.jdbc.connection.datasource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.sql.DataSource;

/**
 * Named sources supplied directly or created lazily, without specification/factory infrastructure.
 * Resolution is thread-safe and each successful supplier is evaluated once per registration.
 * The application owns source/pool shutdown; this registry never closes supplied resources.
 */
public final class DataSourceRegistry implements DataSourceResolver {

    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();

    public void register(String name, DataSource dataSource) {
        add(name, new Registration(Objects.requireNonNull(dataSource, "dataSource")));
    }

    public void registerLazy(String name, Supplier<? extends DataSource> factory) {
        add(name, new Registration(Objects.requireNonNull(factory, "factory")));
    }

    private void add(String name, Registration registration) {
        requireName(name);
        if (registrations.putIfAbsent(name, registration) != null) {
            throw new IllegalStateException("DataSource already registered: " + name);
        }
    }

    /** Returns null only when the name is absent. Factory failures propagate and may be retried. */
    public DataSource obtain(String name) {
        requireName(name);
        Registration registration = registrations.get(name);
        return registration == null ? null : registration.resolve();
    }

    DataSource obtainOrCreate(String name, Supplier<? extends DataSource> factory) {
        requireName(name);
        return registrations
            .computeIfAbsent(name, key -> new Registration(Objects.requireNonNull(factory, "factory")))
            .resolve();
    }

    public DataSource require(String name) {
        DataSource dataSource = obtain(name);
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not registered: " + name);
        }
        return dataSource;
    }

    @Override
    public DataSource resolve(String name) {
        return require(name);
    }

    /** Resolves every currently registered source. The result is immutable. */
    public Map<String, DataSource> snapshot() {
        Map<String, DataSource> snapshot = new LinkedHashMap<>();
        registrations.forEach((name, registration) -> snapshot.put(name, registration.resolve()));
        return Map.copyOf(snapshot);
    }

    private static void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("DataSource name must not be blank");
        }
    }

    private static final class Registration {

        private volatile DataSource dataSource;
        private Supplier<? extends DataSource> factory;

        private Registration(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        private Registration(Supplier<? extends DataSource> factory) {
            this.factory = factory;
        }

        private DataSource resolve() {
            DataSource resolved = dataSource;
            if (resolved == null) {
                synchronized (this) {
                    resolved = dataSource;
                    if (resolved == null) {
                        resolved = Objects.requireNonNull(factory.get(), "DataSource factory returned null");
                        dataSource = resolved;
                        factory = null;
                    }
                }
            }
            return resolved;
        }
    }
}
