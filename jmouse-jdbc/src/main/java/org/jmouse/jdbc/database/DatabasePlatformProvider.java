package org.jmouse.jdbc.database;

import org.jmouse.core.Ordered;

/**
 * Strategy interface for creating {@link DatabasePlatform} instances.
 * <p>
 * {@code DatabasePlatformProvider} is responsible for:
 * <ul>
 *     <li>declaring whether it supports a specific database/vendor/version</li>
 *     <li>constructing an appropriate {@link DatabasePlatform} implementation</li>
 *     <li>participating in ordered resolution when multiple providers match</li>
 * </ul>
 *
 * <p>
 * Providers are typically registered in a {@link DatabasePlatformRegistry}
 * and evaluated in descending {@link #order()} priority.
 *
 * <h3>Typical implementation</h3>
 * <pre>{@code
 * public final class PostgresPlatformProvider implements DatabasePlatformProvider {
 *
 *     @Override
 *     public boolean supports(DatabaseInformation info) {
 *         return info.productName().equalsIgnoreCase("PostgreSQL");
 *     }
 *
 *     @Override
 *     public DatabasePlatform create(DatabaseInformation info) {
 *         return new PostgresPlatform(info);
 *     }
 *
 *     @Override
 *     public int order() {
 *         return 100;
 *     }
 * }
 * }</pre>
 *
 * <p>
 * If multiple providers return {@code true} from {@link #supports(DatabaseInformation)},
 * the one with the highest {@link #order()} wins.
 *
 * @author jMouse
 */
public interface DatabasePlatformProvider extends Ordered {

    /**
     * Determines whether this provider supports the given database.
     *
     * @param info runtime database information
     * @return {@code true} if this provider can handle the database,
     *         {@code false} otherwise
     */
    boolean supports(DatabaseInformation info);

    /**
     * Creates a {@link DatabasePlatform} instance for the matched database.
     * <p>
     * This method is only invoked if {@link #supports(DatabaseInformation)}
     * returned {@code true}.
     *
     * @param info runtime database information
     * @return database platform implementation
     */
    DatabasePlatform create(DatabaseInformation info);

    /**
     * Priority for provider ordering and tie-breaking.
     * <p>
     * Providers with higher values take precedence over lower ones
     * when multiple providers support the same database.
     *
     * @return provider priority (default: {@code 0})
     */
    @Override
    default int order() {
        return 0;
    }
}
