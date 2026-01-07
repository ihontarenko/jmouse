package org.jmouse.jdbc.database;

import org.jmouse.core.Contract;
import org.jmouse.core.Sorter;
import org.jmouse.jdbc.database.standard.StandardPlatforms;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry and resolution mechanism for {@link DatabasePlatform}s.
 * <p>
 * {@code DatabasePlatformRegistry} maintains an ordered list of
 * {@link DatabasePlatformProvider}s and is responsible for selecting
 * the most appropriate {@link DatabasePlatform} based on
 * runtime {@link DatabaseInformation}.
 *
 * <h3>Resolution strategy</h3>
 * <ul>
 *     <li>Providers are evaluated in descending {@link org.jmouse.core.Priority} order</li>
 *     <li>The first provider that {@link DatabasePlatformProvider#supports(DatabaseInformation)}
 *         the given database info wins</li>
 *     <li>If no provider matches, a configurable fallback platform is returned</li>
 * </ul>
 *
 * <p>
 * This design allows:
 * <ul>
 *     <li>Vendor-specific platforms (PostgreSQL, MySQL, Oracle, etc.)</li>
 *     <li>Version-aware resolution</li>
 *     <li>Safe ANSI-compliant fallback behavior</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * DatabasePlatformRegistry registry = new DatabasePlatformRegistry()
 *     .register(new PostgresPlatformProvider())
 *     .register(new MySqlPlatformProvider());
 *
 * DatabasePlatform platform = registry.resolve(databaseInfo);
 *
 * PaginationStrategy pagination = platform.pagination();
 * }</pre>
 *
 * @author jMouse
 */
public final class DatabasePlatformRegistry {

    /**
     * Ordered list of registered platform providers.
     * Providers with higher priority are evaluated first.
     */
    private final List<DatabasePlatformProvider> providers = new ArrayList<>();

    /**
     * Fallback platform used when no provider supports the database.
     * Defaults to ANSI-compatible platform.
     */
    private       DatabasePlatform               fallback  = StandardPlatforms.ansi();

    /**
     * Registers a {@link DatabasePlatformProvider}.
     * <p>
     * Providers are automatically sorted by descending priority
     * after registration.
     *
     * @param provider platform provider to register
     * @return this registry instance (for fluent API)
     */
    public DatabasePlatformRegistry register(DatabasePlatformProvider provider) {
        providers.add(Contract.nonNull(provider, "provider"));
        providers.sort(Sorter.PRIORITY_COMPARATOR.reversed());
        return this;
    }

    /**
     * Sets the fallback {@link DatabasePlatform}.
     * <p>
     * The fallback is used when no registered provider supports
     * the given {@link DatabaseInformation}.
     *
     * @param fallback fallback platform
     * @return this registry instance (for fluent API)
     */
    public DatabasePlatformRegistry fallback(DatabasePlatform fallback) {
        this.fallback = Contract.nonNull(fallback, "fallback");
        return this;
    }

    /**
     * Returns the currently configured fallback platform.
     *
     * @return fallback database platform
     */
    public DatabasePlatform fallback() {
        return fallback;
    }

    /**
     * Resolves a {@link DatabasePlatform} for the given database information.
     * <p>
     * Iterates over registered providers in priority order and returns
     * the first matching platform. If none match, the fallback platform
     * is returned.
     *
     * @param info runtime database information
     * @return resolved database platform (never {@code null})
     */
    public DatabasePlatform resolve(DatabaseInformation info) {
        DatabasePlatform platform = fallback;

        for (DatabasePlatformProvider provider : providers) {
            if (provider.supports(info)) {
                platform = provider.create(info);
                break;
            }
        }

        return platform;
    }
}
