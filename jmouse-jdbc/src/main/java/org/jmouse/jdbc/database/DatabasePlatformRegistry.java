package org.jmouse.jdbc.database;

import org.jmouse.core.Contract;
import org.jmouse.core.Sorter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DatabasePlatformRegistry {

    private final List<DatabasePlatformProvider> providers = new ArrayList<>();
    private       DatabasePlatform               fallback  = StandardPlatforms.ansi();

    public DatabasePlatformRegistry register(DatabasePlatformProvider provider) {
        providers.add(Contract.nonNull(provider, "provider"));
        providers.sort(Sorter.PRIORITY_COMPARATOR.reversed());
        return this;
    }

    public DatabasePlatformRegistry fallback(DatabasePlatform fallback) {
        this.fallback = Contract.nonNull(fallback, "fallback");
        return this;
    }

    public DatabasePlatform fallback() {
        return fallback;
    }

    public DatabasePlatform resolve(DatabaseInformation info) {
        for (DatabasePlatformProvider provider : providers) {
            if (provider.supports(info)) {
                return provider.create(info);
            }
        }
        return fallback;
    }
}